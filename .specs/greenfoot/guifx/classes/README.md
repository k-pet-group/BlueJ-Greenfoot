---
id: greenfoot-guifx-classes
type: submodule-design
title: "Greenfoot Class Diagram"
status: active
parent: greenfoot-guifx
---

# Greenfoot Class Diagram

> Auto-generated from code analysis. Review and refine.

UML-style class diagram for the Greenfoot IDE sidebar (10 files). Displays Actor/World class hierarchy with inheritance arrows, selection management, and import dialogs.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `GClassDiagram` | Main class diagram visualization component |
| `GClassNode` (abstract) | Base class diagram node |
| `LocalGClassNode` | User-defined class node |
| `BuiltInGClassNode` | Greenfoot library class node (Actor, World) |
| `ClassGroup` | Organizes classes in hierarchy groups |
| `ClassDisplaySelectionManager` | Selection state management |
| `InheritArrow` | Inheritance relationship visual |
| `ClassDisplay` | Individual class display rendering |
| `ImportClassDialog` | Import external class into project |

---

## Design Decisions

### Hierarchy Metadata Boundary

- Grouping compares qualified superclass identities. Kotlin must supply these through the shared
  [parser contract](../../../kotlin-for-bluej/parser/README.md#hierarchy-metadata), not via Greenfoot-specific
  import strings or `World`/`Actor` guesses. Same-package source chains work before compilation.
- Source metadata is authoritative even when its superclass is unresolved. Missing/unsupported Kotlin
  source metadata does **not** trigger reflection of potentially stale output after edits or failed compilation.
  Current compiled-only targets may still use reflection. Existing Java/Stride source/fallback behaviour is retained.
- Each `recalculateGroups` builds one superclass snapshot before recursively distributing targets into
  World, Actor and Other groups. Recursive visits reuse that snapshot; this bounds metadata work and ensures
  a consistent refresh when another declaration changes import resolution. The snapshot is discarded after
  grouping. `addClass` continues to use source metadata for initial placement.
- The hierarchy contract is independent of deferred Kotlin workflows. Automated boundary tests do not
  constitute installed GUI/runtime acceptance; see [Greenfoot's support boundary](../../README.md#kotlin-local-scenarios).
