---
id: kotlin-for-bluej-editor-completion
type: submodule-design
title: "Kotlin Code Completion"
status: active
parent: kotlin-for-bluej-editor
depends-on:
  - kotlin-for-bluej-parser-analysis
  - bluej-editor-flow
tags:
  - kotlin
---

# Kotlin Code Completion

> What Kotlin code completion suggests, and why. Guiding principle: **match BlueJ's Java completion
> closely**; where Kotlin differs, follow what a Kotlin programmer can actually write.

**Structure:** Ctrl+Space in a Kotlin editor goes to `KotlinCompletionProvider`, which runs
`KotlinCompletionEngine` on [Kotlin Semantic Analysis](../../parser/analysis/README.md) in the background
and turns the result into editor items (`KotlinAssistContent`: popup row, documentation, insertion).
Popup behaviour, request staleness and insertion mechanics follow the shared contract in
`bluej-editor-flow`.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Placeholder identifier inserted at the caret before analysis | Makes incomplete code (`x.` at the end of a line, before more statements) parse as a member access, so the receiver resolves. |
| 2 | Plain completion (nothing before the caret) is curated, not "everything in scope" | Everything visible in Kotlin includes hundreds of default-imported library declarations; Java offers members, locals and a few shortcuts. Offered: locals, parameters, lambda `it`, local functions; members of implicit receivers (`this`, outer classes, companions); Java statics usable unqualified; the project's own top-level functions/properties and extensions applicable to `this`; a teaching set; Greenfoot statics. |
| 3 | No class names, no keywords, no completion in types, imports, packages or callable references | Java completion offers none of these (keyword completion was dropped from the original requirement for this reason). |
| 4 | Library extensions only after a dot | After a dot they are the Kotlin API (`"s".uppercase()`, `list.forEach`); with nothing before the caret they would swamp the list (`let`, `apply`, ... apply to `this`). |
| 5 | Teaching set `println`, `print`, `readln`, `listOf`, `mutableListOf`, `repeat`, `maxOf`, `minOf` | Replaces Java's `System.out` shortcut. Only the top-level functions, not same-named extensions; among overloads with the same parameter names, only the most general (`println(Any?)`, not twelve). |
| 6 | `Greenfoot.name` statics when `greenfoot.*` or `greenfoot.Greenfoot` is imported | Same rule as Java, checked on the import directives instead of a regex. |
| 7 | Java getters appear only as Kotlin properties (`world`, `rotation`, `isAtEdge`) | Idiomatic Kotlin. The setter matching the property's type is hidden too (assign the property instead); other overloads stay (`setImage(String)`, since `image = "a.png"` would not compile). Static getters stay methods. |
| 8 | Extensions hidden by members are not offered; `@HidesMembers` extensions hide members | Mirrors Kotlin resolution: a member wins over an extension of the same shape (name and parameter count), except where Kotlin prefers the extension (`forEach`). |
| 9 | After `ClassName.`: statics, companion/object members and enum entries, without `Any` members | Java's static-only completion; `Rock.toString()` would not mean what it looks like. |
| 10 | A member property hidden by a local is offered as `this.name` | Same as Java. Only for the innermost receiver; hidden properties of outer receivers are omitted. |
| 11 | Deprecated declarations (Kotlin or Java annotation) are never offered | Decided for students; Java completion does not filter them. |
| 12 | Types are shown with short names; Java platform types without `!` | `World` reads better than `World!` for students. |
| 13 | Candidates are plain data, captured on the analysis thread | The analysis is invalid once the task ends. KDoc text and, for Java methods, the JVM signature for Javadoc lookup are captured up front. |
| 14 | Java members documented through the project's `JavadocResolver`, batched per declaring class | Exactly Java completion's mechanism, so Greenfoot API documentation and real parameter names (instead of `p0`) come from the same place. |
| 15 | KDoc converted to Javadoc form and rendered by the existing Javadoc display | KDoc block tags (`@param`, `@return`) match Javadoc; only HTML escaping and common Markdown (`` `code` ``, links, `[references]`, `**bold**`) need converting. One renderer, one look. |
| 16 | Popup rows and the documentation header use Kotlin syntax: `roll(steps: Int, fast: Boolean): Int`, `world: World` | Shows what the student would write. The shared popup's separate type column (Java's `int move(int)` layout) is left empty, which collapses it; Java and Stride rows are unchanged. Parameters without a known name show just their type; default values are marked `= ...` only in the documentation, to keep rows short. Trade-off: a row too wide for the popup is truncated from the end, so the return type is lost first. |
| 17 | Calls insert placeholders for required parameters only | Kotlin defaults and varargs make other arguments optional; Java-style `_name_` placeholders (type-based when no name is known), first one selected. Named arguments are not used. |
| 18 | A required function-typed last parameter becomes a trailing lambda | `repeat(_times_) { }`, `forEach { }` with the caret inside -- how Kotlin is written. An optional last lambda is simply left out. |
| 19 | Analysis in the background; the popup appears when ready, if the request is still current | Keeps the editor responsive during the first request's ~1s start-up. Stale results are dropped by the shared request contract, which relies on the completion shortcut itself not typing into the document (see `bluej-editor-flow` Key Contracts). |
| 20 | Messages instead of silence: "Loading code completion..." after 300ms, "No completions available" when nothing can be completed, "Code completion failed" (also logged), and a timeout message after 5s | Students should know whether to wait. Unlike Java, which shows nothing when there is nothing to complete. A superseded request shows nothing. |
| 21 | Timers are FX `Timeline`s, the analysis runs on the shared pool | No new threads, as the Kotlin architecture requires. |
| 22 | Sorted as Java does: Greenfoot shortcuts first, then by name, kind and parameters | Java puts its static shortcuts first and sorts the rest with `AssistContent.getComparator()`. |

