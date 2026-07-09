---
id: bluej-greenfoot-architecture
type: architecture-design
title: "BlueJ-Greenfoot Architecture Design"
status: active
parent: goal-and-requirements
---

# BlueJ-Greenfoot Architecture Design

BlueJ and Greenfoot are educational Java IDEs sharing a monorepo. BlueJ is a general-purpose pedagogical IDE; Greenfoot extends it for 2D game/simulation programming. Built with Gradle, targeting Java 21 and JavaFX 23.0.2.

---

## Module Boundaries

| Module | Role |
|--------|------|
| `boot/` | Entry point, classpath bootstrapping, splash screen |
| `bluej/` | Core IDE: compiler, debugger, editor, parser, package manager, Stride |
| `greenfoot/` | Simulation IDE: world/actor model, collision, sound, export |
| `lang-stride/` | Public API for Stride language (Terminal, Utility) |
| `threadchecker/` | javac compiler plugin for thread-safety static analysis |
| `anns-threadchecker/` | Thread-safety annotation definitions (`@OnThread`, `Tag`) |

**Key dependency rules:**
- `boot/` loads `bluej/` via reflection (custom URLClassLoader); compile-only dependency on `threadchecker` and `anns-threadchecker`
- `greenfoot/` extends `bluej/`
- `bluej/` uses `lang-stride/` and `anns-threadchecker`
- `threadchecker/` uses `anns-threadchecker`

---

## Key Architectural Decisions

- **Dual-VM architecture** — The IDE runs in a Main VM (UI) while user code runs in a separate Debug VM. They communicate via memory-mapped shared memory (VMComms, 3-lock FileLock protocol). This isolates user code crashes from the IDE.
- **JDI-based debugging** — Full Java debugging without custom protocols; uses static field writes via JDI + breakpoint resume for cross-VM control.
- **Compile-time thread safety** — A custom `threadchecker` javac plugin with `@OnThread` annotations enforces thread contracts statically across the entire codebase. See the threadchecker and anns-threadchecker specs for details.
- **Single JobQueue for compilation** — Serialized, thread-safe compilation.
- **Stride language** — Bridges block-based and text-based programming for educational scaffolding.
- **Extensions API v3.4** — Third-party plugin support.

---

## Threading Architecture (Cross-Module)

Thread safety is the primary cross-cutting concern. The `@OnThread(Tag.X)` annotation system declares thread affinity on methods, constructors, classes, and packages.

**Key thread tags:** `FXPlatform` (JavaFX app thread), `FX` (superset: FX thread or FX loading thread), `Worker`, `VMEventHandler`, `Simulation` (Greenfoot debug VM), `Any`, `Swing` (legacy).

**Inter-thread communication patterns:**
- FX dispatch: `Platform.runLater()`, `JavaFXUtil.runNowOrLater()`
- Blocking FX call: `JavaFXUtil.runPlatformAndWait()` via `CompletableFuture.get()`
- Background dispatch: `Utility.runBackground()` (8-thread pool)
- Cross-VM: JDI field rendezvous, shared memory IPC (VMComms)
- Simulation posting: `Simulation.runLater()`

**Deadlock avoidance invariants:**
- Simulation monitor released before calling user `act()` methods
- `JdiDebugger` monitor released before `VMReference` operations
- `SoundClip` lock released before JDK audio calls (synchronous callbacks)
- VMComms 3-lock protocol: each process holds at most 2 locks; consistent acquisition order
- `getVMNoWait()` used when caller already holds a lock

---

## Build

- Gradle 8.5, Java 21 with preview features
- GitHub Actions: build + test on push/PR; cross-platform installers (Windows WiX, Linux deb, macOS Intel + ARM)

---

## Sub-Module Specs

| Module | Spec |
|--------|------|
| bluej/ | [BlueJ Core IDE](bluej/README.md) |
| greenfoot/ | [Greenfoot Simulation IDE](greenfoot/README.md) |
| boot/ | [Boot Module](boot/README.md) |
| lang-stride/ | [Stride Language API](lang-stride/README.md) |
| threadchecker/ | [Thread-Safety Checker](threadchecker/README.md) |
| anns-threadchecker/ | [Thread-Checker Annotations](anns-threadchecker/README.md) |
