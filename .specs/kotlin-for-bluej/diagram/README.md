---
id: kotlin-for-bluej-diagram
type: module-design
title: "Class Diagram Integration"
status: active
parent: kotlin-for-bluej
depends-on:
  - bluej-pkgmgr-target
  - kotlin-for-bluej-compiler
tags:
  - kotlin
---

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

- **Source detection priority**: `.stride` -> `.java` -> `.kt` -> `NONE`. Both IDEs discover `.kt` sources and deduplicate their generated facade outputs. Discovery retains its existing filename rules; new-class identifier validation must not hide existing function files such as `when.kt`.
- **ClassRole hierarchy is unchanged** -- Kotlin `class`/`abstract class`/`interface`/`enum class` map to existing `StdClassRole`/`AbstractClassRole`/`InterfaceClassRole`/`EnumClassRole`
- **Dependency system is unchanged** -- `KotlinInfoParser` produces standard `ClassInfo` consumed by `analyseDependencies()`
- **Template substitution** -- Kotlin templates use same `$CLASSNAME`/`$PKGLINE` variables as Java
- **`canConvertToStride()` returns false** for Kotlin
- **`enforcePackage()` rewrites Kotlin package directives** alongside Java. The branch that inserts a missing directive omits the trailing semicolon for Kotlin (Java emits `";\n\n"`, Kotlin emits `"\n\n"`); rename and delete branches reuse the PSI-derived `Selection`s from `KotlinInfoParser`
- **Package move (`Package.importFile`)** accepts `.kt` alongside `.java` and `.stride` via the shared `stripRecognisedSourceExtension` helper. After the source file is copied to the destination, the standard `addClass` → `analyseSource` flow picks up the right `SourceType.Kotlin`, applies `enforcePackage`, and the next compile in the destination emits the proper `.class` (regular classes) or facade `.class` (top-level functions).
- **Non-BlueJ project import (`Import.convertNonBlueJ`)** treats `.kt` files as first-class source files: `findInterestingDirectories` marks a directory interesting if it contains any recognised source file (Java, Stride, or Kotlin); `findSourceFiles` (formerly `findJavaFiles`) returns all such files. The mismatch loop dispatches to `KotlinInfoParser.parse` for `.kt` files and `InfoParser.parse` for `.java` files.
- **Add Class from File…** (`PkgMgrFrame.doAddFromFile`) uses `FileUtility.getSourceFilterFX` (formerly `getJavaStrideSourceFilterFX`) which lists `*.java`, `*.stride`, and `*.kt` in the file picker. The selected files are routed through `Package.importFile`, which is also Kotlin-aware.
- **Right-click call menus show parameter names** -- the "call constructor" / "call method" menu items display real parameter names for Kotlin classes and bench objects, matching Java. Names come from `ClassInfo` comments emitted by `KotlinInfoParser` for every method and constructor; see [Parser: Callable Parameter Names](../parser/README.md#callable-parameter-names) for the signature-matching contract and resolution limits.

### Source Hierarchy Lifecycle

Kotlin hierarchy names follow the [parser's qualified-identity contract](../parser/README.md#hierarchy-metadata).
`SourceInfo` supplies the project context; the editor's general Kotlin type resolution remains out of scope.

- A Kotlin metadata request resolves exact project targets (including enclosing targets for nested names)
  from **current source declarations**, not old compiled output. Kotlin PSI supplies kind/identity before
  compilation without opening editors or recursively analysing the target's parents. Java source kind is
  read through the existing parser without a project-bytecode resolver; Stride's generated Java is used only
  when compiled. Unreadable, unsupported or mismatched source identities reserve the target name as unknown.
- Source-file existence is checked even if a target's cached source type has not yet refreshed. Only a
  current compiled-only target or a library may use reflection; an untracked `.class` resource inside the
  project directory cannot be treated as a library. Java/Stride metadata caching and analysis paths are unchanged.
- The Kotlin PSI snapshot is cached by complete source text and filename, avoiding timestamp/size races.
  Hierarchy lookup runs anew on every `getInfo` request: creating, changing or removing another source
  declaration can shadow/unshadow an import even when this class's own source is unchanged. This is a
  request-time refresh, not a new project-wide watcher or invalidation service.
- `setSourceModified` clears both metadata and the snapshot. Read failure also clears cached results;
  unsupported source replaces the previous `ClassInfo` with null. `getInfoIfAvailable` deliberately remains
  a non-refreshing view of the last result for existing role-assignment callers.
- Greenfoot takes one superclass snapshot per diagram recalculation before recursive grouping, avoiding
  repeated metadata work in the recursion. See the [Greenfoot diagram contract](../../greenfoot/guifx/classes/README.md).
  The remaining Greenfoot workflow boundaries are owned by the [Greenfoot spec](../../greenfoot/README.md#kotlin-local-scenarios).

### Dependency Editing (UI -> Source)

When a user draws an inheritance arrow, `FlowLanguageSupport` strategy handles the source modification:
- Java: `" extends " + name`, `" implements " + name`
- Kotlin: `" : " + name + "()"`, `", " + name` (unified supertype list)

Kotlin supertype edits use PSI directly in `KotlinLanguageSupport`; hierarchy metadata does not carry supertype-editing `Selection`s. `KotlinInfoParser` still supplies PSI-derived package-directive selections for package editing.

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
- **No Kotlin object/companion** *diagram representation* -- a companion object is not drawn as its own node or `<<companion>>` stereotype. Companion **methods** are nonetheless invokable as static-style operations on the enclosing class (see [Companion Object Methods](companion-objects/README.md)). Top-level `object` declarations remain unrepresented.
- **No Stride <-> Kotlin conversion**
- **Template subset** -- only 5 Kotlin templates vs 6+ Java templates

---

## Dependencies

Cross-module: `KotlinInfoParser` (parser module), `KotlinLanguageSupport` (editor module), `KotlinCompiler` (compiler module)
