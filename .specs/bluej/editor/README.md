---
id: bluej-editor
type: submodule-design
title: "Source Code Editor"
status: active
parent: bluej
---

# Source Code Editor

Text and frame-based source code editing (67 files, 4 sub-packages). Supports dual editing modes: traditional text editing with syntax highlighting (Java), and Stride frame-based visual editing.

---

## Key Interfaces

| Class | Purpose |
|-------|---------|
| `Editor` (interface) | Abstract editor contract -- 36 methods, implemented by both FlowEditor and FrameEditor |
| `TextEditor` (interface) | Sub-interface extending `Editor` with 16 text-specific methods (buffer access, caret, selection) |
| `EditorWatcher` (interface) | Callback contract (24 methods) from editor to BlueJ infrastructure (implemented by `EditableTarget`) |

The package declares `@OnThread(Tag.FXPlatform)` -- all interface methods must be called on the JavaFX platform thread.

---

## Type-Safe Downcast Pattern

- `assumeText()` -> `TextEditor`: FlowEditor returns `this`; FrameEditor returns a proxy that throws on text-only operations
- `assumeFrame()` -> `FrameEditor`: FlowEditor returns `null`; FrameEditor returns `this`

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `base/` | Virtualized text rendering, scrolling, selection, line display |
| `flow/` | Java text editor: document model, undo, syntax/scope coloring, find/replace |
| `stride/` | Stride frame editor integration: tabs, catalogue, shelf, overlays |
| `fixes/` | Quick fixes: diagnostics-to-fix mapping, correction suggestions, code completion |

---

## EditorWatcher Callback Categories

- **Core events**: `modificationEvent()`, `saveEvent()`, `closeEvent()`
- **Compilation**: `scheduleCompilation(immediate, reason, type)`
- **Breakpoints**: `breakpointToggleEvent()`, `clearAllBreakpoints()`
- **Data recording**: 13 methods for Blackbox research system analytics
- **UI/Misc**: `generateDoc()`, `getPackage()`, `showPreferences()`, etc.

---

## Design Decisions

1. **Polymorphic Editor interface**: Works at semantic level (insertMethod, setExtends) so both text and frame editors implement it
2. **Type-safe downcast pattern**: `assumeText()`/`assumeFrame()` instead of raw casts. FrameEditor's TextEditor proxy throws `UnsupportedOperationException` on invalid operations.
3. **FrameEditor is NOT a JavaFX node**: Unlike FlowEditor (extends `BorderPane`), FrameEditor is just an integration class. The actual GUI lives in `FrameEditorTab`.
4. **Heavy analytics integration**: 13 of EditorWatcher's 24 methods are data recording for the Blackbox research system
5. **`@OnThread(Tag.FXPlatform)` everywhere**: Thread checker enforces single-threaded access at compile time

---

## Dependencies

Uses: `parser/`, `compiler/`, `utility/`
