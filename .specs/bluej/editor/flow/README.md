---
id: bluej-editor-flow
type: submodule-design
title: "Flow Text Editor"
status: active
parent: bluej-editor
---

# Flow Text Editor

The traditional text-based Java editor. Implements the `TextEditor` interface with a custom document model (gap buffer), undo/redo, syntax and scope coloring, find/replace, and incremental parser integration.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `FlowEditor` | Main text editor; extends `ScopeColorsBorderPane`, implements `TextEditor` |
| `FlowEditorPane` | Custom text rendering pane (extends `BaseEditorPane`) -- NOT JavaFX TextArea |
| `HoleDocument` | Gap-buffer document with change tracking and tracked positions |
| `DocumentUndoStack` | Undo/redo with explicit compound edit grouping |
| `JavaSyntaxView` | Maps parser scope tree to syntax CSS classes and scope background colors; implements `NodeStructureListener` |
| `FlowErrorManager` | Compiler diagnostic display with squiggly underlines and quick-fix integration |
| `FlowActions` | Editor action definitions with key binding management |
| `FindPanel` / `FindNavigator` | Find/replace UI and navigation logic |
| `FlowIndent` | Auto-indentation; default tree-walking implementation for Java + dispatch hook for `FlowLanguageSupport.calculateIndents` (Kotlin) |
| `KotlinIndent` | Token-driven Kotlin auto-indenter; no parse-tree dependency |
| `CompletionProvider` | Per-language code completion strategy (see [Code Completion](#code-completion)); `JavaCompletionProvider` is the Java implementation |
| `CompletionRequest` / `CompletionResult` / `CompletionInsertion` | The completion contract's types: the editor's callback for one Ctrl+Space (staleness, result, messages), a provider's candidates with their replace-start, and the segments a chosen candidate inserts |
| `TrackedPosition` | Caret/anchor positions that auto-adjust on text edits; has `Bias` (FORWARD/NONE) |

---

## Key Contracts

- `FlowEditor` is both a JavaFX UI component AND the `TextEditor` implementation
- `assumeText()` returns `this`; `assumeFrame()` returns `null`
- `HoleDocument` uses a gap buffer: O(1) amortized sequential edits, O(n) random-access edits
- A document replacement creates one undo unit unless explicitly grouped in a compound edit; there is no timing/contiguity-based merge in `DocumentUndoStack`
- Compound undo groups history only: document listeners still see individual replacements and exceptions do not roll them back. Coordinated Kotlin preparation changes therefore use one enclosing replacement (see `kotlin-for-bluej-editor`).
- Only visible lines are tokenized and styled (lazy `StyledLines` AbstractList)
- Deferred scope repaint requests are checked against the current document line count after layout;
  undo/deletion may remove a queued line before it is processed. Such requests are discarded rather than
  indexing stale lines. This does not change undo grouping or document notification behavior.
- A key press bound to an editor action never also types its character. The key-typed event that
  follows a bound key press is dropped; the suppression ends with that key's release (or the next key
  press the actions do not handle), so text arriving as a typed event with no key press of its own --
  the macOS accent picker, some input methods -- is never swallowed by an earlier shortcut. Otherwise
  bindings on keys that type something -- Option+key on macOS (Option+Space types a non-breaking
  space), Shift+key -- would edit the document under the action; for example a user-bound Option+Space
  completion inserted a space that made the background Kotlin request stale.
- Insert Method follows the editor's source language; Kotlin template/fallback behavior is owned by
  `kotlin-for-bluej-editor`.

---

## Scope Coloring

`JavaSyntaxView` acts as a `NodeStructureListener` on the parser's scope tree:
1. Parser updates `ParsedNode` tree
2. `getBackgrounds(lineIndex)` walks the tree to find overlapping scopes
3. Creates `BackgroundItem` nodes with scope colors from `ScopeColors` palette
4. `FlowEditorPane` distributes backgrounds to visible lines

---

## Auto-Indent Dispatch

`FlowIndent.calculateIndentsAndApply` checks the parser root type with one
`instanceof` and routes Kotlin sources to `KotlinIndent`; everything else
falls through to the default tree-walking implementation that walks
`NodeAndPosition<ParsedNode>` and `JavaParentNode.isSwitchBlockNode()`.

| Parser root | Path |
|---|---|
| `KotlinParsedCUNode` | `KotlinIndent.calculateIndentsAndApply(...)` — token-driven; no parse-tree read; multi-line strings flagged `LEAVE_UNTOUCHED` |
| Anything else (incl. `null`) | Tree-walking via `IndentCalculator` / `NodeIndentCalculator`, including `checkMethodSpacing` |

Java behavior is bit-identical to the pre-feature implementation (verified
by `TestAutoIndent` passing unchanged).

---

## Code Completion

Ctrl+Space (the `code-completion` action, the only trigger) asks the editor's `FlowLanguageSupport` for
a `CompletionProvider`. The provider decides what to suggest, how each popup row looks and what
choosing a row inserts. FlowEditor owns everything else: the `SuggestionList` popup, prefix filtering
while typing, the insertion itself, Blackbox recording and save-after-insert. Keeping that in one place
gives every language the same popup behaviour; Kotlin is meant to match Java closely.

Contracts:
- Each Ctrl+Space hands the provider a `CompletionRequest`. The provider shows a result through it at
  most once, on the FX thread (Java does so synchronously; Kotlin later, from a background analysis),
  or a short caret-anchored message (e.g. nothing to complete). While still working it may show a
  progress message (e.g. still loading), which stays up until the outcome replaces it.
- A request stops being current when the document changes, the caret moves or completion is requested
  again; its late results and messages are then ignored. This is what makes background providers safe:
  the editor never shows a popup for text that is no longer there.
- Showing nothing means "nothing to complete" (Java's long-standing behaviour when no expression type
  is found). An empty result still shows an empty popup, as before.
- Candidates are opaque to the editor: it hands them back to the provider that produced them for
  display and insertion.
- An insertion is a list of segments applied as separate document edits, so Java keeps its existing
  undo granularity (`name(`, parameters and `)` undo separately; see the undo contract above).

### Key Lifecycles

- **Provider creation**: `JavaLanguageSupport` and `KotlinLanguageSupport` each hold one stateless,
  shared provider (`JavaCompletionProvider`, `KotlinCompletionProvider`, the latter specified by
  `kotlin-for-bluej-editor-completion`); plain text returns null and does nothing.
  `getCompletionProvider()` is called only from `FlowEditor.createContentAssist`.
- **Request and result**: every Ctrl+Space makes a fresh numbered request (superseding the previous one)
  and hides any completion message; a result that is still current makes a fresh popup. Nothing is
  cached between presses. A result lives only as long as its popup, which dismisses itself if the caret
  moves before the replace-start position. At most one message is up at a time; it is hidden by the
  next request, result or message and by anything that makes its request stale (a document change, a
  caret move). A plain message also dismisses itself after 3 s; a progress message has no timer, so
  it cannot vanish before the outcome it announces.
- **Destruction**: the editor keeps no reference to results after the popup closes, and providers hold
  no per-editor state, so there is nothing to dispose.

Limitations: no automatic popup after `.`; Java completion is computed on the FX thread.
Staleness is judged by document changes and caret position, so an edit that is undone still makes a request stale.

---

## Design Decisions

- **Custom rendering over TextArea**: Needed for scope backgrounds, inline error squiggles, and per-token syntax highlighting
- **Gap buffer**: O(1) amortized sequential editing (the dominant typing pattern)
- **English message matching for fixes**: javac doesn't expose stable error codes; `DiagnosticMessage` provides both localized and English messages
- **Lazy line styling**: Avoids syntax-highlighting off-screen lines
- **CSS-driven syntax colors**: Allows theme customization through stylesheets

---

## Dependencies

Uses: `editor/base/` (rendering), `parser/` (incremental parsing, scope tree), `editor/fixes/` (quick fixes)
