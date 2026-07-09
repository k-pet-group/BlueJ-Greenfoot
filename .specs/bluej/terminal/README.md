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

## Dependencies

Uses: `debugger/` (program I/O), `utility/` (JavaFX helpers)
