# Java Parser

An incremental Java parser (103 files) that builds and maintains a scope-based syntax tree for the editor. Provides syntax highlighting, code completion, symbol resolution, and class metadata extraction.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `JavaParser` | Recursive-descent parser with ~80 callback methods for syntactic events |
| `EditorParser` | Subclass that builds a `ParsedCUNode` scope tree via a `scopeStack` |
| `TextParser` | Expression parser with operand/operator stacks; resolves expression types via `EntityResolver` |
| `CompletionParser` | Extends `TextParser`; parses up to cursor position, captures suggestion entity and token |
| `InfoParser` | Subclass that extracts class metadata into `ClassInfo` |

---

## Internal Organization

| Sub-package | Purpose |
|-------------|---------|
| `lexer/` | `JavaLexer` -> `JavaTokenFilter` tokenization pipeline |
| `nodes/` | Scope tree nodes with red-black tree position tracking |
| `entity/` | Semantic entity resolution: types, packages, values, wildcards, constants |
| `context/` | `.ctxt` file persistence for javadoc, parameter names, class metadata |
| `symtab/` | `ClassInfo` data structure and `Selection` for source ranges |

---

## Key Design Decisions

1. **Callback-based parser**: Multiple parser subclasses (editor, completion, info extraction) share the same grammar. Behavior is in subclass callbacks.
2. **Gap buffer document**: `HoleDocument` gives O(1) insertions at the edit point. Combined with incremental parsing, keystroke response is sub-millisecond.
3. **Scope tree, not full AST**: Only scope boundaries are nodes. Expressions are parsed on-demand by `TextParser`/`CompletionParser`. This keeps the tree small.
4. **Reparse batching**: Queuing reparses prevents redundant work during rapid typing. The queue is flushed once per edit event.
5. **Separate InfoParser**: Class metadata extraction (`ClassInfo`) is a distinct pass from scope-tree building.

---

## Dependencies

Uses: `editor/` (`HoleDocument`, `FlowEditor`), `utility/`

---

## Existing Documentation

`bluej/doc/BlueJ-parser.txt` -- Detailed architecture reference covering the callback pattern, scope stack, expression stacks, incremental parsing, and node boundary rules.
