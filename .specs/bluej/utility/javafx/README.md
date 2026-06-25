# JavaFX Utilities

JavaFX-specific utility classes (59 files + 3 dialog + 5 binding).

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `JavaFXUtil` | Static utility methods: animations, bindings, thread management |
| `FXAbstractAction` | Base for UI actions with keyboard shortcuts |
| `ResizableCanvas` | Canvas that resizes with its parent |

---

## Thread-Safe Functional Interfaces

FX-thread-annotated variants of standard functional interfaces: `FXConsumer`, `FXFunction`, `FXPlatformConsumer`, `FXPlatformFunction` -- enforce `@OnThread` at the type level.

---

## Reactive Bindings (`javafx/binding/`)

`DeepListBinding`, `ConcatListBinding`, `ConcatMapListBinding` -- nested and concatenated observable list synchronization.

---

## Dialogs (`javafx/dialog/`)

`InputDialog` (text input with validation), `DialogPaneAnimateError` (error animation effects).
