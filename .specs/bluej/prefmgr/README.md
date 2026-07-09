---
id: bluej-prefmgr
type: submodule-design
title: "Preferences Manager"
status: active
parent: bluej
---

# Preferences Manager

Persistent user preferences system (7 files). Stores editor settings, compiler options, and UI preferences using JavaFX observable properties for reactive UI binding. Preferences persist to `~/.bluej/bluej.properties`.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `PrefMgr` (singleton) | Central store of all preferences as JavaFX properties (BooleanProperty, IntegerProperty, StringProperty) |
| `PrefMgrDialog` | JavaFX tabbed preferences dialog |

---

## Key Patterns

- **Observable properties**: JavaFX property bindings for reactive UI updates
- **Panel-based UI**: Tabbed dialog with extensible preference panels

---

## Dependencies

Uses: `utility/` (JavaFX helpers)
