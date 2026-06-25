# GUI (Rendering & Input)

> Auto-generated from code analysis. Review and refine.

Low-level rendering and input handling for the simulation display (7 files). Renders the world to a BufferedImage and manages keyboard/mouse input with frame-based polling.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `WorldRenderer` | Renders World into BufferedImage: background, actors (with rotation), text labels, dragged objects with shadows |
| `KeyboardManager` | Keyboard state polling: tracks down/latched keys per frame; recognizes key names (arrows, F1-F12, etc.) |

---

## Input Sub-package

| Class | Purpose |
|-------|---------|
| `MousePollingManager` | Collects and prioritizes mouse events into frames |
| `MouseEventData` | Data holder for mouse event information |
| `PriorityManager` | Event priority: dragEnd > click > press > drag > move |
| `WorldLocator` | Maps pixel coordinates to world grid cell coordinates |

---

## Key Patterns

- **Frame-based polling**: Input state sampled per simulation frame, not per event
- **Priority queue**: Mouse events prioritized by importance within each frame
- **Coordinate transform**: `WorldLocator` converts screen pixels to world cells

---

## Dependencies

Uses: Actor/World from root greenfoot package

---

## Design Decisions

> TODO: Document rationale — cannot be inferred from code alone.