---

## Contracts

- `KotlinCompletionEngine.complete` takes the current editor text and caret. Its result is **null** when
  there is nothing to complete (comment, string, type, import, declaration name, or a receiver whose type
  cannot be resolved) and otherwise holds the start of the typed identifier and the candidates.
- Candidates are unsorted; ordering is the caller's job. A declaration reachable in several ways (e.g. an
  `Any` member through both `this` and the companion) appears once, the first way found winning.
- Locals are collected before members so that properties they hide are recognised.
- Visibility is checked from the caret's scope; private members of other classes never appear.

---

## Key Lifecycles

- **Result and candidates**: created by one completion request on the analysis thread, immutable, and
  handed back to the requester; they own no PSI or descriptors and are dropped with the popup. Nothing is
  cached between requests.
- **Editor items**: created from a result on the FX thread by `KotlinAssistContent.create`, which performs
  the Javadoc lookups (as Java completion does on the FX thread); they live as long as their popup.
- **Provider and request**: `KotlinLanguageSupport` shares one stateless `KotlinCompletionProvider`. Each
  Ctrl+Space snapshots the project (`KotlinAnalysisRequest.forProject`, with the editor's current text)
  and submits it to the project's analysis service. Exactly one outcome is shown -- result, message or
  timeout, whichever comes first; the loading message and timeout timers are cancelled when the outcome
  arrives. A timed-out analysis still completes in the background and its result is discarded.

---

## Known Limitations

- **Javadoc needs a loadable class** -- Java members of uncompiled project classes get no Javadoc, and
  their compiled-library counterparts without documentation keep type-based placeholders (`_Int_`),
  which, as in Java, are not always valid identifiers (`_List<T>_`).
- **KDoc Markdown is converted only partly** -- lists, headings and code blocks show as plain text.
- **Approximations**: hidden extensions are detected by name and parameter count; generic extensions are
  matched by star-projecting their own type parameters; "most general overload" compares parameter types
  pairwise. These can occasionally keep or drop a borderline suggestion.
- **Not completed**: `super.` members, labels (`this@Outer`), named arguments, hidden properties of outer
  receivers.
- **No automated end-to-end test in a live project** -- request snapshotting from a real `Project` and
  the popup itself are checked manually; the provider's outcome handling, request staleness, items and
  engine are tested separately.
- **Performance** (student-sized project, real Greenfoot classes): about 1.1s for the session's first
  request, then roughly 25ms analysis plus up to 50ms collecting candidates.
