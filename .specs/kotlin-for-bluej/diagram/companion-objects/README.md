---
id: kotlin-for-bluej-diagram-companion-objects
type: submodule-design
title: "Companion Object Methods as Static Operations"
status: active
parent: kotlin-for-bluej-diagram
tags:
  - kotlin
---

# Companion Object Methods as Static Operations

> Invoking Kotlin `companion object` methods from the class diagram as if they were static methods of the enclosing class.

**Parent spec:** [Class Diagram Integration](../README.md)

---

## Problem

A method declared in a Kotlin `companion object` is **not** a JVM static method. Kotlin
compiles it to an *instance* method on a synthetic nested class `Foo$Companion`, reached only
through the public static field `Foo.Companion`. The only true static is the one Kotlin emits
for a `@JvmStatic`-annotated companion member.

BlueJ builds the class-diagram menu purely by reflection over the enclosing class's declared
methods, filtered to JVM statics. A plain companion method is therefore neither enumerated (it
lives on a different class) nor admitted by the static filter — so students could not call it.

## Contract

A companion object's methods are surfaced as **static-style operations on the enclosing
class** — there is no separate diagram node and no `«companion»` stereotype; the companion
itself stays invisible, exactly as a `@JvmStatic` companion method already appears today. The
invocation a user triggers runs as `Foo.<companionName>.method(args)`, using the companion's
actual name (`Companion` by default, or a custom name). This mirrors `KotlinFileRole`'s
"inherit static methods" contract for top-level functions ([toplevel](../../toplevel/README.md)).

Detection reuses the `@kotlin.Metadata` mechanism and graceful-degradation contract of
`KotlinPropertyAccessorDetector` (parser/reflection metadata is the single source of truth for
"is this Kotlin, and what is its companion"); on any parse failure the class is treated as
having no companion.

---

## Key Lifecycles — companion-backed static operation

A *companion-backed static operation* is a method-menu entry whose target is a companion
instance method. Tracing every path that creates, filters, and invokes it:

1. **Creation** — Created in exactly one place: the enclosing class's reflective method
   enumeration appends the companion's public, non-synthetic, non-`Object` declared methods,
   each flagged with the companion receiver name, and the result is cached on the class view.
   No other path produces a companion-flagged entry — object-bench entries come from instance
   enumeration and are never flagged. `@JvmStatic` twins are de-duplicated by call signature so
   the real static is kept and the companion duplicate dropped (otherwise the member appears
   twice).
2. **Filtering** — A companion method reports `isStatic() == true` (it is callable in a static
   context), so the static/instance menu filter — which keys off `isStatic()` — lands companion
   entries in the class's static menu and never on object-bench instance menus. Visibility is
   judged from the companion method's real modifiers via `getModifiers()`.
3. **Invocation** — Reached only through the static-invocation path (no bench receiver). The
   call-string builder prefixes the companion receiver name before the method name. Result
   handling (void / generic return / invoker record) is unchanged because it keys off the
   method entry, not the receiver form. `MethodView.isStatic()` returns true for companion
   methods (callable in a static context) even though their reflected `Method` is a *non-static*
   instance method on `Foo$Companion`, so the surrounding surfaces need no companion-specific
   branch — but two were latent gaps:
   - The call **dialog header** keys off `isStatic()` to label the call `Foo.method(...)`; before
     `isStatic()` reported true for companions it fell through to the instance branch and
     rendered `null.method(...)` (the instance name is null for a class-diagram invocation).
   - **Parameter names** come from a `CommentEntry` matched by signature. Two things are needed:
     `KotlinInfoParser` must recurse into the companion `object` when walking the class body
     (else no comment is emitted at all for companion methods); and, because companion methods
     are commonly expression-bodied with an *inferred* return type, the emitted `void name(...)`
     target only matches the reflected `<type> name(...)` via the **return-type-agnostic
     fallback** in `View.loadClassComments` (see [parser spec](../../parser/README.md#callable-parameter-names)).
     Both the `void` guess and the fallback become unnecessary once the Kotlin Analysis API
     resolves inferred return types.
4. **Destruction** — Lifetime is the cached class view; evicted with the view on recompile or
   classloader swap. No independent references survive.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | No separate diagram node — methods join the class's static menu | Consistency: a `@JvmStatic` companion method already shows there; companion-as-classifier is not a CS1 concept and needs no new rendering |
| 2 | De-dup `@JvmStatic` twins by call signature, keep the real static | A `@JvmStatic` member yields both a class static and a companion instance method; without dedup it appears twice |
| 3 | Support named companions (`companion object Factory`) | The companion name is available from metadata for free; the receiver becomes `Foo.Factory.method()` |
| 4 | `MethodView.isStatic()` reports true for companion methods (rather than a separate "callable without instance" predicate) | Every `isStatic()` reader in the view layer wants "callable in a static context", which is exactly what a companion method is; folding it in makes the intuitive check correct-by-default (the dialog-header bug came from `isStatic()` *not* covering companions). Modifier *display* and truthful-static readers use `getModifiers()`, which is untouched. The one cost: `Invoker` must test `isKotlinCompanionMethod()` before `isStatic()` (comment marks this) |
| 5 | Include companion property accessors | The gap was "call companion members"; accessors reuse the existing auto-generated labelling and need no extra code |
| 6 | Declared companion members only | MVP scope; inherited companion supertype members are excluded |

---

## Acceptance Criteria

- A `.kt` class with `companion object { fun bar() = 42 }` shows `bar()` in the class's
  right-click menu; invoking it executes `Foo.Companion.bar()` and returns `42` on the bench.
- A `@JvmStatic` companion method appears exactly once (as the real static).
- A named companion (`companion object Factory`) invokes as `Foo.Factory.method()`.
- Companion methods do **not** appear on object-bench instance menus.

---

## Known Limitations

- **Top-level `object` declarations** (singletons, invoked via `Foo.INSTANCE`) are not surfaced —
  only companion objects of a class.
- **No companion field inspection** — companion properties are reachable only through their
  accessor methods, not via the object inspector.
- **Inherited companion members** (companion extends a class / implements an interface) are not
  surfaced — declared members only.
- **Companion property accessors show no parameter names** — they are compiler-synthesized
  (`getX`/`setX`), not source `fun` declarations, so there is no KDoc/source param to attach.
  Identical to how ordinary Kotlin property accessors already behave.
