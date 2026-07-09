---
id: greenfoot-core
type: submodule-design
title: "Greenfoot Simulation Core"
status: active
parent: greenfoot
---

# Greenfoot Simulation Core

Simulation engine and initialization layer (18 files). Runs in the debug VM and drives the act() cycle. Manages world state, project lifecycle, and image caching.

---

## Key Entry Points

| Class | Purpose |
|-------|---------|
| `GreenfootMain` (singleton) | Entry point in debug VM; initializes subsystems |
| `Simulation` (singleton) | Main simulation thread; controls act() cycles, timing, speed |
| `WorldHandler` (singleton) | Bridge between World objects and VM communication layer |
| `ProjectManager` | Project lifecycle: open, close, reset |

---

## Simulation State Machine

- **Disabled** -> Paused (world set) -> Running (COMMAND_RUN) -> Paused (COMMAND_PAUSE)
- Paused -> SingleStep (runOnce) -> Paused (act cycle completes)
- Running -> Disabled (world discarded)

Act cycle: `world.act()` -> each `actor.act()` -> repaintIfNeeded -> write pixels to shared memory -> delay based on speed slider

---

## Threading Model

The `Simulation` class is the most synchronized class in the codebase (40 `synchronized` blocks).

### Two-Lock Architecture

- **Lock 1: `synchronized(this)`** -- protects control state: `paused`, `runOnce`, `speed/delay`, `queuedTasks`, `isRunning`. Also serves as the condition variable for `wait()`/`notifyAll()`.
- **Lock 2: `synchronized(interruptLock)`** -- protects the delay-interrupt protocol: `delaying`, `interruptDelay`.

### Critical Invariant

**The simulation monitor is never held during user code execution.** The simulation thread releases the monitor before calling `world.act()`, `actor.act()`, `world.started()`, or `world.stopped()`. This prevents user code from deadlocking the control mechanism.

### Volatile Fields

- `volatile boolean enabled` -- whether a world is installed; checked at top of tight loops
- `volatile boolean abort` -- terminal shutdown flag

### Cross-Thread Communication

| Direction | Mechanism |
|-----------|-----------|
| FXPlatform -> Simulation | `setPaused()`, `runOnce()`, `setSpeed()` (synchronized + notifyAll) |
| FXPlatform -> Simulation (tasks) | `runLater(SimulationRunnable)` -- queued tasks drained in `maybePause()` |
| Simulation -> VMComms Worker | `paintRemote()` via `AtomicReference` image handoff |
| VMComms Worker -> Simulation | Commands dispatched from `readCommands()` |

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| `MIN_PRIORITY` thread | User code should not starve IDE UI threads |
| Two-lock design | `interruptLock` separate from simulation monitor to avoid deadlock in `setPaused`/`interruptDelay` |
| `volatile` for `enabled` and `abort` | Checked in tight loops; immediate visibility without monitor overhead |
| `requireSynchronized = true` on `paused` | Enforced at compile time by threadchecker |
| `run()` delegates to `runContent()` | Debugger breakpoint on `run()` first line would otherwise fire every iteration |
| Tasks drained in `maybePause()` | Ensures `runLater()` tasks execute at safe points (between act cycles) |

---

## Dependencies

Uses: `event/` (listeners), `collision/` (detection), `vmcomm/` (IPC), `sound/` (audio)
