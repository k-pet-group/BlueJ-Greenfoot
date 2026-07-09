---
id: bluej-graph
type: submodule-design
title: "Graph (Class Diagram)"
status: active
parent: bluej
---

# Graph (Class Diagram)

Class diagram interaction layer (6 files). Manages selection, marquee selection, and keyboard navigation within the visual class dependency diagram.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `SelectionController` | Handles mouse/keyboard input for graph selection; manages marquee and rubber band |
| `SelectionSet` | Set of selected graph elements (Targets); notifies listeners via FXPlatformConsumer |
| `TraverseStrategy` (interface) | Strategy for pluggable keyboard navigation |

---

## Dependencies

Uses: `pkgmgr/target/` (Target elements in the diagram)
