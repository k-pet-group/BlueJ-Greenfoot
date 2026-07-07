---
id: bluej
type: module-design
title: "BlueJ Core IDE"
status: active
parent: bluej-greenfoot-architecture
depends-on:
  - lang-stride
---

# BlueJ Core IDE

BlueJ is an educational Java IDE providing compilation, debugging, code editing (text and block-based Stride), project management, version control, and an extension system. It is the foundation upon which Greenfoot is built.

---

## Key Packages

| Package | Purpose |
|---------|---------|
| `pkgmgr/` | Central orchestrator: project/package management, class diagram, class targets |
| `stride/` | Stride block-based visual programming: frames, slots, Java<->Stride conversion |
| `parser/` | Incremental Java parser with symbol tables, scope tracking, code completion |
| `editor/` | Text editor (custom virtualized renderer) and Stride frame editor with scope coloring and quick fixes |
| `debugger/` | Abstract debugger interface with JDI implementation; state machine: IDLE<->RUNNING<->SUSPENDED |
| `debugmgr/` | Debug UI: object bench, inspectors, code pad, execution controls |
| `groupwork/` | Git-based teamwork via Eclipse JGit |
| `extensions2/` | Third-party plugin API v3.4 |
| `compiler/` | Single-threaded compilation queue using javax.tools |
| `runtime/` | ExecServer: two-threaded execution model in debug VM |
| `utility/` | Shared utilities: JavaFX helpers, file I/O, dialogs, debug logging |
| `collect/` | Blackbox anonymous, opt-out usage statistics collection |
| `extmgr/` | Extension lifecycle: loading, wrapping, custom class loaders |
| `views/` | Reflection-based class/method/field view wrappers |
| `prefmgr/` | Persistent user preferences |
| `graph/` | Class dependency diagram visualization |
| `terminal/` | Interactive console connected to debug VM |
| `testmgr/` | Unit test recording and replay |
| `classmgr/` | Dynamic class loading for user classes |

---

## Root Package Entry Points

- `Main` -- Application entry point after Boot; initializes Config, selects GuiHandler (BlueJ vs Greenfoot), opens projects
- `Config` -- Centralized static singleton for configuration: layered properties (bluej.defs -> greenfoot.defs -> user props -> CLI), i18n labels, OS detection
- `BlueJEvent` -- Static event bus with int event IDs and `BlueJEventListener` dispatch
- `GuiHandler` -- Interface abstracting GUI startup for both BlueJ and Greenfoot

---

## Two-VM Architecture

User code runs in a **separate JVM** (the "debug VM") connected via JDI. This isolates the IDE from user code crashes, infinite loops, and `System.exit()` calls. The trade-off is complexity in the startup protocol and method invocation via JDI reflection.

---

## Two-Stage Boot

`Boot.main()` in the `boot/` module dynamically builds the classpath and loads `bluej.Main` reflectively via `URLClassLoader`. This allows runtime classpath configuration for user libraries and the debug VM.

---

## BlueJ/Greenfoot Shared Codebase

A single `Config.isGreenfoot` flag (set once at startup) drives mode-dependent behavior. The `GuiHandler` interface abstracts the GUI layer so both applications share the same core.

---

## Configuration System (Config)

Property hierarchy (each layer overrides the previous):
1. `systemProps` -- `lib/bluej.defs`
2. `greenfootProps` -- `lib/greenfoot.defs` (Greenfoot mode only)
3. `userProps` -- `~/.bluej/bluej.properties`
4. `commandProps` -- CLI `-D` properties

---

## Usage Statistics (two separate systems)

1. **Launch ping** (`Main.updateStats()`): once per launch, a background thread sends one anonymous
   HTTP GET to `stats.bluej.org` (or `stats.greenfoot.org`) with OS, app/Java version, UI language,
   and per-language editor-open counts. Counts are accumulated across sessions by
   `Config.recordEditorOpen` (called from `ClassTarget` on first editor show) in `userProps` keys
   `session.numeditors.{java,stride,kotlin}`, then sent and reset on the next launch. Editor counts
   are only included when all three properties exist (`getEditorCount != -1` for each). Opt-out:
   set the `bluej.uid`/`greenfoot.uid` property to `private`.
2. **Blackbox** (`collect/`): opt-in fine-grained research data collection — unrelated to the
   launch ping.

---

## Threading Model

Compile-time-enforced thread safety via `@OnThread` annotations, checked by the `threadchecker` annotation processor.

| Tag | Meaning |
|-----|---------|
| `FXPlatform` | JavaFX platform thread only |
| `FX` | FX platform thread OR FX loader thread |
| `Swing` | Swing EDT (legacy) |
| `Worker` | Background worker threads |
| `VMEventHandler` | JDI event handler thread |
| `Any` | Safe from any thread |

Violations are compile-time errors.

---

## Key Interfaces and Patterns

- **Observer pattern everywhere**: `EditorWatcher`, `CompileObserver`, `DebuggerListener`, `PackageListener`, `BlueJEventListener`
- **`Project` -> `Package` -> `ClassTarget`** -- Core project model hierarchy
- **`Editor`** (interface) -> `TextEditor` -> `FlowEditor`; `FrameEditor` implements `Editor` directly
- **`Debugger`** (abstract) -> `JdiDebugger`
- **`Repository`** (interface) -> Git implementation
- **`JobQueue`** (singleton) -- Serialized compilation

---

## Dependencies

- **External:** Guava 33.3.1, Eclipse JGit 7.1.0, XOM 1.3.9, Classgraph 4.8.90, Apache HTTP Client 4.5.13, JavaFX 23.0.2, NSMenuFX 3.1.0
- **Internal:** `anns-threadchecker`, `threadchecker`, `lang-stride`
- **Test:** JUnit 5.5.2, TestFX 4.0.15-alpha

---

## Known Limitations

- **Single-project windows**: Each `PkgMgrFrame` shows one project
- **No incremental compilation**: Entire source files recompiled; `JobQueue` serializes on a single thread
- **JDI overhead**: Method invocation through JDI reflection is slow
- **English-dependent quick fixes**: Diagnostic-to-fix matching uses English diagnostic text, not javac error codes
- **Legacy Swing remnants**: Some code paths still reference Swing EDT

---

## Existing Documentation

- `bluej/doc/BlueJ-architecture-and-design.txt`
- `bluej/doc/BlueJ-parser.txt`
- `bluej/doc/threading-issues.txt`
