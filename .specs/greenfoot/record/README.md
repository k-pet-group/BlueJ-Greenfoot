---
id: greenfoot-record
type: submodule-design
title: "Greenfoot Recorder"
status: active
parent: greenfoot
---

# Greenfoot Recorder

"Save the World" recording system (1 file). Captures interactive user actions and generates a `prepare()` method as Stride code that can reproduce the world state.

---

## Key Class

`GreenfootRecorder` -- records actor creation, placement, method calls, movements, removals. Generates a `prepare()` NormalMethodElement for insertion into the World subclass.

---

## Contracts

- Objects named using deterministic numbering: `crab`, `crab2`, `crab3`, etc. Tracked via `HashMap<DebuggerObject, String>`.
- `validToSave` is **invalidated** by Act/Run (unpredictable state changes) and **set** by world creation/reset (clean starting state).

---

## Dependencies

Uses: `core/` (GreenfootMain, WorldHandler), Stride code elements (`bluej.stride.framedjava`)
