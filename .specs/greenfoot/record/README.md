---
id: greenfoot-record
type: submodule-design
title: "Greenfoot Recorder"
status: active
parent: greenfoot
depends-on: [bluej-editor, kotlin-for-bluej-editor, bluej-views]
---

# Greenfoot Recorder

"Save the World" recording system. Captures interactive setup as structured operations and renders source that can reproduce the world state.

---

## Key Class

`GreenfootRecorder` owns recording state and replay names. `RecordedAction` keeps construction, placement, invocation, movement and removal data separate from Java/Stride or Kotlin rendering.

---

## Contracts

- Objects named using deterministic numbering: `crab`, `crab2`, `crab3`, etc. Tracked via `HashMap<DebuggerObject, String>`.
- `validToSave` is **invalidated** by Act/Run (unpredictable state changes) and **set** by world creation/reset (clean starting state).

---

## Key Lifecycles

Interactive construction, placement, movement, removal and instance/static invocations append immutable
operations. Argument and parameter arrays are copied at capture, before callers can reuse them. Replay
naming reconstructs the object-name map without adding recorded operations; removing an actor removes
its mapping but retains the recorded removal. Clearing a recording discards operations and optionally
the name map; successful saves retain names for continued recording. Act/Run eligibility remains
separate from these operations and is revalidated by world creation/reset.

### Save acknowledgement

A save captures an immutable action prefix, World class and recording generation under the recorder
monitor. Rendering, editor access and completion callbacks run outside that monitor. Only one source
insertion may be pending per recorder, so overlapping requests cannot insert the same prefix twice;
newly captured actions remain available for the next save.

Clear/reset or a different World invalidates the generation. Completion consumes only the acknowledged
prefix of the still-current generation, never later captures or a replacement recording. Failed edits
retain the recording; superseded/overlapping requests are cancelled without triggering compilation.
An already-dispatched legacy edit can still finish in its old editor after a reset, but its completion
cannot consume the new recording or authorize a new user compilation.

Kotlin uses the current editor buffer, not disk text or reflected source. A coordinated PSI plan is
applied as one normal document replacement after checking destination identity, writability and source
snapshot equality. Existing document undo/dirty listeners and automatic error-check scheduling remain
active. Java/Stride retain their existing placement rules and complete both insertions before reporting
success. Source editing is not a rollback transaction: exceptions can leave partial changes, and a
Stride loading failure before frame availability still has no completion signal. Insertion success
acknowledges neither saving to disk nor successful compilation; the UI handles the subsequent user
compilation (see `greenfoot-guifx`).

---

## Kotlin replay contract

Kotlin source insertion uses the bounded structural planner described by `kotlin-for-bluej-editor`.

Kotlin replay must generate native declarations and calls from structured recorded operations while
preserving argument expression strings unchanged. Class/receiver/method information, generated names,
and arguments stay separate from emitted source; a preassembled Java constructor-call string is not the
language-independent recording format.

Static operations declared by a metadata-confirmed Kotlin file facade render as package-level
references, not calls on the generated JVM container. Named packages are qualified and escaped;
default-package declarations are unqualified. Property access retains the declared-name mapping in
`bluej-views`; ordinary functions retain their reflected names. Java/Stride rendering and real
class/companion receivers are unchanged. Facade identity is not inferred from a `Kt` name suffix.

Invocation remains Java-based. Successful invocation means an expression was accepted in a Java helper,
not that the same text is valid or equivalent in Kotlin. The supported replay arguments are therefore
limited to expressions that already work with the same meaning in both languages. No argument parser,
Java-to-Kotlin expression converter, Kotlin expression evaluator, or new argument-rejection policy is
part of this boundary. Structural parsing of the World source for insertion is a separate concern.

### Limitations and deferred compatibility work

- Java-only nested construction, array initializers and casts are not translated. Their copied text may
  fail normal Kotlin compilation; inserting source successfully is not a claim that it compiles.
- Shared spelling does not guarantee shared meaning: string interpolation, equality and numeric typing
  can differ. These cases are outside the pass-through guarantee even if the output compiles.
- Live invocation-object names are not a general persistent-reference format; arbitrary reference
  reconstruction is not promised by preserving argument text.
- A recorded actor local can shadow a class/companion receiver with the same name (for example a
  lowercase class). Such companion calls may fail Kotlin compilation even with otherwise compatible
  arguments. This generated-name collision is deferred separately from expression compatibility:
  no import aliases, actor renaming, operation reordering or rejection guard is introduced to hide it.
- Current-World property accesses remain unqualified. A generated actor local such as `score` for a
  `Score` actor can hide the World's `score` property: setters can produce reassignment/type errors and
  getters can read the wrong value. Explicit World qualification is deferred; name-safe replay is not
  guaranteed even for ordinary student identifiers.
- Top-level replay does not disambiguate source names. A same-named World member or replay local can
  hide an unqualified top-level function/property; a local/member can also hide a package prefix.
  The result may fail compilation or compile while targeting the wrong declaration. This limitation
  is explicitly retained to keep the correction bounded to unambiguous replay: no import aliases,
  student/generated-name changes or rejection guard are introduced. Removing it requires separately
  agreed source-aware disambiguation, not merely removing the JVM facade receiver.
- Non-property functions use their reflected JVM name. Functions renamed with `@JvmName` can therefore
  produce unresolved references in Kotlin replay. Declared function-name recovery is outside the
  current beginner-focused scope; the separate metadata-signature mapping for property accessors remains.

This deliberately separates statement generation from expression-language support. Broader expression
conversion or Kotlin-native invocation, and live-reference reconstruction, remain deferred compatibility
work to revisit explicitly before claiming general argument support. They must not be silently inferred
from the ability to compile and invoke Kotlin classes.

---

## Dependencies

Uses: `core/` (GreenfootMain, WorldHandler), Stride code elements for Java/Stride output, and Kotlin parser/metadata support through BlueJ. No compiler dependencies are added to the student classpath.
