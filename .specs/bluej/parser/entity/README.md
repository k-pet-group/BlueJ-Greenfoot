# Entity Resolution

Resolves Java identifiers to their semantic meaning (29 files). Implements a type hierarchy for entities: packages, classes, values, type parameters, wildcards, and constants. Supports **lazy resolution** -- entities are created as `UnresolvedEntity` and resolved on demand.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `JavaEntity` (abstract) | Base: `resolveAsValue()`, `resolveAsType()`, `resolveAsPackageOrClass()`, `getSubentity()` |
| `EntityResolver` (interface) | `resolvePackageOrClass(name)`, `resolveQualifiedClass(name)`, `getValueEntity(name)` |
| `TypeEntity` / `PackageEntity` | Resolved type or package node |
| `UnresolvedEntity` / `UnresolvedSubEntity` | Deferred resolution; resolves lazily via `EntityResolver` |
| `ParsedReflective` | Wraps `ParsedTypeNode` to provide reflective API for **uncompiled** source |
| `PositionedResolver` | Wraps a `JavaParentNode` + position; rejects forward references |
| `ClassLoaderResolver` | Resolves via `ClassLoader.loadClass()` for compiled classpath types |
| `TparEntity` | Type parameter with lazy bound resolution to prevent cycles |
| `ErrorEntity` | Propagating error state: all methods return self or null (prevents NPE cascades) |

---

## Resolution Order (Scope Chain)

1. Local variables (before cursor position)
2. Method parameters
3. Enclosing scope variables
4. Type members (fields) of enclosing type
5. Inherited members
6. Type parameters of enclosing method/class
7. Inner classes
8. Explicit imports
9. Wildcard imports / `java.lang.*`
10. Classes in same package
11. Fully-qualified via `ClassLoader.loadClass()`
12. Unresolved names assumed to be packages

Implemented via resolver chaining: `PositionedResolver` -> `ParsedCUNode` -> `PackageResolver` -> `ClassLoaderResolver`.

---

## Three Resolution Paths

| Method | Returns | Use case |
|--------|---------|----------|
| `resolveAsValue()` | `ValueEntity` or null | Variable, field, parameter access |
| `resolveAsType()` | `TypeEntity` or null | Type reference in declarations |
| `resolveAsPackageOrClass()` | `PackageOrClass` or null | Import resolution, qualified names |

---

## Key Design Decisions

- **Lazy resolution**: `UnresolvedEntity` avoids ordering problems during parse-tree construction; handles forward references naturally
- **Three resolution paths**: Mirrors Java's name resolution rules where the same identifier can mean different things in different contexts
- **Error propagation**: `ErrorEntity` returns itself from all methods rather than null, preventing NPE cascades
- **ParsedReflective adapter**: Presents uncompiled source through the same `Reflective` interface as compiled code, enabling code completion for code that won't compile
- **`TparEntity` lazy bounds**: Prevents infinite recursion on self-referencing type parameters like `<T extends Comparable<T>>`
