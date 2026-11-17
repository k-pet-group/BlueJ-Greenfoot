---
id: bluej-views
type: submodule-design
title: "Views"
status: active
parent: bluej
---

# Views

Class reflection view layer (12 files). Provides a cached, read-only representation of Java classes and their members for display in inspectors, code completion, and the extensions API.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `View` | Cached representation of a Java class; factory method `getView()` with thread-safe static cache |
| `MemberView` (abstract) | Base for class members; hierarchy: `FieldView`, `CallableView` -> `ConstructorView`, `MethodView` |
| `ViewFilter` | Filters members by visibility/accessibility criteria |
| `Comment` / `CommentList` | Javadoc comment storage with lazy loading |

---

## Key Patterns

- **Flyweight + Cache**: `View.getView()` caches View instances by Class object
- **Lazy loading**: Comments loaded on demand via `loadComments()`

---

## Kotlin Metadata Across Loaders

The IDE and student/project classloaders are siblings under Boot, so installed Kotlin classes can
carry a different `kotlin.Metadata` class identity from the IDE's metadata library. Typed annotation
lookup alone silently loses companion callables and generated-property labels despite valid bytecode.

The companion, property-accessor and file-facade detectors share a loader-neutral read: use a typed
annotation when available; otherwise identify `kotlin.Metadata` by annotation type name and copy its
primitive/String-array values into the metadata library's own annotation factory. Strict metadata
parsing and the detectors' logged, empty-result failure behavior remain unchanged. This is not a
reason to expose compiler/metadata jars or the IDE loader to student code.

### Key Lifecycles — Metadata Reads

A read is transient and synchronous with reflective member enumeration or source-replay rendering.
Foreign annotation values are adapted for that read only; no extra classloader, global metadata cache
or retained student annotation is introduced. The resulting member views retain the existing View cache and eviction
lifecycle. See [companion operations](../../kotlin-for-bluej/diagram/companion-objects/README.md).

## Kotlin File-Facade Identity

Source replay distinguishes an ordinary Kotlin file facade from a real class by parsed metadata kind,
not by a class-name suffix. A facade's static methods represent package-level Kotlin declarations;
its JVM container is not a Kotlin source receiver. Missing or unreadable metadata does not establish
facade identity: parsing failures are logged and classification returns false, without guessing from
names. The query exposes no metadata-library types and follows the transient read lifecycle above.
Multifile facades and custom JVM source-name recovery are not added by this boundary.

## Kotlin Properties in Source Replay

Source generation must recover a property's declared Kotlin name from its JVM accessor signature,
not infer it by decapitalizing `getX`/`setX`. Acronyms, accessor `@JvmName` annotations and ordinary
same-name overloads make that inference unsafe. The property detector provides this separate lookup
for zero-argument getters and one-argument setters, including static bridges to companion properties.
It shares the loader-neutral metadata reader and introduces no global classloader cache. Existing
menu-label detection is unchanged; source replay does not require exposing metadata jars to students.

## Dependencies

Uses: Java reflection API (`java.lang.reflect`) and the IDE-only Kotlin metadata library.
