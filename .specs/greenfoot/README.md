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
| `record/` | Screen recording (GreenfootRecorder) |

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

## Existing Documentation

- `greenfoot/doc/Greenfoot-architecture-and-design.txt`
- `greenfoot/doc/Inter-VM-design.txt`
- `greenfoot/doc/Greenfoot-API.pdf`
