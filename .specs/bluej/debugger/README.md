---
id: bluej-debugger
type: submodule-design
title: "Debugger (JDI)"
status: active
parent: bluej
---

# Debugger (JDI)

Abstract debugger interface with a JDI (Java Debug Interface) implementation (59 files). Manages a debug VM where user code runs, handling breakpoints, stepping, variable inspection, and object creation.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Debugger` (abstract) | Abstract debugger contract defining state machine and operations |
| `JdiDebugger` | JDI-based implementation; persistent across VM sessions |
| `VMReference` | Manages the connection to one debug JVM process; uses field rendezvous protocol |
| `VMEventHandler` | Thread that processes JDI events: breakpoints, step completions, exceptions, VM death |
| `JdiObject` | Wraps a JDI `ObjectReference` with GC-pinning lifecycle |
| `DebuggerResult` | Wraps the outcome of a debugger invocation (normal/exception/terminated) |

---

## State Machine

`UNKNOWN -> NOTREADY -> IDLE <-> RUNNING <-> SUSPENDED`

`NOTREADY -> LAUNCH_FAILED` on startup error. `IDLE/SUSPENDED -> NOTREADY` on VM restart.

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `jdi/` | JDI implementation: JdiDebugger, JdiThread, JdiObject, VMReference, VMEventHandler |
| `gentype/` | Generic type handling for inspecting parameterized types in the debug VM |

---

## Field Rendezvous Protocol

BlueJ communicates with the debug VM by **writing `ExecServer` static fields via JDI** (`ClassType.setValue()`), then resuming suspended threads. No sockets, no serialization.

- `vmStarted()` / `vmSuspend()` -- empty methods with JDI breakpoints for synchronization
- The same pattern is used by `runShellClass()`, `instantiateClass()`, `launchFXApp()`, `runTestSetUp()`, `runTestMethod()`

---

## JdiObject GC Pinning

`JdiObject` pins remote objects via `disableCollection()` on construction and `enableCollection()` in `finalize()`. This prevents the debug VM GC from collecting objects displayed on the bench or under inspection.

---

## VMEventHandler Architecture

Two-thread design: a helper sub-thread reads from the JDI `EventQueue` (blocking) and forwards `EventSet`s into a `LinkedBlockingQueue`. The main handler thread processes events in two passes:
1. **Pass 1 (screen)**: Pre-suspends threads marked `DONT_RESUME` before blanket `eventSet.resume()`
2. **Pass 2 (handle)**: Dispatches each event to the appropriate `VMReference` handler

---

## Threading and Lock Ordering

| Monitor | Protects |
|---------|----------|
| `JdiDebugger.this` | VM lifecycle state (`vmRef`, `machineLoader`, `autoRestart`) |
| `serverThreadLock` | Serializes server thread usage |
| `VMReference.this` | `serverThreadStarted` flag |
| `workerThread` | Worker thread reservation and readiness |
| `eventHandler` | Resume + state change atomicity |

Deadlock avoidance: `newClassLoader()` releases `JdiDebugger.this` before calling `VMReference.newClassLoader()`.

---

## Design Decisions

- **Field rendezvous over JDI method invocation**: Avoids JDI's `invokeMethod()` which is complex and can deadlock. Static field writes + breakpoint synchronization is simpler and works even when all threads are suspended.
- **Thread replacement after exceptions**: `ExecServer.newThread()` creates a fresh server thread after each invocation -- workaround for a JDI bug where exception events become unreliable on reused threads.
- **Two-pass event processing**: Pass 1 pre-suspends `DONT_RESUME` threads before the blanket `eventSet.resume()`, avoiding the need for `SUSPEND_ALL` policies.
- **Separate monitors per concern**: Distinct locks prevent lock-ordering deadlocks while providing fine-grained synchronization.

---

## Dependencies

**External:** JDI (Java Debug Interface, bundled with JDK)

Uses: `runtime/` (ExecServer, Shell), `debugmgr/` (UI)

---

## Existing Documentation

`bluej/doc/BlueJ-architecture-and-design.txt`
