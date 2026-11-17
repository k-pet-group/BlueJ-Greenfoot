---
id: bluej-terminal
type: submodule-design
title: "Terminal"
status: active
parent: bluej
---

# Terminal

Interactive I/O console (6 files) connected to the debug VM.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Terminal` | Main JavaFX console window; integrates with debugger for program I/O |
| `TerminalTextPane` | JavaFX text pane for styled terminal output display |
| `InputBuffer` | Thread-safe circular type-ahead buffer for terminal input |

---

## Threading

- Program stdout/stderr arrives from `IOHandlerThread` and is marshalled to FXPlatform via `Platform.runLater()`
- User keyboard input is written to `InputBuffer` from the FX thread, consumed by the debug VM's `BJInputStream`
- Terminal visibility triggered by a breakpoint in `ExecServer.showTerminalOnInput()` (suspend policy: `SUSPEND_NONE`)

---

## Design Decisions

- **`InputBuffer` as thread-safe circular buffer**: Decouples the FX-thread producer (keyboard) from the debug VM consumer
- **Breakpoint-triggered terminal show**: Debug VM doesn't pause -- it just notifies the IDE to make the terminal visible

---

## Source Links

Stack-frame recognition accepts `.java` and `.kt` source locations. Package lookup uses the declaring JVM
class, but target lookup uses the **reported source filename's stem**, not a nested class or generated
facade name. The link retains that filename, extension and reported line through the existing editor
navigation path. Java frames, including Stride's generated Java, keep their route.

A known package and source-stem target permit a link; foreign/native/unknown-source frames keep their
existing non-link styling. This is not Kotlin synthetic-line remapping or a general debugger change.
Focused matcher tests do not establish live click, keyboard activation or missing-source editor behavior.

## Dependencies

Uses: `debugger/` (program I/O), `utility/` (JavaFX helpers)
