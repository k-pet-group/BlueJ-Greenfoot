---
id: bluej-runtime
type: submodule-design
title: "Runtime (ExecServer)"
status: active
parent: bluej
---

# Runtime (ExecServer)

Debug VM execution environment (5 files). Runs inside the debug VM and provides the two-threaded execution model that BlueJ's debugger controls via JDI.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `ExecServer` | Main runtime controller; manages main thread and worker thread; owns static field IPC protocol |
| `Shell` (abstract) | Base for auto-generated `__SHELL<N>` classes; provides `getScope()`, `putObject()`, and `makeObj()` overloads |
| `BJMap<K,V>` | Simple array-backed map for scope storage; used instead of `HashMap` for JDI field visibility |
| `BJInputStream` | `InputStream` wrapper that intercepts EOF signals and triggers terminal display on input |
| `UnitTestExtension` | JUnit 5 `InvocationInterceptor` that captures parameterized test arguments for display |

---

## Static Field IPC Protocol

The IDE (`VMReference`) and the debug VM (`ExecServer`) communicate through **JDI reads/writes of `ExecServer`'s public static fields**. No sockets or serialization.

Key IPC fields: `classToRun`, `execAction`, `methodReturn`, `exception`, `parameterTypes`, `arguments`, `threadToRunOn`.

Synchronization is purely breakpoint-based: empty methods (`vmStarted()`, `vmSuspend()`) serve as JDI breakpoint targets.

---

## Two-Thread Model

- **Main thread (server)**: Executes user code. Respawned via `newThread()` after every command (workaround for JDI bug with exception events on reused threads). Sets `execAction = EXIT_VM` as failsafe in `finally`.
- **Worker thread**: Housekeeping (class loading, scope management) at `Thread.MAX_PRIORITY` to avoid starvation by user code.

---

## Main Thread Action Codes

| Constant | Purpose |
|----------|---------|
| `EXEC_SHELL` (0) | Execute an auto-generated `__SHELL<N>` class |
| `TEST_SETUP` (1) | Instantiate test class, run setUp(), return fixture fields |
| `TEST_RUN` (2) | Run JUnit 5 test via LauncherFactory |
| `DISPOSE_WINDOWS` (3) | Dispose all tracked AWT Window objects |
| `EXIT_VM` (4) | `System.exit(0)` -- also the failsafe default |
| `LOAD_INIT_CLASS` (5) | `Class.forName()` with initialization |
| `INSTANTIATE_CLASS` (6) | Default-constructor fast path |
| `INSTANTIATE_CLASS_ARGS` (7) | Parameterized constructor |
| `LAUNCH_FX_APP` (8) | `Application.launch()` via FXPreloader |

---

## Thread-Target Dispatch

User code can run on: default thread (0), FX thread (1), Swing EDT (2), or custom thread (3, used by Greenfoot for simulation). Non-default targets use `CompletableFuture` to marshal results back.

---

## BJMap Scope Lifecycle

- `objectMaps: Map<String, BJMap<String, Object>>` -- scopes keyed by package ID
- `newLoader()` clears ALL scopes because old object references are invalid under a new class loader
- `getObject()` blocks with `wait()` (used by Greenfoot to wait for bench objects)

---

## Design Decisions

- **Static fields as IPC**: JDI can read/write fields without invoking methods. No serialization overhead. Works even when all threads are suspended.
- **Breakpoint-based synchronization**: Simplest way to suspend/resume threads without locks that could deadlock with user code.
- **Thread respawning**: Works around a JVM/JDI bug where exception events become unreliable on reused threads.
- **`makeObj()` anonymous classes**: JDI auto-boxes primitives; typed `result` fields let the IDE recover the original primitive type.
- **`BJMap` over `HashMap`**: Simple array-backed structure makes fields directly visible to JDI inspection.
- **`execAction = EXIT_VM` as failsafe**: If the IDE crashes, the debug VM exits instead of hanging.
- **No direct Greenfoot dependency**: `customThreadRunner` (`Consumer<Runnable>`) allows Greenfoot to inject its simulation thread dispatch.

---

## Dependencies

**Controlled by (IDE side):** `VMReference` in `bluej/debugger/jdi/`, `Invoker` in `bluej/debugmgr/`
