---
id: greenfoot
type: module-design
title: "Greenfoot Simulation IDE"
status: active
parent: bluej-greenfoot-architecture
depends-on:
  - bluej
---

# Greenfoot Simulation IDE

Greenfoot is a 2D game/simulation IDE built on top of BlueJ. Students create scenarios by extending `World` and `Actor` classes. Provides collision detection, audio, image manipulation, and a visual execution environment with Run/Pause/Step controls.

---

## Public API

| Class | Purpose |
|-------|---------|
| `Actor` | Base class for all game objects; act(), move(), turn(), collision queries |
| `World` | Game scene; manages actors, background, cell grid |
| `Greenfoot` | Static utility: getKey(), isKeyDown(), getMouseInfo(), setWorld(), playSound(), ask() |
| `GreenfootImage` | Image loading, drawing primitives, transformations |
| `GreenfootSound` | Audio playback (WAV, AIFF, MIDI, MP3) |
| `Color` / `Font` / `MouseInfo` / `UserInfo` | Named colors, font spec, mouse state, online storage |

---

## Key Subsystems

| Package | Purpose |
|---------|---------|
| `guifx/` | JavaFX IDE interface: GreenfootStage, ControlPanel, WorldDisplay, dialogs |
| `sound/` | Audio: SoundFactory, SoundClip (WAV/AIFF), MidiFileSound, SoundStream (MP3) |
| `core/` | Simulation engine: GreenfootMain, Simulation (act cycle thread), WorldHandler |
| `importer/` | Scratch project import |
| `collision/` | IBSP spatial partitioning for collision detection |
| `platforms/` | Strategy pattern: IDE mode vs. standalone exported mode |
| `export/` | Scenario export: JAR creation, web page generation |
| `gui/` | Low-level rendering: WorldRenderer, keyboard/mouse input managers |
| `event/` | Observer pattern: SimulationListener, WorldListener, PublishListener |
| `vmcomm/` | Inter-VM communication via memory-mapped shared memory |
| `record/` | Interactive setup recording and source replay (GreenfootRecorder) |

---

## Architecture

Greenfoot uses a **dual-VM architecture** inherited from BlueJ:

- **Main VM**: Runs the IDE UI (JavaFX), project management, world display
- **Debug VM**: Runs user scenario code -- World, Actors, act() cycle via `Simulation` thread
- **VMComms**: Memory-mapped file IPC carrying events (main->debug) and rendered frames (debug->main)

Key singletons in the debug VM: `GreenfootMain`, `Simulation`, `WorldHandler`

### Three-Lock Protocol (Deadlock Prevention)

VMComms uses regions A (server area), B (user area), C (sync area) with a strict acquisition order that prevents either VM from holding all three locks simultaneously.

### Shared Memory Layout (Debug VM -> Main VM)

| Offset | Content |
|--------|---------|
| 0 | Sequence index when image was painted |
| 1-2 | Width, Height in pixels |
| 3..3+WxH | Pixel data (BGRA) |
| after pixels | Last processed command seq ID, error count, exec start time, speed, world counter, cell size, ask request, delay/ready flags |

---

## Dependencies

**External:** JavaFX 23.0.2, OpenCSV 2.4, Apache HTTP Client 4.5.13, JLayer 1.0.1 (MP3), Simple-PNG 0.2.0

**Internal:** `bluej` (core IDE), `anns-threadchecker`, `boot`

---

## Kotlin Local Scenarios

Local Kotlin-source scenarios reuse the shared BlueJ Kotlin compiler, text editor and JVM execution
pipeline. The authoring UI supplies Kotlin ordinary/Actor/World/subworld templates; hierarchy metadata
uses the shared qualified-name contract before and after compilation. Students call the compiled Java
Greenfoot API and compiled libraries without installing Kotlin. The compiler and metadata tooling stay
on the IDE classpath, while students receive stdlib; see [Kotlin distribution](../kotlin-for-bluej/distribution/README.md).

Local authoring/runtime acceptance remains scoped to Kotlin-only student sources, not fully reliable
mixed Java/Kotlin/Stride projects. Both IDEs share the mixed-request coordination contract in
`bluej-compiler`; this does not repair omitted inputs or deferred dependency discovery. Source-selection
correctness waits for Kotlin Analysis API integration, without a whole-scenario compilation workaround.
Existing Java/Stride scenarios and generated Java invocation shells retain their routes; no new
language-policy checks or Stride/Kotlin guarantee are added. Kotlin Save
the World uses structured recording and PSI-guided source insertion under the bounded `greenfoot-record`
contract; UI completion and compilation are owned by `greenfoot-guifx`. Kotlin class duplication uses the
bounded copy contract in `greenfoot-guifx`; runtime links use `bluej-terminal`, and native Insert Method
uses `kotlin-for-bluej-editor`. These are not general refactoring, semantic navigation or completion.

Import facilities and bundled Java source helpers are unchanged. Importing such a helper introduces
mixed student sources, not a compiled-library dependency. Selected-source queue/compiler probes do not
establish a reliable import-to-execution workflow while source selection remains incomplete. No new
source chooser, Kotlin catalog entries or helper ports are supplied. Source-project sharing and client
publishing artifacts follow `greenfoot-export`: preserve source archives, exclude Java/Kotlin/Stride
sources from runtime artifacts and embed student stdlib only for Kotlin-source publishing. Hosted Kotlin
conversion/playback remains unverified. Existing controls are not blocked by new language-policy checks.

Focused regression coverage protects hierarchy/source-cache/diagram boundaries and setup recording,
source generation, document undo and save acknowledgement. Additional focused checks cover class-copy
text, terminal frame matching and native method insertion/undo. They do not establish live source-link
activation or duplication dialogs. Shared real-queue regressions additionally cover request isolation,
combined failure/completion, BlueJ temporary output and Greenfoot retained check-only output. These are
not restored M1 authoring/installed-loader fixtures: discovery, class authoring/templates, creation-name
handling and installed-loader behavior still have no dedicated Greenfoot Kotlin fixtures.
Export fixtures additionally cover archive preservation, extracted-source recompilation and isolated
stdlib-dependent execution from a produced publishing JAR. They do not open a live Project/export UI
or exercise a Greenfoot World/Actor lifecycle. Compiler/direct-engine, archive and editor tests do not
establish live installed GUI/runtime or gallery acceptance.

## Existing Documentation

- `greenfoot/doc/Greenfoot-architecture-and-design.txt`
- `greenfoot/doc/Inter-VM-design.txt`
- `greenfoot/doc/Greenfoot-API.pdf`
