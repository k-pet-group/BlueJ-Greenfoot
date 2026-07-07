---
id: kotlin-for-bluej-editor-psi-scope
type: submodule-design
title: "PSI-Based Scope Detection"
status: active
parent: kotlin-for-bluej-editor
tags:
  - kotlin
---

# PSI-Based Scope Detection

> Uses the full Kotlin PSI parser from `kotlin-compiler-embeddable` to build accurate scope structure for `JavaSyntaxView` scope coloring.

---

## Overview

Delegates scope detection to the battle-tested PSI parser bundled inside `kotlin-compiler-embeddable`. PSI understands full Kotlin grammar (trailing lambdas, `when` expressions, string templates, destructuring), guaranteeing 100% accurate scope boundaries.

- **MVP**: Full-file PSI reparse on every edit via `KtPsiFactory.createFile()` (~50-100ms for 500 lines)
- **Shared environment**: Singleton `KotlinEnvironmentManager` creates one `KotlinCoreEnvironment`, shared between editor and compiler. ~1-2s setup cost paid once, lazily on first `.kt` file open.

---

## PSI Node -> BlueJ Node Mapping

All scope nodes are `KotlinParentNode` with configurable `nodeType`. Each container has an inner node for body content (container+inner pattern).

| PSI Node | Container Node Type | Scope Color |
|----------|-------------------|-------------|
| `KtClass` | `NODETYPE_TYPEDEF` | Green |
| `KtObjectDeclaration` (incl. companion) | `NODETYPE_TYPEDEF` | Green |
| `KtNamedFunction` | `NODETYPE_METHODDEF` | Yellow |
| `KtSecondaryConstructor` | `NODETYPE_METHODDEF` | Yellow |
| `KtClassInitializer` (init block) | `NODETYPE_METHODDEF` | Yellow |
| `KtIfExpression` / `KtWhenExpression` | `NODETYPE_SELECTION` | Blue |
| `KtLoopExpression` (for/while/do) | `NODETYPE_ITERATION` | Pink |
| `PsiComment` | `KotlinCommentNode` (COMMENT) | Comment styling |
| `KtStringTemplateExpression` (multiline) | `KotlinStringNode` | STRING_LITERAL gaps + code children |

**Not scoped**: `KtPrimaryConstructor` (no block body), `KtProperty`.

Inner nodes span content between braces for block bodies, or the expression itself for braceless bodies (`fun f() = expr`, `if (x) return y`).

---

## Key Contracts

### KotlinEnvironmentManager

- Lazy singleton; thread-safe (double-checked locking)
- `getEnvironment()`, `getProject()`, `getPsiFactory()`, `dispose()`, `isInitialized()`

### KotlinPsiScopeBuilder

- `buildScopesFromFile(KtFile, parent, baseOffset, listener)` -- full-file scope tree construction
- `buildScopesFromBlock(KtBlockExpression, parent, baseOffset, listener)` -- block-level (for incremental reparse)
- Comment detection at 4 levels: file, class body, block expression, attached to declarations (via AST traversal)
- String template handling: multiline `"""..."""` -> `KotlinStringNode` with children for `$name`/`${expr}`
- Companion objects scoped as green type declarations (anonymous ones named "Companion")
- Uses type-specific PSI getters for control flow bodies (`getBody()`, `getThen()`/`getElse()`, `getEntries()`)

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Full PSI parser | Handles all Kotlin grammar correctly; already on classpath |
| 2 | Shared singleton environment | Avoids two heavyweight environments; ~1-2s cost paid once |
| 3 | Full-file reparse for MVP | Simpler and robust for 100-500 line files |
| 4 | PSI tree -> KotlinParentNode conversion | Ensures Kotlin tokenization at every level; PSI tree discarded after conversion |
| 5 | Container+inner pattern | Required by `JavaSyntaxView.drawNode()` for correct body backgrounds |
| 6 | Type-specific PSI getters for loops/branches | Kotlin wraps bodies in `KtContainerNode` making generic `getChildren()` unreliable |
| 7 | Secondary constructors as METHODDEF (yellow) | Matches Java treatment; structurally identical to method bodies |
| 8 | Primary constructors skipped | No block body; `init` blocks serve this purpose |
| 9 | Init blocks as METHODDEF (yellow) | Initializer code analogous to constructors |
| 10 | Companion objects scoped | Treated like any other `object` (green type scope); members highlighted within the companion body. Diagram representation is unaffected (separate `KotlinInfoParser`/compiled-class path) |
| 11 | KotlinCommentNode | Returns single comment-typed token; prevents keyword highlighting in comments |
| 12 | KotlinStringNode | Handles `$template` expressions correctly where `MultilineStringTracker` cannot |
| 13 | `handlesMultilineStrings()` virtual method | OOP polymorphism keeps `JavaSyntaxView` language-agnostic |

---

## Known Limitations

- **~1-2s first-parse delay** (environment initialization)
- **~50-100ms full-file reparse per edit** (debounced; future: block-level incremental)
- **PSI environment ~10-20 MB** memory
- **Shaded class paths** may change between Kotlin versions (isolated to 2 files)
- **No semantic resolution** (sufficient for scope coloring; type resolution is post-MVP)
