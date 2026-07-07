---
id: greenfoot-guifx
type: submodule-design
title: "Greenfoot JavaFX UI"
status: active
parent: greenfoot
---

# Greenfoot JavaFX UI

> Auto-generated from code analysis. Review and refine.

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

## Design Decisions

> TODO: Document rationale — cannot be inferred from code alone.
