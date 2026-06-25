# Java Code Highlighting

Token-level syntax coloring and structural scope background highlighting in the BlueJ Java editor.

---

## Two Independent Highlighting Systems

1. **Token Highlighting** -- foreground text colors based on lexical category (keywords, strings, comments)
2. **Scope Highlighting** -- background fill and border colors based on structural nesting (class body, method body, loop, conditional)

Both use CSS (`java-colors.css`) and render together in `FlowEditorPane`.

---

## Token Categories (Token.TokenType enum)

| Category | Examples | Color |
|----------|----------|-------|
| `KEYWORD1` | Control flow (`if`, `for`, `return`) and modifiers (`public`, `static`, `final`) | `#660033` (dark purple) |
| `KEYWORD2` | Type declarations (`class`, `interface`, `enum`, `record`, `import`) | `#cc0000` (red) |
| `KEYWORD3` | Reference keywords (`this`, `super`, `null`, `true`, `false`) | `#006699` (blue) |
| `PRIMITIVE` | Primitive types (`int`, `boolean`, `void`, etc.) | `#cc0000` (red) |
| `STRING_LITERAL` | `"..."`, `"""..."""` text blocks | `#006600` (dark green) |
| `COMMENT_NORMAL` | `// ...`, `/* ... */` | `#999999` (gray) |
| `COMMENT_JAVADOC` | `/** ... */` | `#000099` (dark blue) |
| `COMMENT_SPECIAL` | `//#...`, `/*#...*/` | `#ee00bb` (magenta) |
| `DEFAULT` | Identifiers, operators, numbers, delimiters | `#000000` (black) |

---

## Edge Cases

- `super` is context-sensitive: after `?` wildcard -> KEYWORD2; otherwise -> KEYWORD3
- `default` appears in two roles but `isModifier()` check runs first, so modifier context wins
- Numeric literals fall through to DEFAULT -- not distinctly highlighted
- `CHAR_LITERAL` has its own TokenType but no CSS rule -- renders as default black

---

## Scope Color Mapping

| Node Type | Border | Fill |
|-----------|--------|------|
| `NODETYPE_TYPEDEF` | `rgb(188,218,188)` green | `rgb(225,248,225)` green |
| `NODETYPE_METHODDEF` | `rgb(215,215,205)` gray-yellow | `rgb(250,250,180)` yellow |
| `NODETYPE_ITERATION` | `rgb(210,177,210)` purple | `rgb(248,233,248)` pink |
| `NODETYPE_SELECTION` | `rgb(188,188,210)` blue | `rgb(233,233,248)` blue |

User-configurable intensity slider (0-20) via `ScopeHighlightingPrefDisplay`.

---

## Pipeline

- **Token**: `JavaLexer` -> `JavaParentNode.tokenizeText()` -> `Token.TokenType` -> CSS class -> styled `Text` nodes
- **Scope**: Parser `ParsedNode` tree -> `JavaSyntaxView.colorsForNode()` -> `BackgroundItem` regions -> Canvas behind text

---

## Key Files

- `bluej/parser/Token.java` -- `TokenType` enum
- `bluej/parser/nodes/JavaParentNode.java` -- `tokenizeText()` classification
- `bluej/editor/flow/JavaSyntaxView.java` -- rendering
- `bluej/lib/stylesheets/java-colors.css` -- all color definitions
