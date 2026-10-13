---
id: bluej-compiler
type: submodule-design
title: "Compiler"
status: active
parent: bluej
---

# Compiler

Shared single-threaded compilation queue for BlueJ and Greenfoot, coordinating javac and K2 over the
source set selected by callers. Source selection and dependency discovery belong to `bluej-pkgmgr`,
not the compiler queue.

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

## Request Contract

Java-only behavior is the baseline for all requests. A mixed request is one queued operation with two
internal stages, not two independently completed jobs. `CompileObserver` signatures are unchanged:
one start, one ascending sequence and one final completion cover the original selected inputs in order.
No phase completion may trigger intermediate target analysis, class loading or Greenfoot reset.

- Inputs, javac options, classpath and output-retention policy are captured per request. Shared backends
  are configured only on the compiler thread; enqueueing another request cannot replace the first
  request's Java symbol inputs. Paths are copied, **not file contents**: source is read at execution time.
- K2 runs first with selected Java files as symbol inputs. javac runs only after K2 succeeds, since K2
  does not emit their Java bytecode. Either phase failing makes the sole final result unsuccessful.
  Diagnostic ownership remains language-specific; see `kotlin-for-bluej-compiler`.
- Retained compilations write directly to project output, just as Java-only compilation does. Failure
  may leave old or partial class files; there is no staging, publication transaction or rollback.
- BlueJ check-only requests own temporary output shared through both stages. It precedes retained
  classes on the phase classpath, and is cleaned after compilation, including failed attempts. javac's
  source path stays rooted at the real project directory, not the temporary output directory.
  Greenfoot check-only retains classes under its existing `CompileType` policy.
- Java-only, Kotlin-only, generated Java invocation shells and existing Java/Stride routing keep their
  backend behavior. No Stride/Kotlin mixing guarantee or student-classloader dependency is added.

### Key Lifecycles

`JobQueue.addJob` creates the request from caller-selected non-null inputs; an empty selection enqueues
nothing. `CompilerThread` serializes execution, allowing producers to enqueue while compilation runs.
`Job` announces the full start, configures/runs the required stages, closes compilation resources and
cleans request-owned temporary output before final notification. Exceptions from compilation/start or
an end callback are logged; a throwing end callback is not retried. FX adapters keep their existing
synchronous dispatch. Direct public backend calls retain their own output cleanup; they do not own a
queue lifecycle.

The `busy` flag is set to `true` on enqueue and `false` when queue drains. `waitForEmptyQueue()` blocks on this flag.

### Limitations and Java-only Parity

**Temporary diagnostic difference:** a Kotlin failure postpones Java diagnostics until a later request
reaches javac. Running javac against missing or obsolete Kotlin output is not a safe substitute. Removing
this exception requires diagnostic-only analysis with a usable view of unsuccessful Kotlin declarations,
no trust in obsolete Kotlin bytecode, and coherent combined results/diagnostics. Merely installing the
Kotlin Analysis API does not prove this safe.

**Temporary recovery difference:** failed mixed requests do not get automatic dependency-based subset
recovery. The rationale and requirements for restoring parity belong to `bluej-pkgmgr`.

Source-selection correctness remains deferred until Kotlin Analysis API integration. This queue neither
expands the selected set nor adds a whole-project fallback. Missing inferred/body-use dependencies and
omitted stale callers remain known limits; request correctness is not full mixed-project reliability.
Stale editor completions remain subject to the existing target eligibility checks in `bluej-pkgmgr`.

---

## Dependencies

Uses: `utility/` (threading helpers)
