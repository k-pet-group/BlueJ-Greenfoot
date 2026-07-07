---
id: kotlin-for-bluej-editor-highlighting
type: submodule-design
title: "Kotlin Code Highlighting"
status: active
parent: kotlin-for-bluej-editor
depends-on:
  - bluej-editor-highlighting
tags:
  - kotlin
---

# Kotlin Code Highlighting

> Token-level syntax coloring and how it integrates with BlueJ's shared highlighting infrastructure.

---

## Overview

Kotlin highlighting reuses BlueJ's shared `Token.TokenType` enum and `java-colors.css` so both languages display with the **same color palette**. Keywords are grouped by grammatical role (control flow, declarations, references) rather than by Kotlin's hard/soft keyword distinction.

Pipeline: PSI `KotlinLexer` -> `KotlinToken.mapTokenType()` -> soft keyword reclassification -> `KotlinToken.toDisplayType()` -> `Token.TokenType` -> `getCSSClass()` -> `java-colors.css` -> FlowEditorPane

---

## Token Classification

### KEYWORD1 -- Control Flow and Modifiers (`#660033` dark purple)

- **Control flow (17):** `if`, `else`, `for`, `while`, `do`, `when`, `break`, `continue`, `return`, `throw`, `try`, `is`, `!is`, `in`, `!in`, `as`, `typeof`
- **Visibility (4):** `private`, `public`, `internal`, `protected`
- **Other modifiers (26):** `abstract`, `open`, `final`, `sealed`, `override`, `inner`, `lateinit`, `data`, `value`, `inline`, `tailrec`, `operator`, `infix`, `const`, `suspend`, `annotation`, `reified`, `external`, `crossinline`, `noinline`, `expect`, `actual`, `contract`, `vararg`, `out`

### KEYWORD2 -- Declarations (`#cc0000` red)

- **Type declarations:** `class`, `interface`, `enum`, `object`, `typealias`, `package`, `import`
- **Member declarations:** `fun`, `val`, `var`
- **Structural:** `constructor`, `init`, `companion`, `where`, `by`, `get`, `set`

### KEYWORD3 -- References (`#006699` blue)

`this`, `super`, `null`, `true`, `false`

### Other

| Category | Color |
|----------|-------|
| `STRING_LITERAL` (quotes, string parts, escape sequences, template markers) | `#006600` green |
| `CHAR_LITERAL` (integer, float, character literals) | black (no CSS rule) |
| `COMMENT_NORMAL` (line, block, shebang) | `#999999` gray |
| `COMMENT_JAVADOC` (KDoc) | `#000099` dark blue |
| `LABEL` (annotation `@`) | `#999999` gray |
| `DEFAULT` (identifiers, operators, delimiters) | `#000000` black |

---

## Soft Keyword Reclassification

PSI returns soft keywords (`abstract`, `data`, `sealed`, etc.) as `IDENTIFIER`. `KotlinLexer.nextToken()` reclassifies by text via `KotlinToken.mapSoftKeywordByText()`. This is context-free (highlights even when used as identifiers), matching IntelliJ's behavior.

---

## Scope Highlighting

Same scope types and colors as Java:

| Kotlin Construct | Color |
|-----------------|-------|
| `class`, `interface`, `enum class`, `object`, `companion object` | Green |
| `fun`, `init`, constructors | Yellow |
| `for`, `while`, `do` | Pink |
| `if`/`else`, `try`/`catch`, `when` | Blue |

Rendering pipeline shared with Java via `JavaSyntaxView.colorsForNode()`.

---

## Multiline String Handling

`KotlinParsedCUNode.handlesMultilineStrings()` returns `true`, bypassing `MultilineStringTracker`. Instead:
- `KotlinStringNode` covers the string literal
- Gaps -> `STRING_LITERAL` (green)
- `$name`/`${expr}` template bodies -> child nodes with normal code tokenization

---

## Known Gaps

1. No `COMMENT_SPECIAL` (BlueJ special comment patterns render as normal comments)
2. No distinct numeric highlighting (renders as black)
3. `@` annotation is gray (not purple like Java modifiers)
4. No `PRIMITIVE` category (Kotlin's `Int`, `Boolean` are classes, render as black)
