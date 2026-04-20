# Debug Manager (UI)

User-facing debugging interface (~41 files). Provides the Object Bench for storing live objects, inspectors for viewing object state, a code pad for expression evaluation, and execution controls. Orchestrates the full invocation lifecycle: shell code generation -> compilation -> execution in the debug VM -> result unwrapping.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Invoker` | Generates shell source (`__SHELL{N}.java`), compiles it, executes it in the debug VM, and processes results |
| `ObjectBench` | Stores and displays live object references on the bench UI |
| `ObjectWrapper` | Wraps a debug VM object for display; provides context menu with method invocation |
| `ResultWatcher` (interface) | Callback interface for invocation lifecycle events |
| `Shell` (in `bluej.runtime`) | Base class for generated shell files; provides `makeObj()` result wrappers and scope access |
| `ExecControls` | Execution control panel: step over, step into, continue, halt |

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `objectbench/` | Object bench: stores live objects, drag-and-drop, context menus, `ObjectWrapper`, result watchers |
| `inspector/` | Object, array, and class inspectors for viewing state |
| `codepad/` | Code pad for evaluating expressions interactively |

---

## Invocation Flow

1. User invokes a method/constructor from bench or class target
2. `Invoker` generates `__SHELL{N}.java` extending `bluej.runtime.Shell` with bench variables in scope
3. Shell class is compiled, then executed on the debug VM server thread
4. `handleResult()` unwraps the result from `Shell.makeObj()` anonymous wrappers
5. `ResultWatcher` callbacks deliver result to bench/inspector

**Constructor fast path**: Parameterless constructors bypass shell generation entirely -- `debugger.instantiateClass()` is called directly on a worker thread.

---

## Shell Code Generation Contracts

- **SCOPEINIT**: All bench variables are injected via `Shell.getScope()` using the *accessible* type determined by `ObjectWrapper.findIType()`
- **Constructor results**: Wrapped in `new Object() { MyClass __bluej__result__; ... }` to preserve exact constructed type via JDI field inspection
- **Non-void method results**: Wrapped via `Shell.makeObj()` anonymous classes with typed `result` field to preserve primitive/boxed distinction
- **Void methods**: Direct call, no wrapping

---

## Design Decisions

- **Shell files over direct JDI invocation**: Enables expression evaluation, type-safe overload resolution, and natural bench-object-as-variable syntax
- **Anonymous wrapper for constructors**: Preserves exact type (including generics) via JDI field inspection, since JDI can inspect field types but not return value types
- **`Shell.makeObj()` for primitives**: Anonymous classes with typed `result` fields let the debugger distinguish `int` from `Integer` via field type inspection

---

## Threading

- All `ResultWatcher` callbacks execute on `@OnThread(Tag.FXPlatform)`
- Shell source generation: FXPlatform
- Compilation: CompilerThread via `JobQueue`
- Debug VM execution: server thread via JDI
- Constructor fast path: dedicated worker thread (prevents infinite-loop constructors from freezing UI)

---

## Dependencies

Uses: `debugger/`, `runtime/`, `pkgmgr/`, `views/`, `testmgr/record/`
