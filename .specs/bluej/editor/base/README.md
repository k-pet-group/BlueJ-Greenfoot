---
id: bluej-editor-base
type: submodule-design
title: "Editor Base Components"
status: active
parent: bluej-editor
---

# Editor Base Components

Low-level editor rendering foundation (7 files). Provides a custom virtualized text editor built on JavaFX primitives (Region, TextFlow, Text nodes) -- NOT using JavaFX TextArea. Used by both the code editor (`FlowEditorPane`) and the terminal (`TerminalTextPane`).

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `BaseEditorPane` | Abstract `Region` subclass: scrolling, selection, caret, input handling |
| `LineDisplay` | Virtualization engine: only creates JavaFX nodes for on-screen lines (`HashMap<Integer, MarginAndTextLine>`) |
| `TextLine` | Extends `TextFlow`: renders one line of styled text with scope backgrounds, selection, error underlines |
| `MarginAndTextLine` | Composite `Region`: left margin (line numbers, breakpoint/step icons) + `TextLine` content |
| `BackgroundItem` | Lightweight `Region` for scope-colored backgrounds behind code |
| `LineContainer` | Simple `Region` that vertically stacks lines with uniform line height |
| `EditorPosition` (interface) | `getPosition()`, `getLine()`, `getColumn()` (zero-based) |

---

## Key Design Decisions

- **No JavaFX TextArea**: Built from `Region`, `TextFlow`, and `Text` primitives for scope backgrounds, inline error squiggles, breakpoint icons, and virtualized rendering
- **Virtualized rendering**: Only visible lines have JavaFX nodes. Lines scrolled out of view are removed from the scene graph.
- **TextFlow for text layout**: Each `TextLine` extends `TextFlow`, getting free text layout, character positioning, and hit testing
- **Scroll events at leaf level**: Handled on `MarginAndTextLine` (not `BaseEditorPane`) to work around macOS trackpad gesture issues
- **BackgroundItem as unmanaged Region**: Scope backgrounds positioned manually by `TextLine.layoutChildren()`, not by TextFlow layout
- **Structural comparison for backgrounds**: `BackgroundItem.sameAs()` avoids overriding `equals()` (which would break JavaFX identity semantics) while enabling diff-based updates

---

## Key Invariants

- Margin width: 32px (text starts at `TEXT_LEFT_EDGE`)
- Error underlines: 3px-wide zigzag `Path` with `Color.RED` stroke
- Adjacent `StyledSegment`s with identical CSS classes are merged to reduce `Text` node count
- Vertical scroll is batched with 50ms delay; drag auto-scroll at 50ms intervals
