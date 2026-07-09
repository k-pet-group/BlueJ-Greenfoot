---
id: bluej-parser-lexer
type: submodule-design
title: "Lexer (Tokenizer)"
status: active
parent: bluej-parser
---

# Lexer (Tokenizer)

Java source tokenization (5 files). Converts raw text into a stream of located tokens for the parser.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `JavaLexer` | Hand-written character-level tokenizer; reads `Reader` char-by-char, emits `LocatableToken` stream |
| `JavaTokenFilter` | Wraps `JavaLexer`; strips comments/whitespace, saves last comment, provides `LA(n)` lookahead via `LinkedList` buffer |
| `JavaTokenTypes` (interface) | ~120 integer constants for token types (keywords, operators, literals) |
| `LocatableToken` | Token carrying type, text, and begin/end `(line, column)` positions |

---

## Key Contracts

- `JavaLexer` is hand-written (not generated) for precise control over position tracking and incremental-friendly behavior
- `>>` / `>>>` shift operator vs. nested generic closing brackets resolved in `JavaLexer.getGTType()`
- Contextual keyword reclassification (`var`, `record`, `sealed`) is handled by `JavaParser`, not the lexer
- The lexer can be constructed with a `Reader` spanning only part of a document for partial reparsing

---

## Design Decisions

- **Hand-written lexer**: Allows precise control and incremental-friendly partial document reading
- **Separate filter stage**: Lexer does tokenization; filter handles comment stripping and lookahead buffering
