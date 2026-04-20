# Flow Text Editor

The traditional text-based Java editor (27 files). Implements the `TextEditor` interface with a custom document model (gap buffer), undo/redo, syntax and scope coloring, find/replace, and incremental parser integration.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `FlowEditor` | Main text editor; extends `ScopeColorsBorderPane`, implements `TextEditor` |
| `FlowEditorPane` | Custom text rendering pane (extends `BaseEditorPane`) -- NOT JavaFX TextArea |
| `HoleDocument` | Gap-buffer document with change tracking and tracked positions |
| `DocumentUndoStack` | Undo/redo with compound edit grouping based on timing, type, and contiguity |
| `JavaSyntaxView` | Maps parser scope tree to syntax CSS classes and scope background colors; implements `NodeStructureListener` |
| `FlowErrorManager` | Compiler diagnostic display with squiggly underlines and quick-fix integration |
| `FlowActions` | Editor action definitions with key binding management |
| `FindPanel` / `FindNavigator` | Find/replace UI and navigation logic |
| `FlowIndent` | Auto-indentation using parser scope analysis |
| `TrackedPosition` | Caret/anchor positions that auto-adjust on text edits; has `Bias` (FORWARD/NONE) |

---

## Key Contracts

- `FlowEditor` is both a JavaFX UI component AND the `TextEditor` implementation
- `assumeText()` returns `this`; `assumeFrame()` returns `null`
- `HoleDocument` uses a gap buffer: O(1) amortized sequential edits, O(n) random-access edits
- `DocumentUndoStack` merges edits based on time proximity (<500ms), type (inserts vs deletes), and contiguity; paste/find-replace always create separate undo units
- Only visible lines are tokenized and styled (lazy `StyledLines` AbstractList)

---

## Scope Coloring

`JavaSyntaxView` acts as a `NodeStructureListener` on the parser's scope tree:
1. Parser updates `ParsedNode` tree
2. `getBackgrounds(lineIndex)` walks the tree to find overlapping scopes
3. Creates `BackgroundItem` nodes with scope colors from `ScopeColors` palette
4. `FlowEditorPane` distributes backgrounds to visible lines

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
