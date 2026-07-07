---
id: bluej-parser-symtab
type: submodule-design
title: "Symbol Table"
status: active
parent: bluej-parser
---

# Symbol Table

Class metadata extraction and source position tracking (3 files).

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `ClassInfo` | Metadata container populated by `InfoParser`: name, modifiers, superclass, interfaces, methods, javadoc, type parameters, source positions |
| `ClassInfo.SavedComment` | Inner class: `target` (signature), `comment` (javadoc text), `paramnames` |
| `Selection` | Wraps a `SourceSpan` (start line/col -> end line/col); used for marking refactoring targets |

---

## Key Contracts

- `ClassInfo` is a **flat snapshot**, not part of the incremental scope tree. Used for class diagram display, `.ctxt` persistence, dependency tracking, and refactoring.
- `Selection` is **mutable** (`combineWith()`, `extendEnd()` modify internal state). Typically created during a single `InfoParser` pass and then read-only in practice, but not thread-safe by construction.
- The `used` list collects all type references encountered during parsing, feeding the class diagram's dependency arrows.
