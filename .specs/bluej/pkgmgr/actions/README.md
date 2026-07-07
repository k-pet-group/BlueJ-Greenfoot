---
id: bluej-pkgmgr-actions
type: submodule-design
title: "Package Manager Actions"
status: active
parent: bluej-pkgmgr
---

# Package Manager Actions

Menu and toolbar actions for the main IDE window (38 files). Uses the Command pattern with a `PkgMgrAction` base class. Actions are configuration-driven via `Config.getString()` for labels and shortcuts.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `PkgMgrAction` (abstract) | Base action with label, shortcut, thread annotation |
| `NewClassAction`, `NewPackageAction` | Creation |
| `OpenProjectAction`, `SaveProjectAction` | Project lifecycle |
| `CompileAction`, `RunTestsAction` | Build and test |
| `ExportProjectAction`, `PrintAction` | Output |
| 20+ more specialized actions | Help, preferences, library management, etc. |
