# Event System

> Auto-generated from code analysis. Review and refine.

Observer/listener event system (7 files) for world lifecycle, simulation state, and publish progress. All listeners extend `java.util.EventListener` with corresponding event objects.

---

## Listeners and Events

| Listener | Event | Purpose |
|----------|-------|---------|
| `WorldListener` | `WorldEvent` | World creation and removal |
| `SimulationListener` | — | Simulation state changes (started, stopped, speed change, act rounds) |
| `PublishListener` | `PublishEvent` | Publishing progress (upload, errors, auth) |
| `ValidityListener` | `ValidityEvent` | Validity state changes |

---

## SimulationListener Threading

`SimulationListener` distinguishes between sync and async events:
- **SyncEvent** (on simulation thread): STARTED, STOPPED
- **AsyncEvent** (off simulation thread): CHANGED_SPEED, NEW_ACT_ROUND, END_ACT_ROUND

---

## Dependencies

Self-contained; uses only standard `java.util.EventListener` / `java.util.EventObject`.

---

## Design Decisions

> TODO: Document rationale — cannot be inferred from code alone.
