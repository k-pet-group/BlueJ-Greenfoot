---
id: greenfoot-guifx
type: submodule-design
title: "Greenfoot JavaFX UI"
status: active
parent: greenfoot
depends-on: [greenfoot-record]
---

# Greenfoot JavaFX UI

The JavaFX-based IDE interface for Greenfoot (41 files). Provides the main window, world display, control panel (Run/Pause/Step/Reset), class browser, and dialogs for export, images, and sound recording.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `GreenfootGuiHandler` | Implements BlueJ's GuiHandler interface; entry point for Greenfoot GUI |
| `GreenfootStage` | Main window/stage for the Greenfoot IDE |
| `ControlPanel` | Run, Step, Pause, Reset buttons and speed slider |
| `WorldDisplay` | Renders the simulated world from frames received via VMComms |
| `NewClassDialog` | Dialog for creating new Actor/World subclasses |

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `classes/` | Class browser and class-related UI components |
| `export/` | Export dialog and publishing UI |
| `images/` | Image library browser |
| `soundrecorder/` | Sound recording UI |

---

## Dependencies

Uses: `core/` (simulation state), `vmcomm/` (world frames), BlueJ UI framework

---

## Authoring Contracts

- New Scenario offers Java, Stride and Kotlin. Both ordinary-class and image/subclass dialogs offer
  those languages and select `Package.getDefaultSourceType()` initially; the language of a selected
  superclass is not a project-wide language policy. Java/Stride template selection is unchanged.
- Kotlin's ordinary, Actor, World and subworld templates create **open** classes, so the normal
  Greenfoot subclass action remains useful. All expose public no-arg construction. Direct World
  construction delegates to `World(600, 400, 1)`; a subworld delegates to its actual parent's no-arg
  constructor. Actor templates use `override fun act()`, which remains overridable in descendants.
  Ordinary templates omit example methods so generating another ordinary subclass does not introduce
  accidental final-method/override conflicts.
- Skeletons use the existing `ProjectUtils.createSkeleton` translation and locale fallback, with one
  additive Kotlin parent-clause placeholder: empty for no parent, otherwise `: Parent()`. The direct
  World template owns its dimension arguments. Existing Java/Stride placeholders and output are
  unchanged. Kotlin labels/templates currently fall back to English; translating every locale is not
  required for this local authoring milestone.
- Creation names are **not discovery filenames**. All languages retain the existing Java-compatible
  class-name restriction used by the JVM-facing tools and target discovery (so a new Kotlin class
  called `int` cannot disappear on reopen), and reject control characters before filesystem checks.
  Kotlin creation additionally requires a single unquoted
  Kotlin identifier, rejecting hard keywords, `$`, backticks, underscore-only names, source fragments
  and paths. The shared Kotlin parser helper implements the Kotlin lexical rule, allowing soft
  keywords; it does not filter discovery filenames. Both dialogs reject Windows device names.
- Normal creation rejects an existing package target or same-stem `.java`, `.stride`, `.kt` or `.class`
  path, including files not yet discovered by the diagram. Built-in Actor/World names are unavailable.
  The checks run in the dialogs and again immediately before creation; the destination is reserved
  without overwriting an existing file. This is ordinary name safety, not mixed-source enforcement.

## Authoring Lifecycle

- New Scenario creates `MyWorld` with the selected language's direct-World template. Ordinary class
  creation and ordinary subclass creation use the standard template; Actor/World subclass creation
  uses the image dialog and chooses Actor, World or subworld by the parent's existing diagram kind.
- Successful skeleton generation follows the existing add-target, compile and add-to-diagram path.
  A selected image is assigned only after creation succeeds. A failed skeleton write removes only
  the newly reserved destination; it does not register a target or assign an image/current World.
- Subsequent source metadata, grouping and editor dispatch remain at their existing shared boundaries;
  see the [class diagram contract](classes/README.md) and
  [Kotlin diagram integration](../../kotlin-for-bluej/diagram/README.md).

## Compilation Lifecycle

`GreenfootStage` consumes the sole full-request start/completion from `bluej-compiler`; internal K2/javac
stages must not separately discard or reset the World. The existing final behavior is retained: a closed
Project is ignored, focus controls reset, then messages/grouping are refreshed. No new success-only reset
guard is introduced. Target readiness and stale-edit rejection remain owned by `bluej-pkgmgr`.

## Save the World Lifecycle

- Tools and World context-menu saves share one FX-thread request path. Each request captures its
  Project and recorder identities. Source lookup uses the recorder's World class name to resolve the
  current ClassTarget in that Project, returning its editor and source type; it must not reuse the
  context menu's captured target, which may describe a different World. No current project means no
  request; missing targets, source or editors are ordinary source-edit failures, not language policy.
- Completion belongs to the captured Project/recorder pair. A callback is ignored if either identity
  has changed. Only `SAVED`, after the source edit and recording-snapshot acknowledgement, requests
  compilation. This acknowledges editing, not disk persistence or successful compilation; see the
  [recorder contract](../record/README.md) and
  [editor acknowledgement contract](../../bluej/editor/README.md#source-insertion-acknowledgement).
- `INVALID_RECORDING` preserves the existing Act/Run explanation. `EDIT_FAILED` uses a distinct
  source-error dialogue: the recording is retained, and users should inspect source before retrying
  because partial edits may remain. `CANCELLED` (superseded/overlapping saves) neither reports a false
  source error nor compiles. The recorder owns snapshot consumption and cancellation, not the menus.

## Scope and Verification Limits

Save the World follows the bounded `greenfoot-record` replay contract, including its deferred expression
and source-name compatibility cases; it is not general Kotlin replay support. Shared mixed-request
correctness follows `bluej-compiler`; full mixed-project source selection remains deferred. Structural
duplication/import, surrounding workflows and export/publishing remain separate integration areas.
No language-policy rejection or whole-scenario compilation fallback is added. The existing duplication
dialog still uses its explicit source language; adding Kotlin creation is not a claim that duplication
is Kotlin-aware.

M1 acceptance limits are unchanged. Its hierarchy fixtures and the newer
[recording/source/editor regressions](../README.md#kotlin-local-scenarios) do not exercise live dialogs.
Installed create/reopen and execution acceptance remains unconfirmed; automated M2 save checks do not
establish that acceptance or live Save/Reset/reopen verification.
