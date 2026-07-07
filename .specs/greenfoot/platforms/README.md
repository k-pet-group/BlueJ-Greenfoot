---
id: greenfoot-platforms
type: submodule-design
title: "Platform Abstraction"
status: active
parent: greenfoot
---

# Platform Abstraction

Strategy pattern (9 files) supporting two execution contexts: IDE mode (running inside BlueJ) and standalone mode (exported JAR). Three delegate interfaces each have IDE and standalone implementations.

---

## Delegate Interfaces

| Interface | Purpose |
|-----------|---------|
| `ActorDelegate` | Default image resolution for actor classes |
| `GreenfootUtilDelegate` | Resource loading, sound enumeration, storage, UserInfo |
| `WorldHandlerDelegate` | World instantiation, painting, actor addition, error handling |

Each has an IDE and standalone implementation (e.g., `ActorDelegateIDE` / `ActorDelegateStandAlone`).

---

## Key Contracts

- **Dependency injection**: Delegates initialized at startup via `GreenfootUtil.initialise()`
- IDE mode gets full features (VMComms, project manager, class browser, export)
- Standalone mode gets minimal runtime (local resources, no IDE chrome)

---

## Dependencies

Uses: `util/` (GreenfootUtil facade), `core/` (WorldHandler)
