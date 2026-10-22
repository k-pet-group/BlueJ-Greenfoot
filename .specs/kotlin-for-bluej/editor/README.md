---
id: kotlin-for-bluej-editor
type: module-design
title: "Kotlin Editor & Syntax"
status: active
parent: kotlin-for-bluej
depends-on:
  - bluej-editor
  - kotlin-for-bluej-parser
tags:
  - kotlin
---

# Kotlin Editor & Syntax

> Kotlin syntax highlighting and scope coloring by extending BlueJ's incremental parser infrastructure, reusing `JavaSyntaxView` unchanged.

---

## Core Insight

`JavaSyntaxView` is not Java-specific -- it operates on `ParsedNode` trees for both per-line tokenization and scope background coloring. By building a Kotlin-specific parse tree, we get full editor functionality with zero changes to `JavaSyntaxView`.

---

## Key Components

1. **KotlinParsedCUNode** -- root node extending `ParsedCUNode`. Full-file PSI reparse on edit. `handlesMultilineStrings()` returns `true` to bypass `MultilineStringTracker`.
2. **KotlinParentNode** -- configurable `nodeType` scope node. Overrides `tokenizeText()` to use `KotlinLexer`. Used for all Kotlin scopes (class, fun, control flow, inner bodies).
3. **KotlinCommentNode** -- single comment-typed token rendering; prevents keyword highlighting inside comments.
4. **KotlinStringNode** -- `STRING_LITERAL` for gaps; child `KotlinParentNode` for `$name`/`${expr}` template bodies.
5. **KotlinEnvironmentManager** -- shared `KotlinCoreEnvironment` singleton.
6. **FlowLanguageSupport** -- strategy interface replacing `boolean isKotlin` in FlowEditor.

**Submodules:**
- [PSI-Based Scope Detection](psi-scope/README.md) -- PSI tree -> ParsedNode conversion
- [Highlighting](highlighting/README.md) -- token-level syntax coloring details
- [Auto-Indent](auto-indent/README.md) -- token-driven Kotlin indenter (no scope-tree dependency)

---

## FlowLanguageSupport Strategy

| Implementation | Parser Root | Supertype Syntax |
|----------------|-------------|------------------|
| `JavaLanguageSupport` | `ParsedCUNode` | `extends`/`implements` |
| `KotlinLanguageSupport` | `KotlinParsedCUNode` | `: SuperClass(), Interface` |
| `PlainTextLanguageSupport` | `null` (fail-fast) | throws `UnsupportedOperationException` |

FlowEditor delegates parser creation and source-editing (setExtendsClass, addImplements, etc.) to the strategy. Callers pass strategy explicitly at construction.

Auto-indent is **not** routed through the strategy. `FlowIndent` dispatches
on `parser.getParser() instanceof KotlinParsedCUNode` in one place — see
[Auto-Indent](auto-indent/README.md). Indent correctness no longer depends
on `KotlinPsiScopeBuilder` emitting the right `isInner` / container shape.

---

## PSI-guided source edits

PSI is a detached structural snapshot, not the editor document. Editor changes use PSI-derived positions
and normal editor text replacement, not PSI mutation: the compiler-only environment lacks the tree-copy
extension point and POM mutation service, and a write-action wrapper does not supply them. This avoids
installing editor-platform services merely to edit student source.

`KotlinSourceEditor` plans bounded preparation-method/initialization edits. It preserves unrelated text,
reuses compatible ordinary zero-argument block-bodied Unit methods, and declines malformed or ambiguous
structure rather than guessing. It does not resolve inherited members or prove constructor/control-flow
coverage. Supplied statements remain separate so each operation can be indented without rewriting
embedded argument lines; their expressions are neither converted nor validated.

### Preparation edit lifecycle

A plan is created from the current normalized editor text and contains one enclosing UTF-16 range and
replacement, including unchanged intervening text. Planning does not modify the document. The caller
must discard a plan if its source snapshot no longer matches, otherwise apply it through the existing
document path for undo/dirty notifications. Plans are temporary values, not a second source cache;
applying one does not establish that the resulting program compiles.

