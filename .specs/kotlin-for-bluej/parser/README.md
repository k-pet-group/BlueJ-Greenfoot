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
- Supertype-editing `Selection` positions derived from `PsiElement.getTextRange()`
- Package-directive `Selection`s (keyword span + name span + zero-length post-name marker) derived from `KtFile.getPackageDirective()` so `ClassTarget.enforcePackage()` can rewrite the line in place; built via `KotlinParserUtils.packageSelections(ktFile, source)`. The post-name marker takes the place of Java's `;` (Kotlin has no trailing semicolon).

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

---

## Known Limitations

- **Full-file reparse per edit** (~50-100ms for 500 lines) -- see [Incremental PSI Reparse](incremental-reparse/README.md)
- **Soft keyword reclassification is context-free** -- `data` highlighted as keyword even when used as identifier
- **No entity resolution** -- no code completion or type-aware features for Kotlin. The Ctrl+Space keybinding short-circuits to a transient "unavailable" popup in `FlowEditor`; see [Editor: Known Limitations](../editor/README.md#known-limitations).
- **Shaded PSI class paths** may change between Kotlin versions (concentrated in 4 files)
- **Multi-line `${...}` in triple-quoted strings** -- continuation lines may not highlight correctly (rare in educational code)

---

## Submodules

- [Incremental PSI Reparse](incremental-reparse/README.md) -- tiered strategy to reduce per-keystroke latency
