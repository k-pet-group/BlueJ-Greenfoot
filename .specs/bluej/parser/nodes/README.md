---
id: bluej-parser-nodes
type: submodule-design
title: "Parse Tree Nodes"
status: active
parent: bluej-parser
---

# Parse Tree Nodes

A scope-oriented parse tree (25 files) for incremental reparsing and editor features. **Not a full AST** -- nodes represent scope boundaries (compilation units, types, methods, blocks) rather than individual expressions. Child nodes are stored in a red-black tree keyed by relative document offset.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `ParsedNode` | Abstract base: position, size, node type, child tree, `textInserted()`/`textRemoved()` |
| `ParentParsedNode` | Routes edits to children, handles `NODE_GREW`/`SHRUNK`/`REMOVE_NODE` |
| `JavaParentNode` | Adds entity resolution, tracks variables and inner types |
| `IncrementalParsingNode` | Multi-state resumable parsing with state markers and `doPartialParse()` |
| `ParsedCUNode` | Compilation unit root; manages imports |
| `ParsedTypeNode` | Class/interface/enum; stores type params, superclass, implements |
| `TypeInnerNode` | Body of a type; tracks methods and field variables |
| `MethodNode` / `MethodBodyNode` | Method scope and body scope |
| `FieldNode` | Field/local variable; supports `var` type inference |
| `RBTreeNode` / `NodeTree` | Red-black tree for O(log n) child management with relative offsets |
| `ReparseableDocument` (interface) | Bridges parse tree to document model: `scheduleReparse()`, `makeReader()`, `flushReparseQueue()` |

---

## Key Design Decisions

- **Red-black tree for children**: O(log n) position updates vs. O(n) for a flat list. Critical for large files.
- **Relative offsets**: Only the tree path from edit point to root needs adjustment on each edit.
- **State markers as checkpoints**: Allows resumable parsing from mid-file, avoiding full reparse.
- **Not a full AST**: Expressions are not represented as nodes. This keeps the tree small. Expression analysis is deferred to `TextParser`/`CompletionParser` on demand.
- **`growsForward()` / `marksOwnEnd()` protocol**: Controls edit ownership at node boundaries (documented in `bluej/doc/BlueJ-parser.txt`).

---

## Incremental Parsing Algorithm

Edit propagation via `textInserted()`/`textRemoved()`:
1. Find affected child via red-black tree lookup
2. Delegate to child; child returns a status code: `ALL_OK`, `NODE_GREW`, `NODE_SHRUNK`, `REMOVE_NODE`
3. Parent handles cascading reparse if needed

Only the **innermost affected scope** does real work.

`IncrementalParsingNode` uses state markers as parse checkpoints. `doPartialParse()` returns control codes (`PP_OK`, `PP_ENDS_NODE`, `PP_PULL_UP_CHILD`, `PP_EPIC_FAIL`, etc.) enabling resumable partial reparsing.

---

## Observer Pattern

`NodeStructureListener` notifies the editor of `nodeAdded()`, `nodeRemoved()`, `nodeChangedLength()` -- used for syntax highlighting, code folding, and outline updates.
