# Extension Manager

Extension lifecycle management (12 files). Discovers, loads, wraps, and manages third-party extensions from `~/.bluej/extensions2/`.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `ExtensionsManager` (singleton) | Discovers and loads extension JARs; delegates IDE events to loaded extensions |
| `ExtensionWrapper` | Wraps an extension JAR; handles loading, version checking, and safe try-catch delegation |
| `ExtensionPrefManager` | Manages extension preference panels |
| `ExtensionsMenuManager` | Builds context menus contributed by extensions |

---

## Security

`ExtensionWrapper` contains an inner `FirewallLoader` (custom ClassLoader) that restricts extensions to only `bluej.*`, `rmiextension.*`, and `greenfoot.*` classes, preventing access to IDE internals.

---

## Menu Implementations

Four `ExtensionMenu` implementations provide context menus for: class targets, object bench objects, packages, and the tools menu bar.

---

## Dependencies

Uses: `extensions2/` (Extension API), `pkgmgr/` (project context)
