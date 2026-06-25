# Thread-Safety Checker

A javac compiler plugin that statically verifies `@OnThread` thread-safety contracts at compile time. Hooks into the ANALYZE phase to walk the AST and check that all method calls, lambda expressions, field accesses, and overrides respect thread-tag compatibility.

**3 Java files** (2 main + 1 test). Core scanner (`TCScanner`) is ~1,750 lines.

Dependencies: Java Compiler API (`com.sun.source.util`), `anns-threadchecker` (compile-only). Test: JUnit 5.10.2, JavaFX.

---

## Key Classes

- **`TCPlugin`** — javac `Plugin` implementation; registers a `TaskListener` on ANALYZE events
- **`TCScanner`** — `TreePathScanner` that visits all invocations, lambdas, constructors, and field accesses to check thread-tag compatibility

---

## Tag Resolution

Tags are resolved with a first-found-wins priority. Both "current context" (what thread am I on?) and "invocation target" (what thread does the callee require?) follow the same general order:

1. Lambda scope (nearest enclosing lambda's resolved tag)
2. Method annotation (`@OnThread` on method)
3. Built-in method override (`methodAnns` map for known library methods)
4. Class annotation
5. Package annotation (source `package-info.java`)
6. Built-in package override (`packageAnns` map)
7. Inherited from overridden parent methods
8. (Targets only) Inherited from superclass with `applyToAllSubclassMethods`

---

## Lambda Thread Tag Inference

Lambdas inherit thread context from their functional interface:

- **Special method recognition:** `Platform.runLater` -> `FXPlatform`, `SwingUtilities.invokeLater` -> `Swing`, `background.execute` -> `Worker`
- Otherwise: SAM method annotation -> interface class annotation -> package annotation

The `callingMethodStack` tracks parameter types of enclosing method calls so lambdas can be matched to their functional interface parameter.

---

## Override Validation

`checkAgainstOverridden()` walks all supertypes and validates child tag can override parent tag via `Tag.canOverride()`. Detects conflicting tags from multiple interfaces.

**Exemptions:** anonymous inner classes, `java.lang.Object` methods, `Comparable`/`Comparator` methods, `java.awt` parents, constructors, Thread subclass constructors.

---

## Field Access Rules

- Explicit `@OnThread` on field -> uses that tag
- `volatile` fields -> `Any`
- `final` primitive/String/File/AtomicInteger fields -> `Any`
- Otherwise -> inherits enclosing class tag
- `requireSynchronized = true` -> caller must be in `synchronized(this)`

---

## Built-in Library Knowledge

The scanner hardcodes threading contracts for standard library and JavaFX classes that lack `@OnThread` annotations:

- **Packages:** Most `javafx.scene.*` / `javafx.beans.*` -> `FX`; `javafx.scene.web` -> `FXPlatform`; `javax.swing.*` -> `Swing`
- **Classes:** `EventHandler` -> `FXPlatform`, `Thread` -> `Worker`, `Platform`/`SwingUtilities`/`EventQueue` -> `Any`
- **Methods:** `Application.start` -> `FXPlatform`, `AnimationTimer.handle` -> `FXPlatform`, Thread lifecycle -> `Any`

---

## Thread-Switching Detection

`fromSpecial()` detects dispatch calls and infers callback tags:
- `Platform.runLater(r)` -> `FXPlatform` (warns if already on FX thread)
- `SwingUtilities.invokeLater(r)` -> `Swing` (warns if already on Swing)
- `SwingUtilities.invokeAndWait(r)` -> `Swing` (**errors** if on FX/FXPlatform: deadlock risk)
- `background.execute(r)` -> `Worker`

---

## Suppression

- `@SuppressWarnings("threadchecker")` on a method skips its body entirely
- Synthetic methods (containing `$`) are silently allowed
- Packages in the `ignorePackages` init argument are skipped

---

## Design Decisions

- **ANALYZE phase** — Runs after type resolution so full type info and annotation mirrors are available.
- **Single shared scanner** — Reused across compilation units; caches resolved annotations for performance.
- **Hardcoded library knowledge** — JavaFX/Swing don't use `@OnThread`, so their threading contracts must be encoded in the plugin.
- **`FX` cannot call `FXPlatform`** — `FX` code may run on a loading thread (not the actual FX app thread), so calling `FXPlatform` methods would be unsafe.
