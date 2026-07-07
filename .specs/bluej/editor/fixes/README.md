---
id: bluej-editor-fixes
type: submodule-design
title: "Quick Fixes"
status: active
parent: bluej-editor
---

# Quick Fixes

Code fix suggestions triggered by compiler diagnostics (8 files). Provides "did you mean?" corrections, import suggestions, and error-specific quick fixes. Also contains the code completion popup (`SuggestionList`).

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `EditorFixesManager` | Per-editor service providing import scanning, type lookups, and `FixSuggestionBase` wrapper |
| `FixSuggestion` (abstract) | `getDescription()` (thread-safe) + `execute()` (FX platform thread) |
| `Correction` | Edit-distance-based "did you mean?" corrections using Damerau-Levenshtein (max distance 2, max 3 suggestions) |
| `FixDisplayManager` (abstract) | UI layer for error+fix display. Subclassed by FlowEditor and Stride |
| `ProjectImportInformation` | Per-project import catalog: pre-scans popular/rare packages for suggestion candidates |
| `SuggestionList` | Code completion popup: transparent Stage with filterable, navigable suggestion list |

---

## Diagnostic-to-Fix Matching

`EditorFixesManager` provides infrastructure but does **not** contain matching logic. Actual matching lives in:
- **Java text editor**: `FlowErrorManager.ErrorDetails.calculateCorrections()` -- sequential if-else chain on **English diagnostic text** (not javac error codes)
- **Stride frame editor**: Error classes in `bluej.stride.framedjava.errors`

---

## Available Quick Fixes

| Error Pattern | Fixes |
|---------------|-------|
| Unknown class | Import class, import package, correct to similar type |
| Wrong comparison (`=` instead of `==`) | Replace `=` with `==` |
| Undeclared variable | Correct to similar, declare local, declare field |
| Undeclared method | Correct to similar method name |
| Unreported exception | Surround with try/catch, add throws clause |

---

## Import Suggestion Tiers

- **Popular** (shown immediately): `java.io`, `java.math`, `java.time`, `java.util`, `java.util.function`, `java.util.stream` (+ `greenfoot` in Greenfoot mode)
- **Rarer** (shown after Ctrl+Space): `java.awt`, `java.nio.file`, `javafx.*`, `javax.swing`, etc.

---

## Design Decisions

- **English message matching**: javac doesn't expose stable machine-readable error codes. `DiagnosticMessage` maintains both `localisedMessage()` and `englishMessage()`.
- **Code completion is separate from quick fixes**: `SuggestionList` and `FixDisplayManager` are distinct systems with different UIs and data sources.
- **Async correction calculation**: Import scanning and edit-distance computation run on background threads.
