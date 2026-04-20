# Scratch Importer

Imports Scratch projects into Greenfoot (16 files). Converts Scratch's stage/sprite model into Greenfoot's World/Actor model, importing images, sounds, and basic structure.

---

## Key Entry Points

- `ScratchImport` -- main entry point
- `ScratchStageMorph` -- Scratch stage -> Greenfoot World
- `ScratchSpriteMorph` -- Scratch sprite -> Greenfoot Actor
- `ScratchMedia` -- asset (image/sound) handling and conversion

Supporting classes (`ScratchPrimitive`, `ScratchObjectReference`, `ScratchPoint`, etc.) represent the Scratch object model during import.

---

## Dependencies

Self-contained; uses only standard library for file parsing.
