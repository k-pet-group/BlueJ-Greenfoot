# Package Manager (Central Orchestrator)

The central orchestrator of BlueJ (115 files). Manages projects, packages, class targets, and the visual class diagram. Nearly every other subsystem connects through pkgmgr.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Project` | Represents an open BlueJ project; owns compiler, debugger, and packages |
| `Package` | Represents a Java package; manages class targets and dependencies |
| `PkgMgrFrame` | Main IDE window showing the class diagram and menu actions |
| `ClassTarget` | Visual representation of a class; tracks compiled/error state |
| `Target` (abstract) | Base class for all diagram elements |

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `target/` | Class/interface targets and roles in the diagram |
| `dependency/` | Dependency graph management (extends, implements, uses) |
| `actions/` | Menu actions: new class, open, compile, run, etc. |
| `print/` | Printing support for class diagrams |

---

## Key Patterns

- **`Project` -> `Package` -> `ClassTarget`** -- Core model hierarchy
- **`PackageListener`** -- Observer for package-level events
- **`PkgMgrFrame`** acts as the main IDE frame, delegating to all subsystems

---

## Threading

Package manager is a threading hotspot:
- `Package` and `Project` are primarily `@OnThread(Tag.FXPlatform)` but contain `synchronized` sections accessed from Worker/CompilerThread
- `PkgMgrFrame` is strictly FX-thread-only
- `Package.java` has **42 synchronized blocks** protecting shared mutable state between FX thread, CompilerThread, and Worker threads

---

## Design Decisions

- **Heavy synchronization in Package**: `Package` is the central coordination point between UI, compiler, and worker threads. The 42 synchronized blocks protect shared mutable state from concurrent access.
- **1-second compilation batching**: `Project.scheduleCompilation()` uses a timer that resets on each new request, coalescing rapid edits into a single compilation.
- **`DependentTarget` synchronized state**: Class targets are updated from the compiler thread (diagnostics) and read from the FX thread (rendering), requiring synchronization.

---

## Dependencies

Uses: `compiler/`, `debugger/`, `editor/`, `parser/`, `groupwork/`, `extensions2/`, `terminal/`, `graph/`
