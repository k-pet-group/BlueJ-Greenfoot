# Compiler

Single-threaded compilation queue (14 files) using the javax.tools Java Compiler API.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Compiler` (abstract) | Abstract compiler interface; configures destination, classpath, debug/deprecation flags |
| `CompilerAPICompiler` | Concrete implementation using `javax.tools.JavaCompiler` API |
| `JobQueue` (singleton) | Thread-safe queue of pending compilation `Job` records |
| `CompilerThread` | Dedicated thread that processes jobs from the queue |
| `CompileObserver` (interface) | Lifecycle callbacks: `startCompile()`, `compilerMessage()`, `endCompile()` |
| `FXCompileObserver` | Marshals observer callbacks to FX platform thread via `Platform.runLater()` |
| `CompileType` (enum) | EXPLICIT_USER_COMPILE, ERROR_CHECK_ONLY, INTERNAL_COMPILE, EXTENSION, INDIRECT_USER_COMPILE |
| `CompileReason` (enum) | EARLY, LATE, INVOKE, REBUILD, EXTENSION, LOADED, MODIFIED, etc. |

---

## Design Decisions

- **Single dedicated compilation thread** at lower-than-normal priority keeps UI responsive while serializing all compilations
- **`Job.compile()` runs outside synchronized block** -- allows enqueueing new jobs during compilation
- **Observer adapter pattern** decouples the compiler thread from UI threading; same observer interface works for FX and Swing targets
- **1-second compilation batching** in `Project.scheduleCompilation()` coalesces rapid edits into a single compile

---

## Key Invariant

The `busy` flag is set to `true` on enqueue and `false` when queue drains. `waitForEmptyQueue()` blocks on this flag.

---

## Dependencies

Uses: `utility/` (threading helpers)
