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

---

## FlowLanguageSupport Strategy

| Implementation | Parser Root | Supertype Syntax |
|----------------|-------------|------------------|
| `JavaLanguageSupport` | `ParsedCUNode` | `extends`/`implements` |
| `KotlinLanguageSupport` | `KotlinParsedCUNode` | `: SuperClass(), Interface` |
| `PlainTextLanguageSupport` | `null` (fail-fast) | throws `UnsupportedOperationException` |

FlowEditor delegates parser creation and source-editing (setExtendsClass, addImplements, etc.) to the strategy. Callers pass strategy explicitly at construction.

---

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
- **No code completion** or type-aware highlighting for Kotlin
- **PSI environment ~10-20 MB** memory
- **Full-file reparse per edit** (~50-100ms for 500 lines; debounced by `FlowReparseRunner`)
- **Multi-line `${...}` spanning lines** -- continuation lines may highlight incorrectly (rare)
