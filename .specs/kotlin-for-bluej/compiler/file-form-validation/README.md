# Kotlin File Form Validation

> Pre-compilation validation enforcing BlueJ's one-concept-per-file model with hard errors that mirror javac's file restrictions.

**Parent spec:** [Kotlin Compiler Integration](../README.md)

---

## Why This Exists

Kotlin's compiler is permissive -- it allows multiple classes per file, mismatched names, and mixing classes with top-level functions. Without explicit validation, students get no indication their file structure violates BlueJ's educational model. Extra declarations are silently ignored by `KotlinInfoParser`.

This adds a **pre-compile validation pass** inside `KotlinCompiler.compile()` that reports violations as hard errors through the standard `CompileObserver` pipeline.

---

## Rules

A `.kt` file must contain exactly one of:
- **Class file**: exactly one class/interface/object/enum/data/sealed/abstract class. No top-level functions or properties.
- **Functions file**: one or more top-level functions and/or properties. No class/object declarations.

### Violations

| # | Condition | Error Message |
|---|-----------|---------------|
| V1 | Two+ `KtClassOrObject` at top level | `"Only one class or object declaration is allowed per file. Found: Foo, Bar"` |
| V2 | Single class whose name != file stem | `"Class 'Dog' should be declared in a file named 'Dog.kt'"` |
| V3 | Class + top-level functions | `"A file with a class declaration cannot also contain top-level functions. Move functions to a separate file or inside the class."` |
| V4 | Class + top-level properties | `"A file with a class declaration cannot also contain top-level properties. Move properties to a separate file or inside the class."` |

### Not Validated (Allowed)

Nested classes, companion objects, `const val` in functions files, type aliases, multiple top-level functions, standalone object declarations.

---

## Algorithm

Runs as pre-compile pass before `invokeCompiler()`:
1. For each source file: parse with `KtPsiFactory.createFile()`, count `KtClassOrObject`, `KtNamedFunction`, `KtProperty`
2. Check V1-V4; create `Diagnostic(ERROR)` for each violation, positioned at the offending keyword
3. If any violations found -> `return false` (skip K2)

All files validated before reporting (not fail-fast on first). Diagnostics point to the offending declaration keyword for actionable feedback.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | Pre-compile in `KotlinCompiler.compile()` | Errors appear in same error list as compiler errors; natural student workflow |
| 2 | Hard errors (not warnings) | Matches Java's `javac` behavior; students must fix before proceeding |
| 3 | Separate `KotlinFileFormValidator` utility class | Testable in isolation; ~100-130 lines |
| 4 | PSI-based analysis | Correctly handles nested classes, companion objects, string literals containing `class`, etc. |
| 5 | Skip K2 entirely on violation | Keeps error list focused on structural problem |
| 6 | Position at offending declaration | Helps students understand exactly which declaration needs to move |

---

## Known Limitations

- **Validation requires PSI parse** -- each file parsed twice (validation + K2). Negligible for small files.
- **No editor-time feedback** -- violations only at compile time (consistent with Java behavior)
- **English-only error messages** -- localisation via `DiagnosticMessage.fromLocalised()` possible later
