---
id: kotlin-for-bluej
type: architecture-design
title: "Kotlin for BlueJ — Architecture Design"
status: active
parent: goal-and-requirements
depends-on:
  - bluej
tags:
  - kotlin
---

# Kotlin for BlueJ — Architecture Design

> Architecture for adding Kotlin as a third language in BlueJ, targeting an educational MVP subset.

## Overview

Kotlin joins Java and Stride as a teaching language. New Kotlin classes live alongside their Java counterparts within the `bluej/` module (e.g., `KotlinCompiler` next to `CompilerAPICompiler`), mirroring how Stride is integrated.

**Core principle**: Parity with Java experience -- same compile button, same object bench, same class diagram interactions.

---

## Key Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | **Integrated subpackages** over separate module | Follows existing Stride pattern; `lang-stride/` only has 2 files. Keeps all IDE code in `bluej/`. |
| 2 | **Reuse FlowEditor** via `FlowLanguageSupport` strategy | FlowEditor is generic. Language specifics (parser creation, extends/implements syntax) encapsulated in strategy implementations. `JavaSyntaxView` reused unchanged. |
| 3 | **KotlinCompiler extends Compiler** | Peer to `CompilerAPICompiler`, selected by `JobQueue` based on file extension. |
| 4 | **PSI KotlinLexer wrapper** for tokenization | Wraps battle-tested JFlex-generated PSI lexer from `kotlin-compiler-embeddable` (already on classpath). ~280 lines adapter vs ~800 lines hand-written. |
| 5 | **Kotlin stdlib bundled** in distribution | Students must not install a Kotlin SDK. |
| 6 | **JDI reuse** for object bench | Kotlin compiles to standard JVM bytecode. No debugger changes needed. |
| 7 | **KotlinInfoParser** for source-level metadata | Extracts ClassInfo from `.kt` source for class diagram, parallel to Java's `InfoParser`. |
| 8 | **Shared JobQueue** for compilation | Serialized compilation; both Java and Kotlin dispatched through same queue. |
| 9 | **One concept per file** for Kotlin | Each `.kt` file contains either a class/object or top-level functions, never both. Mirrors BlueJ's one-class-per-file Java model. |
| 10 | **Reuse ClassTarget** for top-level function files | Function-only files reuse `ClassTarget` with `KotlinFileRole` rather than a new target type. ~300 lines new vs ~1500+ for dedicated type. |

---

## Component Summary

### Modified Existing Components

- `SourceType` -- add `Kotlin` enum value
- `ClassTarget` -- `.kt` detection, `FlowLanguageSupport` strategy, facade naming for top-level functions
- `ClassInfo` -- add `topLevelFunctionsOnly` flag
- `NewClassDialog` -- Kotlin language radio button + template loading
- `FlowEditor` -- `FlowLanguageSupport` field replaces `boolean isKotlin`
- `JobQueue` -- dual compiler dispatch (Java + Kotlin)
- `Diagnostic` -- add `KOTLIN` DiagnosticOrigin
- `Package` -- Kotlin stdlib on classpath, `.kt` file discovery
- `Boot` -- `"kotlin-*.jar"` patterns for runtime classpath
- `build.gradle` -- `kotlin-compiler-embeddable` and `kotlin-stdlib` dependencies

### New Components

- `KotlinCompiler` -- wraps `kotlin-compiler-embeddable` for K2 compilation
- `KotlinFileFormValidator` -- pre-compilation one-concept-per-file enforcement
- `FlowLanguageSupport` / `JavaLanguageSupport` / `KotlinLanguageSupport` / `PlainTextLanguageSupport` -- strategy interface + implementations
- `KotlinLexer` / `KotlinToken` -- PSI lexer adapter + token mapping
- `KotlinInfoParser` -- PSI-based ClassInfo extraction
- `KotlinEnvironmentManager` -- shared KotlinCoreEnvironment singleton
- `KotlinParsedCUNode` / `KotlinParentNode` / `KotlinCommentNode` / `KotlinStringNode` / `KotlinPsiScopeBuilder` -- parse tree nodes and PSI-to-scope conversion
- `KotlinFileRole` -- `extends ClassRole`, `<<functions>>` stereotype for top-level function files
- Kotlin templates -- `.kt.tmpl` skeleton files for class, interface, open class, abstract class, data class, and Kotlin file

### Unchanged Components (Reused As-Is)

- `HoleDocument`, `FlowEditorPane`, `DocumentUndoStack` -- language-agnostic editor infrastructure
- `FlowErrorManager` -- displays diagnostics from any compiler
- JDI Debugger / Object Bench -- operates on JVM bytecode
- `CompilerThread` / `CompileObserver` -- compiler-agnostic
- `ClassRole` hierarchy (existing roles) -- determined from `ClassInfo` properties, language-agnostic

---

## Threading Model

No new threads. Kotlin uses existing threading:

| Operation | Thread |
|-----------|--------|
| Editing + syntax highlighting | FXPlatform |
| Compilation | CompilerThread (single, serialized) |
| Compilation result dispatch | FX via `Platform.runLater()` |
| Object bench operations | VMEventHandler + FXPlatform |

---

## Backward Compatibility

- `SourceType.Kotlin` is additive -- existing enum values unchanged
- Stride detection takes priority over Kotlin in `calcSourceAvailable()`
- Mixed Java+Kotlin projects not in MVP scope
- Distribution size increases ~30-40 MB (acceptable for educational tool)

---

## MVP Scope

**In scope:** Kotlin compilation (K2), syntax highlighting + scope coloring via PSI, class templates, top-level function files, compiler error display, object bench interaction, Kotlin stdlib bundled, basic code completion.

**Out of scope:** Java-Kotlin interop, Kotlin-specific inspection, lambdas/coroutines in parser, REPL, Greenfoot support, code conversion, Kotlin-specific quick fixes.

---

## Sub-Module Index

| Module | Spec |
|--------|------|
| Compiler | `.specs/kotlin-for-bluej/compiler/README.md` |
| Editor & Syntax | `.specs/kotlin-for-bluej/editor/README.md` |
| Parser & Lexer | `.specs/kotlin-for-bluej/parser/README.md` |
| Class Diagram | `.specs/kotlin-for-bluej/diagram/README.md` |
| Top-Level Functions | `.specs/kotlin-for-bluej/toplevel/README.md` |
| Build & Distribution | `.specs/kotlin-for-bluej/distribution/README.md` |
