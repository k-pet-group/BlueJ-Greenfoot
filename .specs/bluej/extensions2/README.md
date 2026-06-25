# Extensions API

The official third-party plugin API (47 files, v3.4). Extensions are loaded from `~/.bluej/extensions2` and can add menu items, respond to IDE events, and interact with projects, packages, and classes through bridge wrapper objects.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Extension` (abstract) | Base class for all plugins; subclass and implement `isCompatible()`, `startup()` |
| `BProject`, `BPackage`, `BClass` | Extension-visible wrappers for project model objects |
| `ExtensionBridge` | Internal bridge connecting extension API to BlueJ internals |

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `event/` | Extension event system: compile events, class events, package events |
| `editor/` | Editor access from extensions |

---

## Dependencies

Uses: `extmgr/` (lifecycle management), `pkgmgr/` (project model)
