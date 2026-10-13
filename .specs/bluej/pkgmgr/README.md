---
id: bluej-pkgmgr
type: submodule-design
title: "Package Manager (Central Orchestrator)"
status: active
parent: bluej
---

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

## Compilation Completion and Recovery

Package receives one full-selected-source lifecycle from `bluej-compiler`, not per-language completion.
Its existing final processing remains authoritative: clear selected targets' queue flags, update metadata
and analyse eligible compiled targets, send extension events and notify chained observers. No intermediate
language result may clear request state or trigger those completion actions.

Target readiness follows Java-only rules. `ClassTarget.markCompiling` records whether the editor is
already modified; invalidation during compilation marks the result stale. `markCompiled` must retain
that stale-edit rejection and only newly mark a target compiled on successful retained output with
current class files and no known error. Whole-request failure does **not** force already-COMPILED targets
to an error state or erase output; ordinary final eligibility is preserved. Greenfoot's final reset policy
is not replaced with a new success-only guard.

Automatic dependency-based subset recovery remains available for failed explicit single-language
requests, with the existing NEEDS_COMPILE and dependency-error filtering. Successful requests (including
empty files), check-only, internal and other compile types do not enter that recovery path. Normal
queued compiles and editor-requested recompilation are separate and remain unchanged.

**Temporary mixed-request exception:** dependency-based subset recovery is suppressed after a failed
mixed request, since the current dependency graph cannot prove a subset independent of the failed
language phase. Restoring Java-only parity requires reliable cross-language dependency/error reachability,
including body uses and inferred types, revision-aware independence checks, and coherent follow-up
requests. This belongs with deferred source-selection/Kotlin Analysis API work, not a whole-project
compilation workaround. Normal requested-source selection is unchanged.

Policy regressions cover the request/type/language gate, including generated Java from Stride. They do
not instantiate a real Project/editor/debugger and therefore do not establish live stale-completion,
target-state or Greenfoot reset acceptance.

---

## Dependencies

Uses: `compiler/`, `debugger/`, `editor/`, `parser/`, `groupwork/`, `extensions2/`, `terminal/`, `graph/`
