---
id: bluej-utility
type: submodule-design
title: "Utility Library"
status: active
parent: bluej
---

# Utility Library

Shared utility classes used across all BlueJ packages (91 files). Provides JavaFX helpers, file operations, dialog management, debug logging, and observable bindings.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Debug` | Logging and debug output |
| `Utility` | General-purpose static helpers; owns the 8-thread `ScheduledExecutorService` background pool |
| `FileUtility` | File I/O operations, path handling |
| `JavaFXUtil` | JavaFX threading, property binding, UI helpers |
| `DialogManager` | Standard dialog creation and display |

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `javafx/` | JavaFX-specific utilities: threading helpers, custom controls, FX-annotated functional interfaces |
| `javafx/dialog/` | Custom dialog components |
| `javafx/binding/` | Observable binding utilities for reactive UI |
| `filefilter/` | File filtering implementations |

---

## Threading Infrastructure

- **Background pool**: `Utility.runBackground()` dispatches to an 8-thread `ScheduledExecutorService`
- **FX dispatch**: `JavaFXUtil.runNowOrLater()` (if on FX thread, runs immediately), `runPlatformLater()` (always posts), `runAfterCurrent()` (defers past current event handler)
- **Blocking cross-thread**: `runPlatformAndWait()` -- 6 overloads using `CompletableFuture.get()`. Deadlock warning: will deadlock if FX thread is waiting on the calling thread.
- **Non-blocking FX futures**: `runPlatformFuture()` -- 6 matching overloads returning `Future<T>`
- **Timer-based**: `runAfter(Duration)` and `runRegular(Duration)` via JavaFX `Timeline`

---

## Design Decisions

- **`CompletableFuture` for FX bridging**: More composable than `CountDownLatch`; supports both blocking (`.get()`) and chaining (`.thenApply()`)
- **`runNowOrLater` vs `runPlatformLater`**: `runNowOrLater` avoids unnecessary `Platform.runLater()` overhead but is unsafe from FX loading threads
- **`Timeline` for FX-thread timers**: Fires natively on the FX Application Thread without `Platform.runLater()` wrappers

---

## Dependencies

**External:** JavaFX, Guava

Used by: virtually every other BlueJ package
