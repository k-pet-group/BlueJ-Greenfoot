# Stride Block-Based Editor

The largest subsystem in BlueJ (223 files). Implements the Stride language -- a block-based visual programming interface that generates Java code. Students manipulate visual "frames" instead of typing text, with editable "slots" for expressions and identifiers.

---

## Key Concepts

- **Frame**: A visual block representing a Java construct (method, if, while, variable, etc.)
- **Slot**: An editable input area within a frame (expression, identifier, type)
- **Canvas**: A container that holds frames vertically, like a code block
- **CodeElement**: Abstract syntax representation of Stride code, independent of visual frames

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `framedjava/frames/` | Visual frame components: MethodFrame, VarFrame, IfFrame, WhileFrame, ForFrame, etc. |
| `framedjava/elements/` | Abstract element definitions for code constructs |
| `framedjava/slots/` | Editable slot widgets within frames |
| `framedjava/ast/` | Abstract syntax tree for Stride code |
| `framedjava/convert/` | Bidirectional Java <-> Stride conversion |
| `framedjava/errors/` | Error handling and display within frames |
| `generic/` | Generic frame components shared across frame types |
| `operations/` | Frame operations: cut, copy, paste, delete, drag-and-drop |

---

## Key Interfaces

- **CodeElement** (abstract) -- Abstract syntax that can generate Java source via `toJavaSource()`
- **FrameEditor** -- Visual editor integration point (located in `editor/stride/`)
- **FXTabbedEditor** -- Tabbed editor container (located in `editor/stride/`)

---

## Data Flow

- **Stride -> Java**: Visual Frames -> CodeElement tree -> `toJavaSource()` -> `.java` file on disk -> compiler
- **Java -> Stride**: Java source -> parser AST -> `convert/` -> CodeElement tree -> Frames + Slots

---

## Dependencies

Uses: `parser/`, `editor/`, `compiler/`