---

## Class-copy identifier rewriting

Kotlin duplication uses PSI token positions to replace identifiers matching the source class's name,
including backtick-escaped spellings. It walks physical token leaves and rewrites from the end, without
mutating PSI or installing mutation services. Literal/comment text stays untouched; references inside
string-template expressions are code and can change.

The contract is deliberately best-effort, at Java duplication's level: it does not resolve bindings,
qualifications, shadowing or external references. Unrelated same-spelled identifiers can change, and no
compilation guarantee is implied. The caller owns file I/O, target registration and image association;
see `greenfoot-guifx`. This source-copy result is not a preparation edit or an editor transaction.

## Method template insertion

Insert Method selects a Kotlin template through the editor's source type. The selected locale/custom
template directory is tried first, with English fallback for the missing Kotlin file rather than Java
syntax. Existing document insertion, auto-indent and compound undo are retained; Java lookup is unchanged.
This does not implement KDoc generation through Add Javadoc or semantic completion.

## Refactored Existing Code

- **`JavaParentNode.tokenizeText()`**: `static` -> virtual (enables `KotlinParentNode` override via virtual dispatch; Java behavior unchanged)
- **`ParsedCUNode.handlesMultilineStrings()`**: new virtual method (base returns `false`; `KotlinParsedCUNode` returns `true`)
- **`JavaSyntaxView`**: `enableParser(ParsedCUNode, boolean)` overload + `handlesMultilineStrings()` check before `MultilineStringTracker` override

---

## Container+Inner Pattern

Each scope-creating construct produces two nodes:
- **Container** (gets type-specific color: green/yellow/blue/pink)
- **Inner node** (`NODETYPE_NONE`, `isInner=true`, gets neutral C3/BK body coloring)

Required because `JavaSyntaxView.drawNode()` checks `isContainer() || isInner()` to decide what to render. Braceless Kotlin bodies (`fun f() = expr`, `if (x) return y`) also get inner nodes spanning the expression.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Reuse JavaSyntaxView unchanged | Avoid duplicating ~2,793 lines of scope coloring + rendering |
| 2 | Full PSI parser for scope detection | Handles all Kotlin grammar correctly (trailing lambdas, when, etc.) |
| 3 | `KotlinParentNode` for all scope nodes | Ensures Kotlin tokenization at every tree level via virtual dispatch |
| 4 | `tokenizeText()` static -> virtual | Minimal change (remove `static`); Java behavior unchanged |
| 5 | Shared `java-colors.css` | Same CSS classes for consistent colors across languages |
| 6 | `FlowLanguageSupport` strategy | Eliminates 8 branch points in FlowEditor; polymorphic dispatch |
| 7 | No entity resolution (MVP) | PSI provides syntax but not type resolution; acceptable for MVP |
| 8 | `KotlinStringNode` for multiline strings | `MultilineStringTracker` forces all-green for Java text blocks; wrong for Kotlin `$template` |

---

## Known Limitations

- **~1-2 second first-parse delay** when `KotlinCoreEnvironment` initializes
- **No code completion** or type-aware highlighting for Kotlin. Ctrl+Space inside a `.kt` editor surfaces a transient caret-anchored popup ("Code completion is not available for Kotlin files") via `FlowEditor.showKotlinUnavailableNotice` — implemented as a `PopupControl` + custom `Skin` mirroring the existing `showErrorOverlay` shape, auto-dismissed after ~3s. This is an honest "not supported yet" signal rather than a working completion path; lifting it requires `BindingContext`-based type resolution (Tier 2, post-MVP).
- **PSI environment ~10-20 MB** memory
- **Full-file reparse per edit** (~50-100ms for 500 lines; debounced by `FlowReparseRunner`)
- **Multi-line `${...}` spanning lines** -- continuation lines may highlight incorrectly (rare)
