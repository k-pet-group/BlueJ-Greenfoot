# Class Diagram Integration

> How Kotlin classes appear and behave on BlueJ's visual class diagram.

**Parent spec:** [Kotlin for BlueJ -- Architecture Design](../DESIGN_DOC.md)

---

## Architecture

Hub-and-spoke pattern with `ClassTarget` as the central hub. Each spoke is a language-agnostic abstraction routed by `sourceAvailable` (a `SourceType`).

| Spoke | Routing |
|-------|---------|
| File identity | `SourceType.Kotlin` with `"kt"` extension |
| Class creation | `NewClassDialog` + Kotlin templates (`.kt.tmpl` files) |
| Source metadata | `SourceInfo` dispatches to `KotlinInfoParser` for `.kt` files |
| Diagram appearance | `ClassRole` hierarchy (unchanged -- language-agnostic) |
| Editor dispatch | `FlowEditor` + `KotlinLanguageSupport` strategy |
| Compilation | `JobQueue` -> `KotlinCompiler` (covered by compiler module) |

All spokes work identically for Kotlin as for Java/Stride. Modifications are in routing logic, not abstractions.

---

## Key Contracts

- **Source detection priority**: `.stride` -> `.java` -> `.kt` -> `NONE`
- **ClassRole hierarchy is unchanged** -- Kotlin `class`/`abstract class`/`interface`/`enum class` map to existing `StdClassRole`/`AbstractClassRole`/`InterfaceClassRole`/`EnumClassRole`
- **Dependency system is unchanged** -- `KotlinInfoParser` produces standard `ClassInfo` consumed by `analyseDependencies()`
- **Template substitution** -- Kotlin templates use same `$CLASSNAME`/`$PKGLINE` variables as Java
- **`canConvertToStride()` returns false** for Kotlin
- **`enforcePackage()` skips Kotlin** (Java-only; Kotlin-aware enforcement is post-MVP)

### Dependency Editing (UI -> Source)

When a user draws an inheritance arrow, `FlowLanguageSupport` strategy handles the source modification:
- Java: `" extends " + name`, `" implements " + name`
- Kotlin: `" : " + name + "()"`, `", " + name` (unified supertype list)

`KotlinInfoParser` populates `Selection` positions via `PsiElement.getTextRange()` for accurate source editing.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | `.kt` detection after `.stride` and `.java` | Backward compatibility -- existing projects unchanged |
| 2 | Reuse ClassRole hierarchy as-is | Kotlin modifiers map cleanly to existing roles via ClassInfo |
| 3 | Same template substitution system | `BlueJFileReader.translateFile()` is language-agnostic |
| 4 | FlowEditor reuse (not a new KotlinEditor) | FlowEditor is a generic text editor; language specifics come from strategy |
| 5 | `KotlinInfoParser` produces standard `ClassInfo` | Zero changes needed in diagram rendering pipeline |
| 6 | One source language per ClassTarget | Consistent with Stride model; mixed-language is out of MVP scope |

---

## Known Limitations (MVP)

- **No data class role** -- `data class` shows as `StdClassRole` (no `<<data>>` stereotype)
- **No Kotlin object/companion** diagram representation
- **No Stride <-> Kotlin conversion**
- **Template subset** -- only 5 Kotlin templates vs 6+ Java templates
- **`enforcePackage()` is Java-only** -- Kotlin package declarations not auto-corrected on move

---

## Dependencies

Cross-module: `KotlinInfoParser` (parser module), `KotlinLanguageSupport` (editor module), `KotlinCompiler` (compiler module)
