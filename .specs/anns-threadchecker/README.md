---
id: anns-threadchecker
type: module-design
title: "Thread-Checker Annotations"
status: active
parent: bluej-greenfoot-architecture
---

# Thread-Checker Annotations

Pure annotation definitions used by the `threadchecker` compiler plugin and all other modules. Declares which thread a method, class, or package should execute on. Zero external dependencies.

**3 Java files.**

---

## Public Interface

- **`@OnThread(Tag)`** — Declares thread affinity. Targets: METHOD, CONSTRUCTOR, TYPE, PACKAGE, TYPE_USE. Retention: CLASS.
  - `ignoreParent` (default false) — Suppress override-checking against parent methods
  - `requireSynchronized` (default false) — Field access requires `synchronized(this)`

- **`Tag`** (enum) — Thread identity tags:
  - `FXPlatform` — Strictly the JavaFX Application Thread only
  - `FX` — FX Application Thread OR an FX loading thread (superset of `FXPlatform`)
  - `Swing` — Swing EDT (legacy, nearly eliminated)
  - `Simulation` — Greenfoot simulation thread (debug VM only)
  - `Worker` — Background worker threads
  - `VMEventHandler` — JDI debugger event-handler thread
  - `NOTVMEventHandler` — Any thread except VMEventHandler (prevents deadlock)
  - `Any` — Safe to call from any thread

- **`LocatedTag`** (package-private) — Tag wrapper with resolution metadata (`ignoreParent`, `requireSynchronized`, `applyToAllSubclassMethods`, origin `info`). Equality based on `tag` field only. Has a static `foundTags` cache for diagnostics.

---

## Tag Compatibility Rules

**Calling (`Tag.canCall`):**
- `Any`/untagged destinations callable from anywhere
- `FXPlatform` can call `FX` (FXPlatform is a subset of FX), but NOT vice versa
- `NOTVMEventHandler` callable from any thread except `VMEventHandler`
- Otherwise, tags must match exactly

**Override (`Tag.canOverride`):**
- `Any` can override any parent tag
- `FX` can override `FXPlatform` (widening)
- Untagged parents can only be overridden by `Any`
- Otherwise, must match exactly

---

## Design Decisions

- **CLASS retention** — Must be visible in `.class` files for cross-compilation-unit checking via `AnnotationMirror`, but runtime reflection is not needed.
- **Package-level annotations** — Allows entire packages to be tagged once rather than annotating every class.
- **`ignoreParent`** — Needed for cases like `BackgroundRunnable.run()` which overrides untagged `Runnable.run()` but must be tagged `Worker`.
- **`FX` vs `FXPlatform`** — JavaFX node construction can happen on non-FX threads before scene graph attachment. `FX` allows this; `FXPlatform` enforces strict FX-thread-only.
- **`NOTVMEventHandler`** — Prevents compile-time calls from VMEventHandler to methods that would deadlock waiting for debugger events.
