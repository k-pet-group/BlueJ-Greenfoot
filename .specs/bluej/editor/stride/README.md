# Stride Editor Integration

Integrates the Stride block-based editor into BlueJ's editor framework (21 files). Provides tabbed editing (shared with Java text editors), frame catalogue (block palette), frame shelf (saved blocks), bird's eye view, overlay panes, and error overview bar.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `FrameEditorTab` (118 KB) | Central GUI coordinator and `InteractionManager`: frame tree, cursors, selection, undo, menus |
| `FrameEditor` (52 KB) | `Editor` interface bridge (non-visual); loads `.stride` XML, lazy panel creation |
| `FXTabbedEditor` (38 KB) | Multi-tab window container; frame drag-and-drop coordination |
| `FrameCatalogue` | Right-side palette of available frame blocks with keyboard shortcuts |
| `FrameShelf` | Per-window saved frame snippet canvas; synced across windows via `FrameShelfStorage` |

---

## Key Contracts

- **FrameEditor is NOT a visual component**: Unlike FlowEditor (extends BorderPane), it's a non-visual bridge. GUI lives in `FrameEditorTab`.
- **No text <-> frame runtime conversion**: `.stride` files are XML; `.java` files are generated on save. Java Preview shows generated Java within frame UI, not in a text editor.
- **Save produces two outputs**: `.stride` (XML frame structure) and `.java` (auto-generated Java source)
- **`assumeText()`** returns a proxy TextEditor that throws `UnsupportedOperationException` on text-specific operations

---

## Tab Hierarchy

- `FXTab` (abstract, extends `javafx.scene.control.Tab`)
  - `FrameEditorTab` -- Stride frame editor (shows catalogue sidebar)
  - `FlowFXTab` -- Java text editor wrapper (no catalogue)
  - `WebTab` -- Embedded WebView for Javadoc/tutorials

---

## View Modes

`NORMAL`, `BIRDSEYE_NODOC`, `BIRDSEYE_DOC`, `JAVA_PREVIEW` -- transitions use `SharedTransition` (500ms for birdseye, 3000ms for Java preview).

---

## Frame Drag-and-Drop

Coordinated by `FXTabbedEditor` (not individual tabs) because frames can be dragged between tabs. Hover-for-500ms switches tabs during drag. Shelf-to-editor = Copy; Editor-to-editor = Move.

---

## Design Decisions

- **InteractionManager pattern**: `FrameEditorTab` is the central interface through which all frames, slots, and cursors communicate
- **Background thread loading**: Stride frames are constructed off the FX thread and handed off via `runPlatformLater()`
- **Shared tab container**: FXTabbedEditor hosts both Stride and Java tabs with a shared menu bar
- **Per-project shelf persistence**: `FrameShelfStorage` saves to `shelf.xml` and syncs across all windows
