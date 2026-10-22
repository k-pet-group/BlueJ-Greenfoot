---
id: bluej-editor-flow
type: submodule-design
title: "Flow Text Editor"
status: active
parent: bluej-editor
---

# Flow Text Editor

The traditional text-based Java editor (27 files). Implements the `TextEditor` interface with a custom document model (gap buffer), undo/redo, syntax and scope coloring, find/replace, and incremental parser integration.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `FlowEditor` | Main text editor; extends `ScopeColorsBorderPane`, implements `TextEditor` |
| `FlowEditorPane` | Custom text rendering pane (extends `BaseEditorPane`) -- NOT JavaFX TextArea |
| `HoleDocument` | Gap-buffer document with change tracking and tracked positions |
| `DocumentUndoStack` | Undo/redo with explicit compound edit grouping |
| `JavaSyntaxView` | Maps parser scope tree to syntax CSS classes and scope background colors; implements `NodeStructureListener` |
| `FlowErrorManager` | Compiler diagnostic display with squiggly underlines and quick-fix integration |
| `FlowActions` | Editor action definitions with key binding management |
| `FindPanel` / `FindNavigator` | Find/replace UI and navigation logic |
| `FlowIndent` | Auto-indentation; default tree-walking implementation for Java + dispatch hook for `FlowLanguageSupport.calculateIndents` (Kotlin) |
| `KotlinIndent` | Token-driven Kotlin auto-indenter; no parse-tree dependency |
| `TrackedPosition` | Caret/anchor positions that auto-adjust on text edits; has `Bias` (FORWARD/NONE) |

---

## Key Contracts

- `FlowEditor` is both a JavaFX UI component AND the `TextEditor` implementation
- `assumeText()` returns `this`; `assumeFrame()` returns `null`
- `HoleDocument` uses a gap buffer: O(1) amortized sequential edits, O(n) random-access edits
- A document replacement creates one undo unit unless explicitly grouped in a compound edit; there is no timing/contiguity-based merge in `DocumentUndoStack`
- Compound undo groups history only: document listeners still see individual replacements and exceptions do not roll them back. Coordinated Kotlin preparation changes therefore use one enclosing replacement (see `kotlin-for-bluej-editor`).
- Only visible lines are tokenized and styled (lazy `StyledLines` AbstractList)
- Deferred scope repaint requests are checked against the current document line count after layout;
  undo/deletion may remove a queued line before it is processed. Such requests are discarded rather than
  indexing stale lines. This does not change undo grouping or document notification behavior.
- Insert Method follows the editor's source language; Kotlin template/fallback behavior is owned by
  `kotlin-for-bluej-editor`.

---

## Scope Coloring

`JavaSyntaxView` acts as a `NodeStructureListener` on the parser's scope tree:
1. Parser updates `ParsedNode` tree
2. `getBackgrounds(lineIndex)` walks the tree to find overlapping scopes
3. Creates `BackgroundItem` nodes with scope colors from `ScopeColors` palette
4. `FlowEditorPane` distributes backgrounds to visible lines

---

## Auto-Indent Dispatch

`FlowIndent.calculateIndentsAndApply` checks the parser root type with one
`instanceof` and routes Kotlin sources to `KotlinIndent`; everything else
falls through to the default tree-walking implementation that walks
`NodeAndPosition<ParsedNode>` and `JavaParentNode.isSwitchBlockNode()`.

| Parser root | Path |
|---|---|
| `KotlinParsedCUNode` | `KotlinIndent.calculateIndentsAndApply(...)` — token-driven; no parse-tree read; multi-line strings flagged `LEAVE_UNTOUCHED` |
| Anything else (incl. `null`) | Tree-walking via `IndentCalculator` / `NodeIndentCalculator`, including `checkMethodSpacing` |

Java behavior is bit-identical to the pre-feature implementation (verified
by `TestAutoIndent` passing unchanged).

---

## Design Decisions

- **Custom rendering over TextArea**: Needed for scope backgrounds, inline error squiggles, and per-token syntax highlighting
- **Gap buffer**: O(1) amortized sequential editing (the dominant typing pattern)
- **English message matching for fixes**: javac doesn't expose stable error codes; `DiagnosticMessage` provides both localized and English messages
- **Lazy line styling**: Avoids syntax-highlighting off-screen lines
- **CSS-driven syntax colors**: Allows theme customization through stylesheets

---

## Dependencies

Uses: `editor/base/` (rendering), `parser/` (incremental parsing, scope tree), `editor/fixes/` (quick fixes)
