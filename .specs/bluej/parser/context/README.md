---
id: bluej-parser-context
type: submodule-design
title: "Compilation Context"
status: active
parent: bluej-parser
---

# Compilation Context

Persisted metadata about compilation units (5 files). Preserves javadoc comments, parameter names, and documentation in `.ctxt` files alongside compiled classes -- information lost in `.class` bytecode.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `CompilationUnitContext` | Metadata container: list of `CommentEntry` items, class name, read-only flag |
| `CommentEntry` | Immutable record: `target` (method/field signature), `text` (javadoc), `paramNames` |
| `CompilationUnitContextLoader` | Loads/caches contexts from `.ctxt` files; bridges `ClassInfo` -> `.ctxt` persistence |
| `PropertyContextFormat` | Serializes/deserializes `.ctxt` files using Java `Properties` format |
| `ClassLoaderProvider` (interface) | Decouples loader from `Project` |

---

## Key Contracts

- `.ctxt` files use Java `Properties` format (`numComments`, `comment0.target`, `comment0.text`, `comment0.params`)
- Loading uses a multi-level lookup: cache -> classpath resource -> project directory
- Cached contexts are **read-only** -- modifications create a new context and write a new file
- `ClassInfo` is FX-thread-bound; `extractClassInfoData()` safely copies data on FX thread so persistence can happen on any thread

---

## Design Decisions

- **Shadow metadata files**: `.ctxt` files solve the problem of compiled bytecode losing source-level information
- **Properties format**: Simple, human-readable, no external dependencies
- **ClassLoaderProvider interface**: Decouples from the heavy `Project` class, enabling testing
