---
id: boot
type: module-design
title: "Boot Module"
status: active
parent: bluej-greenfoot-architecture
---

# Boot Module

Entry point for both BlueJ and Greenfoot. Dynamically constructs the classpath from `lib/*.jar`, shows a splash screen, and loads `bluej.Main` via reflection using a custom URLClassLoader.

**3 Java files.** Dependencies: JavaFX (runtime), `anns-threadchecker` and `threadchecker` (compile-only).

---

## Key Classes

- **`Boot`** — `main()` entry point; contains inner `App` class (JavaFX Application); classpath assembly; version constants (`BLUEJ_VERSION`, `GREENFOOT_VERSION`, `GREENFOOT_API_VERSION`)
- **`SplashWindow`** — Startup splash screen; progress bar appears after a 5-second delay
- **`DrawVersionOnSplash`** — Build utility (separate `src/splash/java/` source set) that renders version numbers onto splash images

---

## Design Decisions

- Boot uses a custom `URLClassLoader` and reflection to load `bluej.Main`, decoupling the entry point from the full IDE classpath.
- The splash screen progress bar is intentionally delayed 5 seconds to avoid flicker on fast startups.
