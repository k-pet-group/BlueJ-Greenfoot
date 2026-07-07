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

## Dependencies

Uses: Java reflection API (`java.lang.reflect`)
