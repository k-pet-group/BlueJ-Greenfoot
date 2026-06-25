# Top-Level Functions Support

> Kotlin files with only top-level functions as first-class entities on BlueJ's class diagram.

**Parent spec:** [Kotlin for BlueJ -- Architecture Design](../DESIGN_DOC.md)

---

## Core Concept: One Concept Per File

Each `.kt` file contains either:
- **A class/object declaration** -> standard `ClassTarget` + standard roles
- **Top-level functions only** -> `ClassTarget` + new `KotlinFileRole`

Mixed files (class + top-level functions) are not supported. Extra declarations compile but only the class appears on the diagram.

---

## Facade Class Naming

Kotlin compiles a top-level-functions file into a JVM facade class whose
name is the source-file stem with its **first letter capitalised** and
`Kt` appended. The naming mismatch is resolved in `ClassTarget`:

| Source file | Compiled facade class |
|---|---|
| `Utils.kt` | `UtilsKt` |
| `utils.kt` | `UtilsKt` (first letter uppercased) |
| `myUtil.kt` | `MyUtilKt` |
| `A.kt` / `a.kt` | `AKt` |

The capitalisation rule lives in one place:
`bluej.parser.kotlin.KotlinParserUtils.kotlinFacadeClassName(stem)`. Every
site that needs to derive the compiled facade name from a source stem
uses that helper:

- `ClassTarget.getQualifiedName()` — class-loader lookup name
- `ClassTarget.getClassFile()` — `.class` file path
- `ClassTarget.InnerClassFileFilter` — inner-class match prefix
- `ClassTarget.removeGeneratedFiles()` — facade `.ctxt` cleanup
- `Package.findTargets()` — facade-class dedup against the set of
  expected facade names built from the Kotlin source stems

The class itself stores a `volatile boolean isKotlinFacade` flag so
`getQualifiedName()` can stay `@OnThread(Tag.Any)` while role assignment
remains on FXPlatform.

---

## Key Components

### KotlinFileRole (new)

- `getStereotypeLabel()` -> `"functions"` (displayed as `<<functions>>`)
- `getClassConstructorOperations()` -> empty list (facade has private constructor)
- `getClassStaticOperations()` -> inherited from ClassRole (reflects facade's static methods = top-level functions)
- `canConvertToStride()` -> false

### KotlinInfoParser changes

- If no `KtClassOrObject` found but `KtNamedFunction` declarations exist, calls `buildTopLevelFunctionsInfo()`
- Synthesizes `ClassInfo` with `topLevelFunctionsOnly=true`, name from file stem
- Reuses `extractMethod()` for dependency extraction (parameter types, return types, KDoc)

### ClassTarget changes

- `determineRole()`: checks `ClassInfo.isTopLevelFunctionsOnly()` -> assigns `KotlinFileRole`
- `getQualifiedName()` / `getClassFile()`: append `"Kt"` suffix when `isKotlinFacade`
- `removeGeneratedFiles()`: facade-aware `.ctxt` deletion (`UtilsKt.ctxt`)
- `prepareForRemoval()`: evicts `CompilationUnitContextLoader` cache + removes debugger breakpoints

### Package.findTargets() changes

- `KotlinSourceFilter` discovers `.kt` files (after Stride loop for priority)
- Facade class dedup: `UtilsKt.class` skipped if `Utils.kt` source exists in `interestingSet`

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | One concept per file | Educational simplicity; matches BlueJ's one-class-per-file Java model |
| 2 | Reuse ClassTarget with KotlinFileRole | Leverages entire existing infrastructure; ~300 lines new vs ~2000+ for a dedicated target type |
| 3 | ClassTarget appends "Kt" suffix | Handles compiler naming convention without requiring `@file:JvmName` |
| 4 | Auto-detect in single parse() method | Callers get ClassInfo regardless of class vs functions file |
| 5 | Suppress constructors, inherit static methods | Facade's static methods are exactly the top-level functions; zero new invocation code |
| 6 | Volatile `isKotlinFacade` flag | Thread-safe reads from any thread while role assignment stays on FXPlatform |
| 7 | Facade class deduplication in findTargets() | Prevents spurious `UtilsKt` ClassTarget alongside real `Utils` target |

---

## Known Limitations

- **Mixed files unsupported** -- only class is represented; functions invisible on diagram
- **Facade class naming not customizable** -- `Kt` suffix always appended
- **JVM type wrapping** -- `kotlin.Int` shows as `java.lang.Integer` on object bench (inherent to JVM compilation)
