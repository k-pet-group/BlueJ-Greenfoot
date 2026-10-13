---
id: kotlin-for-bluej-parser
type: module-design
title: "Kotlin Parser & Lexer"
status: active
parent: kotlin-for-bluej
depends-on:
  - bluej-parser
tags:
  - kotlin
---

# Kotlin Parser & Lexer

> `bluej.parser.kotlin` -- Tokenization, scope detection, and class metadata extraction for Kotlin `.kt` files, leveraging PSI from `kotlin-compiler-embeddable`.

---

## Three Capabilities

1. **Tokenization** -- `KotlinLexer` wraps PSI's JFlex-generated lexer behind BlueJ's `TokenStream` interface. `KotlinToken` maps PSI `IElementType` to BlueJ's `Token.TokenType`.
2. **Scope detection** -- `KotlinParsedCUNode` + `KotlinPsiScopeBuilder` parse the document via `KtPsiFactory.createFile()`, then build `KotlinParentNode` children for scope coloring (class=green, fun=yellow, if/when=blue, loops=pink). Reuses `JavaSyntaxView` unchanged.
3. **Class metadata** -- `KotlinInfoParser` extracts `ClassInfo` from `.kt` source via PSI for the class diagram (parallel to Java's `InfoParser`).

**Key design choice**: Delegates to PSI components already bundled in `kotlin-compiler-embeddable` (on classpath for `KotlinCompiler`). Thin adapters translate between PSI and BlueJ interfaces.

---

## Key Entry Points and Contracts

### KotlinLexer

- Wraps `org.jetbrains.kotlin.lexer.KotlinLexer` (PSI) behind `TokenStream`
- Accepts `Reader` or `CharSequence`; produces `LocatableToken` stream
- **Soft keyword reclassification**: PSI returns soft keywords (`abstract`, `data`, `sealed`, etc.) as `IDENTIFIER`; `nextToken()` reclassifies via text matching
- **Compound operator merging**: `?.` -> `SAFE_ACCESS`, `?:` -> `ELVIS`, `!!` -> `EXCLEXCL`

### KotlinToken

- 70+ constants in the 200-343 range (avoids collision with `JavaTokenTypes`)
- Maps PSI `IElementType` -> BlueJ int -> `Token.TokenType` -> CSS class
- Category mapping: control flow/modifiers -> KEYWORD1, declarations -> KEYWORD2, references -> KEYWORD3, types -> PRIMITIVE

### KotlinEnvironmentManager

- Lazy singleton for `KotlinCoreEnvironment` + `KtPsiFactory`
- ~1-2s creation cost on first use; zero cost for Java-only projects
- Thread-safe (double-checked locking); shared by scope detection and info parsing

### KotlinParsedCUNode

- Root parse node extending `ParsedCUNode` (type-compatible with `JavaSyntaxView.rootNode`)
- **Full-file PSI reparse on every edit** (MVP): removeAllChildren -> createFile -> buildScopesFromFile -> markSectionParsed
- `handlesMultilineStrings()` returns `true` to bypass `MultilineStringTracker`

### KotlinPsiScopeBuilder

- Walks PSI tree, creates `KotlinParentNode` children with container+inner pattern
- Comment detection at 4 levels (file, class body, block expression, attached to declarations) via AST traversal
- Companion object members promoted to parent scope
- Creates `KotlinCommentNode` for comments, `KotlinStringNode` for multiline triple-quoted strings

### KotlinCommentNode / KotlinStringNode

- `KotlinCommentNode`: returns single comment-typed token, prevents keyword highlighting inside comments
- `KotlinStringNode`: `tokenizeText()` returns STRING_LITERAL for gaps; child nodes for `$name`/`${expr}` template bodies get normal tokenization
- Both return `REMOVE_NODE` on edit -> triggers full PSI reparse

### KotlinInfoParser

- PSI-based `ClassInfo` extraction: class name, superclass, interfaces, methods, modifiers, package, type parameters
- For function-only files: synthesizes `ClassInfo` with `topLevelFunctionsOnly=true`
- Supertype identities follow the bounded [Hierarchy Metadata](#hierarchy-metadata) contract below. Supertype editing itself uses PSI in `KotlinLanguageSupport`; hierarchy extraction does not supply editing selections.
- Package-directive `Selection`s (keyword span + name span + zero-length post-name marker) derived from `KtFile.getPackageDirective()` so `ClassTarget.enforcePackage()` can rewrite the line in place; built via `KotlinParserUtils.packageSelections(ktFile, source)`. The post-name marker takes the place of Java's `;` (Kotlin has no trailing semicolon).
- **Callable parameter names** -- emits a `ClassInfo` comment entry (via `info.addComment(target, kdoc, paramNames)`) for every method, the primary constructor, and every secondary constructor. These power the parameter names shown in the class-diagram / object-bench right-click "call constructor / call method" menus (`ConstructorView`/`MethodView.getLongDesc()` -> `CallableView.getParamNames()`). See [Callable Parameter Names](#callable-parameter-names).

### Hierarchy Metadata

`ClassInfo.getSuperclass()` and `getImplements()` carry **qualified type identities**, not display/simple names.
This is needed by both dependency analysis and Greenfoot's superclass grouping. Hierarchy resolution is
separate from the existing callable-signature heuristics and `extractSimpleName`; neither is broadened here.

- Preserve qualified identifier segments (without generic arguments/backticks). Expand explicit imports,
  including `as` aliases and imported outer-class qualifiers. Qualified/explicitly imported names establish
  identity even when the referenced type is unavailable; they do not by themselves establish its kind.
- Resolve bare names from same-file declarations, explicit imports, **verified same-package project types**,
  verified explicit star imports, then verified default imports. No guessed `currentPackage.Unknown` FQNs.
  Same-file/import conflicts, conflicting explicit imports and multiple star candidates remain unresolved. Type
  parameters, nested classifier declarations and unsupported type aliases must not leak through to an
  imported library identity. Member properties/functions do not shadow names in this type lookup.
  This is deliberately conservative on incomplete or ambiguous code.
- The supplied exact-name lookup distinguishes class, interface, absent, and present-but-unknown declarations.
  A present-but-unknown source name blocks library/import fallback. Project lookup is owned by
  [SourceInfo](../diagram/README.md#source-hierarchy-lifecycle); it uses source identities before compilation,
  never a Java `ParsedReflective` requirement for Kotlin.
- Known declaration kind decides superclass versus interface, including `class Child : Base` with
  `constructor(...) : super(...)` in the body. If kind is unavailable, a constructor-call entry proves class
  syntax; delegation or an interface's own supertype list proves interface syntax. A plain entry in a class
  is otherwise omitted rather than assumed to be an interface.
- Library lookup verifies class resources and public types (including nested JVM types) without class
  initialisation. The project adapter excludes source-backed and untracked project bytecode before loading.
  There are no Greenfoot-specific `World`/`Actor` cases and no Kotlin Analysis API session.

### Key Lifecycles — Hierarchy Snapshots

- Ordinary `parse` calls create a short-lived `ParsedSource` PSI snapshot and return a fresh `ClassInfo`.
  Context-free calls use the parser's library loader; syntax-only tests must supply known project identities
  rather than relying on invented types resolving automatically.
- `SourceInfo` retains a snapshot while source text and filename are unchanged. Each metadata request creates
  a new `ClassInfo` and a request-local lookup cache, so changes to **other** declarations are visible without
  reparsing this file. Declaration-kind inspection reads only declarations, never recursively resolves parents.
- An edit/invalidation, changed file contents/name or read failure discards/replaces the retained snapshot.
  Snapshots own no separate environment/disposable; the shared PSI environment retains its existing lifecycle.

### Callable Parameter Names

The right-click call menus display parameter names only when a member's reflected signature has a matching comment entry attached in `View.loadClassComments`. The primary match is **exact string equality** between the comment `target` and `JavaUtils.getSignature()` of the *compiled* member; when that misses, `View` retries on the **return-type-stripped** signature (`name(<param-types>)`). The fallback exists because a Kotlin expression-body function with an *inferred* return type has no return-type node in single-file PSI, so `KotlinInfoParser` emits a `void name(...)` target that can never match the reflected return type — yet param names depend only on the parameter list, and the JVM has no return-type-only overloads among non-synthetic members, so a return-type-agnostic match is both sufficient and unambiguous. `KotlinInfoParser` still reproduces the full `JavaUtils.getSignature` format for the parameter list, mirroring what Java's `InfoParser` does:

- **Always emit, regardless of KDoc.** A comment entry is recorded for every method and constructor even when no KDoc is present (KDoc text is attached only when it exists). This matches Java's `InfoParser`, which records param names independently of Javadoc. Parameter *names* themselves are trivially available from PSI (`KtParameter.getName()`); the work is in the matching key.
- **Target signature format** (must equal `JavaUtils.getSignature`):
  - Method: `<erased-FQ-return-type> name(<erased-FQ-type>, <erased-FQ-type>)` -- e.g. `void setX(int)`. Separator is `", "`.
  - Constructor: `SimpleClassName(<erased-FQ-type>, ...)` -- e.g. `Flight(java.lang.String, java.lang.String, java.lang.String)` (no return type).
- **Type-name resolution is single-file PSI + a fixed table** (the parse environment is PSI-only, no classpath/analysis, so semantic `reference.resolve()` is unavailable and unnecessary):
  - *Kotlin builtins -> JVM types* via a fixed map: `Int->int`, `Long->long`, `Short->short`, `Byte->byte`, `Double->double`, `Float->float`, `Boolean->boolean`, `Char->char`, `String->java.lang.String`, `Any->java.lang.Object`, `Unit->void`. Nullable builtins box (`Int?->java.lang.Integer`).
  - *User-defined types* resolve their FQN from the single file the way Java's `EntityResolver` does: explicit import (`KtImportDirective.getImportedFqName()`) -> same package (`KtFile.getPackageFqName()` + simple name) -> already-qualified source text.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | PSI lexer wrapper instead of hand-written | ~280 lines adapter vs ~800 lines hand-written; battle-tested, full-language coverage |
| 2 | Full-file PSI reparse (MVP) | Educational files 100-500 lines; ~50-100ms acceptable. Incremental is future optimization. |
| 3 | `KotlinParentNode` for all scope nodes | Ensures Kotlin tokenization via virtual dispatch at every tree level |
| 4 | Lazy singleton environment | Zero startup cost for Java-only projects |
| 5 | Flat package (`bluej.parser.kotlin`) | 10 files across 3 capability areas -- not enough to justify sub-packages |
| 6 | Comment/string nodes with REMOVE_NODE | Consistent with Kotlin's full-reparse strategy; simpler than smart local validation |
| 7 | `handlesMultilineStrings()` virtual method | OOP polymorphism keeps `JavaSyntaxView` language-agnostic |
| 8 | Param names via `ClassInfo` comments, mirroring Java's `InfoParser` | Reuses the existing `.ctxt` -> `View` -> menu pipeline unchanged; no new core-side mechanism |
| 9 | Single-file PSI FQN resolution + fixed builtin->JVM table (no semantic resolve) | Parse environment is PSI-only with no classpath; covers the educational subset (primitives, `String`, imported & same-package types) without standing up the analysis frontend |
| 10 | Return-type-agnostic fallback when matching param-name comments (`View.loadClassComments`) | Expression-body methods with an inferred return type can't carry the real return type (no PSI node, no analysis), so the parser emits `void name(...)`; param names are return-type-independent and the JVM has no return-type-only overloads, so stripping the return type before matching fixes the common Kotlin idiom without risking a wrong match. Java is unaffected — its targets carry the correct return type, so the exact match always wins first. **Superseded once the Kotlin Analysis API is integrated**: inferred return types would resolve to their real JVM type, targets would match exactly, and both the parser's `void` guess and this fallback can be removed |

---

## Known Limitations

- **Full-file reparse per edit** (~50-100ms for 500 lines) -- see [Incremental PSI Reparse](incremental-reparse/README.md)
- **Soft keyword reclassification is context-free** -- `data` highlighted as keyword even when used as identifier
- **No general semantic entity resolution** -- the bounded hierarchy lookup above does not provide code completion or other type-aware features for Kotlin. The Ctrl+Space keybinding short-circuits to a transient "unavailable" popup in `FlowEditor`; see [Editor: Known Limitations](../editor/README.md#known-limitations).
- **Shaded PSI class paths** may change between Kotlin versions (concentrated in 4 files)
- **Multi-line `${...}` in triple-quoted strings** -- continuation lines may not highlight correctly (rare in educational code)
- **Hierarchy resolution is bounded** -- no type-alias expansion (including Kotlin library aliases such as `kotlin.collections.ArrayList`), inherited nested-type search or general semantic/accessibility analysis. Bare names require a verified declaration; unknown/ambiguous identities or kinds are omitted.
- **Parameter-name resolution is single-file/heuristic** -- a parameter whose type the file cannot resolve to the correct erased-FQ name will simply not match the reflected member, so that callable falls back to showing no names (never wrong names). Known unresolved cases, all out of MVP scope:
  - **Star imports** (`import com.foo.*`) -- the origin package of a bare simple name is unknown
  - **Type aliases**
  - **Generics / varargs / array erasure** beyond the fixed builtin table (e.g. `List<String> -> java.util.List`, `vararg`)
  - Types from other packages reachable only via the project classpath (Java's `EntityResolver` resolves these; the PSI-only environment does not)

---

## Submodules

- [Incremental PSI Reparse](incremental-reparse/README.md) -- tiered strategy to reduce per-keystroke latency
