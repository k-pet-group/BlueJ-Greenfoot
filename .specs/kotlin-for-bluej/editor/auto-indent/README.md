# Kotlin Auto-Indent

> Token-driven indenter for `.kt` files. Computes target indents from
> brace/paren depth without reading the parse tree, so indent correctness
> is independent of `KotlinPsiScopeBuilder` shape.

---

## Why a separate indenter

`FlowIndent.calculateIndentsAndApply` is invoked from three call sites
(auto-layout menu, insert-method template, programmatic insertion). Its
default implementation walks `ParsedNode` and queries
`child.isInner()` / `child.isContainer()` / `JavaParentNode.isSwitchBlockNode()`.
For Kotlin those queries depend on what `KotlinPsiScopeBuilder` emits, and
every new Kotlin construct (primary-constructor params, property accessors,
`when` entries, anonymous objects, multi-line `when` subjects, ...) has
historically required another scope-builder patch. The token-driven
indenter sidesteps that loop: brace and paren tokens are unambiguous and
don't depend on scope-tree shape.

---

## Dispatch

`FlowIndent.calculateIndentsAndApply` checks the parser root once:

```java
if (parser != null && parser.getParser() instanceof KotlinParsedCUNode) {
    return KotlinIndent.calculateIndentsAndApply(...);
}
// existing tree walk
```

No strategy interface, no `null`-means-fallback protocol. The
`FlowLanguageSupport` strategy intentionally does *not* cover auto-indent
because the two implementations live in the same package and one
delegates to the other — the abstraction would not earn its keep.

The `parser` and `MultilineStringTracker` parameters are accepted for
signature parity with `FlowIndent.calculateIndentsAndApply` but are not
consulted by the Kotlin path — string detection is driven entirely by the
token stream. Either may be `null` in unit tests using a plain
`HoleDocument`.

---

## Algorithm

`KotlinIndent.computeIndents(CharSequence)` walks `KotlinLexer`'s token
stream and returns an `int[]` of target indent widths per line (0-based).
A sentinel of `LEAVE_UNTOUCHED` (`-1`) flags lines that must not be
modified by the apply pass — namely the interior of a multi-line
`"""..."""` string and its closing line.

### Per-LBRACE step stack

`computeIndents` maintains a stack of indent steps (default 1 per
`LBRACE`). The total indent at any line = `sum(stack) + parenDepth`.
A line whose first token is the **soft keyword** `KW_GET` or `KW_SET`
(directly inside a class body) is treated as a property-accessor
continuation:

- the accessor line itself sits at `+1` step (continuation indent)
- the next `LBRACE` we see on that line pushes a step-2 entry (so the
  accessor body sits at `+2` relative to the property declaration)

That keeps property accessors lined up correctly:

```
class Card(val rang: String) {
    val wert: Int
        get() {                          <- +1 continuation
            return when (rang) {         <- +2 body, +3 when body
                "A" -> 14
                else -> rang.toInt()
            }
        }
}
```

### Closing-token un-indent

A line whose first non-whitespace token is `RBRACE`, `RBRACKET`, or
`RPAR` sits one step less than its body content. The step popped is the
one at the top of the stack — so the closing `}` of an accessor body
correctly lines up with `get(...`, even though that brace pushed 2 steps.

### Multi-line string preservation

`OPEN_QUOTE` whose text equals `"""` flips an `inMultilineString` flag;
`CLOSING_QUOTE` clears it. Crucially, the flag is captured **at the moment
the first token of each line is emitted** (alongside brace-stack and
paren-depth), not read live at line finalize. Without that, a line whose
first token is the closing `"""` would have already cleared the flag and
been re-indented — corrupting the string's content. Blank lines crossed
while `inMultilineString` is true are flagged `LEAVE_UNTOUCHED` too.

### Comments

Block-comment continuation lines whose first non-whitespace character is
`*` get a one-space pad after the parent indent so the asterisks align
with `/*` and `*/`. This is implemented in the apply pass by inspecting
the current line's first character; `computeIndents` itself does not
specialise comments.

---

## Apply pass

`KotlinIndent.calculateIndentsAndApply` does three passes:

1. **Collapse blank lines.** Consecutive blank lines → one; whitespace-only
   blank lines → stripped to a bare newline. Lines flagged
   `LEAVE_UNTOUCHED` are skipped (string interior).
2. **Recompute `computeIndents`** on the post-collapse content (offsets
   have shifted).
3. **Rewrite leading whitespace.** For each line in `[startPos, endPos]`
   with `target[line] != LEAVE_UNTOUCHED` whose current leading whitespace
   doesn't already equal the target, replace it. The caret is shifted
   using the same before-me / inside-me / after-me rule that
   `FlowIndent.DocumentIndentAction` uses.

`AutoIndentInformation.perfect` is `true` iff no rewrite happened and no
blank-line collapse fired — the same contract as the Java path, so the
"already perfectly indented" toast continues to work.

