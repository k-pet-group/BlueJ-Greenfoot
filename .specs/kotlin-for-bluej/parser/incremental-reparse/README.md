---
id: kotlin-for-bluej-parser-incremental-reparse
type: submodule-design
title: "Incremental PSI Reparse for Kotlin"
status: active
parent: kotlin-for-bluej-parser
tags:
  - kotlin
---

# Incremental PSI Reparse for Kotlin

> Tiered incremental reparse strategy to reduce per-keystroke cost from ~50-100ms to ~0.1-20ms for common edits.

**Implementation status (2026-04-20):** Only Tier 3 (block-level) and Tier 4 (full-file fallback) are implemented. Tiers 1 and 2 remain future optimizations.

---

## Tiered Strategy

| Tier | Node Type | Strategy | Cost | Status |
|------|-----------|----------|------|--------|
| 1 | `KotlinCommentNode` | KotlinLexer validation | ~0.1ms | Future |
| 2 | `KotlinStringNode` | KotlinLexer validation | ~0.1ms | Future |
| 3 | `KotlinParentNode` (inner body) | `createFile()` wrapper + rebuild children | ~5-20ms | **Implemented** |
| 4 | `KotlinParsedCUNode` (root) | Full-file PSI reparse | ~50-100ms | **Implemented** |

---

## How It Works

The key enabler is BlueJ's existing `pollReparseQueue` in `JavaSyntaxView`: it walks the parse tree to find the **deepest node** containing the reparse position and calls `reparse()` on that node. Each node type implements its own `reparseNode()`:

- `ALL_OK` -> done
- `REMOVE_NODE` -> remove self from parent, re-queue reparse (next poll finds parent)

### Fallback Chain

```
Edit inside function body:
  pollReparseQueue -> finds KotlinParentNode (inner body)
    -> block-level PSI reparse (~5-20ms)
    -> SUCCESS: ALL_OK
    -> FAILURE: REMOVE_NODE -> cascades up

  pollReparseQueue -> finds KotlinParsedCUNode (root)
    -> full-file PSI reparse (~50-100ms)
    -> always succeeds
```

---

## Tier 3 Detail (Implemented)

Inner body nodes (`isInner() == true`) override `reparseNode()`:
1. Read block text including enclosing braces
2. Wrap in synthetic function: `psiFactory.createFile("fun _() {" + innerContent)`
3. Extract block, rebuild children via `KotlinPsiScopeBuilder.buildScopesFromBlock()`
4. Return `ALL_OK` on success; `REMOVE_NODE` if syntax is broken

Container nodes always return `REMOVE_NODE` (can only be validated by full-file parsing).

---

## Future Tiers (1 and 2)

**Tier 1 (Comments)**: `KotlinCommentNode` would absorb edits locally (resize + scheduleReparse), then validate with single `KotlinLexer.nextToken()` call. If still a comment -> `ALL_OK` (~0.1ms). If structure broke -> `REMOVE_NODE`.

**Tier 2 (Strings)**: Same pattern for `KotlinStringNode` -- validate `"""` delimiters with KotlinLexer.

Both were designed but deferred in favor of the simpler always-`REMOVE_NODE` approach for comment/string nodes.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Tiered strategy | Most keystrokes are inside function bodies or comments -- tiers 1+3 cover ~90% of edits |
| 2 | Leverage existing `pollReparseQueue` | Infrastructure already dispatches to deepest node; just implement `reparseNode()` per type |
| 3 | `createFile()` wrapper for block reparse | More robust than `createBlock()` which may have different error-recovery behavior |
| 4 | Containers always REMOVE_NODE | Container boundaries (signatures, class headers) need full-file context to validate |
| 5 | Leaf nodes absorb edits locally | Prevents immediate REMOVE_NODE cascade; scheduled reparse is debounced by `FlowReparseRunner` |
| 6 | Boundary check only on non-containers | Inner/leaf nodes reject text at their end boundary to prevent sibling absorption (block-level reparse would silently succeed for absorbed content). Containers skip this check so they survive `textInserted()` — their inner child REMOVE_NODEs instead, keeping the tree stable for the paint cycle and avoiding keystroke swallowing |

---

## Known Limitations

- Expression-body functions (`fun f() = expr`) have no block body -- inner node returns REMOVE_NODE, cascading to full-file reparse via the container
- Block-level reparse doesn't update sibling/parent nodes (acceptable: scope coloring is syntax-only)
- Nested block reparse may cascade 3-4 hops before reaching root (still faster than full-file)

---

## Boundary Check in textInserted

`KotlinParentNode.textInserted()` rejects text appended at the exact end boundary (`insPos >= nodePos + getSize()`) for **non-container** nodes only. This prevents sibling absorption: without the check, an inner node (e.g., expression-body `20`) would grow to include new text, and the block-level reparse would silently succeed (`fun _() {20X` is valid block content), permanently keeping the wrong tree structure.

Container nodes (TYPEDEF, METHODDEF, SELECTION, ITERATION) skip this check. They delegate to `super.textInserted()`, which grows the container and passes the insertion to the inner child. The inner child then REMOVE_NODEs, triggering the reparse cascade while the container survives in the tree — keeping the display stable during the paint cycle between `textInserted()` and `FlowReparseRunner`.