---

## Threading

`KotlinIndent` inherits the package default `@OnThread(Tag.FXPlatform)` (it
mutates the `Document`). The pure-text helpers (`computeIndents`,
`countLines`, `isWhiteSpaceOnly`, etc.) are individually tagged
`@OnThread(Tag.Any)` so they're broadly callable — in particular,
`computeIndents` can be unit-tested without an FX-thread harness.

---

## What is intentionally not handled

| Case | Reason |
|---|---|
| `checkMethodSpacing` (insert blank line between adjacent methods) | Would require re-introducing parse-tree method detection — exactly the dependency this indenter removes. Cosmetic; can be added back as a token-level scan (adjacent `KW_FUN` declarations at the same brace depth with no blank line) if pilot users ask. |
| `switch` / `case` un-double-indent | Kotlin has no `switch`/`case`; `when` entries indent at body depth like any other block contents. |
| Multi-line generic `<…>` type parameter lists | `<` / `>` are also less-than/greater-than operators; without parsing they're ambiguous. Continuation falls through to surrounding indent; not corrupted, just possibly under-indented. Extremely rare in student code. |
| Smart alignment to the column of an opening delimiter (IntelliJ-style) | The existing Java tree-driven indenter doesn't do this either; consistency wins over IntelliJ parity. Continuation indent is a flat `+1` step. |
| Mixed Java + Kotlin in one file | Not a thing. |
| Whole-file reformatting (line wrapping, import sorting, paren spacing) | Out of scope; that would be ktfmt territory. |

---

## Test fixtures

`bluej/src/test/java/bluej/editor/flow/KotlinIndentTest.java` exercises the
algorithm with a `HoleDocument` and asserts `runTest(expected, src)` plus
idempotency (`runTest(expected, expected)` must round-trip with
`isPerfect() == true`):

- `testComputeIndentsSimpleClass` — pure-function smoke test.
- `testApplyCardClass` — the original Card-class repro (multi-line primary
  constructor parameter list).
- `testPropertyAccessors` — bare `get() { … } set(v) { … }` with
  continuation + body indent.
- `testPropertyAccessorWithWhenExpression` — accessor body containing a
  multi-arm `when` (real-world case from a user; stresses the step-stack
  through three nested levels).
- `testWhenExpression` — `when` entries plus nested `else -> { … }`.
- `testTryCatchFinally` — `} catch (…) {` / `} finally {` continuation at
  outer level.
- `testLambdaBody` — `list.map { it -> … }`.
- `testAnonymousObject` — `object : I { … }` body.
- `testSecondaryConstructor` — `this(…)` delegation clause.
- `testMultiLineFunctionSignature` — `fun foo(\n…\n): T { … }`.
- `testTripleQuotedStringLeftUntouched` — `"""…"""` interior + closing
  line preserved verbatim while surrounding code is re-indented.

`TestAutoIndent` (Java path, unchanged) passes `null` for the new
`FlowLanguageSupport` parameter to exercise the fallback path — proving
the dispatch hook is bit-identical for Java.

---

## Design decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Token-driven instead of parse-tree-driven | Brace/paren tokens are unambiguous and don't depend on `KotlinPsiScopeBuilder` shape; ends the gap-and-patch loop. |
| 2 | One `instanceof KotlinParsedCUNode` check in `FlowIndent`, no strategy hook | Two impls live in the same package, one delegates to the other — strategy would not earn its keep. Keeps the call sites unchanged and avoids a `null`-means-fallback protocol. |
| 3 | Per-LBRACE step stack (not flat depth counter) | Property accessors need `+2` for their body brace; one flat counter can't express that. The stack also keeps closing-`}` un-indent correct without special-casing. |
| 4 | Capture `inMultilineString` at line start, not finalize time | A line whose first token is the closing `"""` had already cleared the live flag; without capture-at-line-start the closing line would be re-indented and the string corrupted. |
| 5 | `KW_GET` / `KW_SET` only when directly under a class body (`braceTop >= 0 && parenDepth == 0`) | Filters out the rare false positives where `get` appears as a regular identifier (function call); inside method bodies and argument lists no continuation is applied. |
| 6 | Apply pass uses manual line-offset bookkeeping (no `Element`) | Lets `KotlinIndent` be unit-tested with a plain `HoleDocument` and `null` parser — keeps test infra trivial. |
| 7 | Skip `checkMethodSpacing` | Cosmetic and would re-introduce the parse-tree dependency we removed. |

---

## Known limitations

- **Continuation indent is flat (`+1` step) inside `(...)` / `[...]`** — no
  smart alignment to the opening delimiter's column.
- **Property accessors detected only when `get` / `set` is the first
  non-whitespace token on its line.** Inline accessors
  (`var size: Int = 0; get() = field`) are not specialised — they'll be
  re-indented as a regular continuation line.
- **No `checkMethodSpacing` equivalent** (see "intentionally not handled").
