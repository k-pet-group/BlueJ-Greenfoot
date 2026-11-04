---
id: kotlin-for-bluej-compiler
type: module-design
title: "Kotlin Compiler Integration"
status: active
parent: kotlin-for-bluej
depends-on:
  - bluej-compiler
  - boot
tags:
  - kotlin
---

# Kotlin Compiler Integration

> Programmatic Kotlin compilation within BlueJ by wrapping `kotlin-compiler-embeddable` (K2) behind the existing `Compiler` interface.

**Parent spec:** [Kotlin for BlueJ -- Architecture Design](../DESIGN_DOC.md)

---

## Architecture

1. **Dispatch** -- The shared queue routes `.kt` inputs to `KotlinCompiler`, everything else to javac. A mixed request passes its selected Java symbol sources directly to the K2 invocation, allowing Kotlin-to-Java references before Java bytecode exists. K2 reads but does not emit Java bytecode; javac owns Java bytecode and diagnostics. Request isolation, ordering, failure behavior and shared output lifetime are owned by `bluej-compiler`.
2. **Pipeline** (inside `KotlinCompiler.compile()`) -- Validate file structure via `KotlinFileFormValidator` -> configure K2 arguments (Kotlin sources plus any Java symbol sources in `freeArgs`) -> invoke `K2JVMCompiler.exec()` in-process -> collect diagnostics via `MessageCollector` (dropping any whose path ends in `.java` -- javac is the authoritative source for those) -> map to BlueJ `Diagnostic` -> notify `CompileObserver`
3. **Classpath** -- Boot supplies stdlib on both BlueJ and Greenfoot's student classpaths; `Job.compile()` passes it to `KotlinCompiler` via `setClasspath()`. Compiler/PSI/metadata dependencies remain IDE-only; see [Build & Distribution](../distribution/README.md).

No new threads. Both internal stages execute in one request on the existing `CompilerThread`; there is
no intermediate observer completion.

---

## Key Contracts

- `KotlinCompiler extends Compiler` -- peer to `CompilerAPICompiler`
- Both BlueJ and Greenfoot construct the Kotlin backend; extension routing is the same in each IDE. Internal Java invocation shells still use javac.
- K2JVMCompiler instantiated per `compile()` call (stateless)
- `Diagnostic.DiagnosticOrigin.KOTLIN` added for tagging
- For `.kt` files, `CompileInputFile` both fields point to same `.kt` file (no Java intermediary)

### Diagnostic Mapping

| Kotlin Severity | BlueJ Diagnostic |
|-----------------|-----------------|
| ERROR, EXCEPTION | `Diagnostic.ERROR` |
| WARNING, STRONG_WARNING | `Diagnostic.WARNING` |
| INFO | `Diagnostic.NOTE` |
| LOGGING, OUTPUT | Filtered |

Line/column from `CompilerMessageSourceLocation` mapped 1:1. Origin = `KOTLIN`. Identifier from shared atomic counter.

---

## Submodules

| Submodule | Description |
|-----------|-------------|
| [File Form Validation](file-form-validation/README.md) | Pre-compile one-concept-per-file enforcement |

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | K2JVMCompiler in-process (not subprocess) | Avoids JVM startup overhead; simpler for small files, frequent compiles |
| 2 | File-extension dispatch within one queued request | No signature change to `addJob()`; callers keep source-selection responsibility |
| 3 | Shared atomic identifier counter | Diagnostic IDs globally unique across Java and Kotlin |
| 4 | One `KotlinCompiler` instance per `JobQueue` | Mirrors `CompilerAPICompiler` lifecycle; K2 is stateless per invocation |

---

## Key Lifecycles

The queue owns a reusable Kotlin backend, configured only by its compiler thread. Each invocation
validates file form and creates a fresh K2 compiler/collector. Queued Java symbol inputs and output
location belong to that request, not mutable next-invocation state on the backend. The coordinator
owns queued output cleanup; direct public backend calls still own their own check-only cleanup.
The compatibility `setJavaSymbolSources` API is for same-thread direct backend callers only: its copied
inputs are consumed and cleared at the next public compile call, including unsuccessful attempts.

## Known Limitations

- **No per-class language mixing** -- each class is wholly Java OR wholly Kotlin. Shared mixed-request coordination is implemented, but source-selection correctness is deferred until Kotlin Analysis API integration; no whole-project workaround or new language-policy rejection is added. This is not certification of fully reliable mixed projects. See `bluej-compiler` for the temporary delayed-Java-diagnostic limitation and `bluej-pkgmgr` for the temporary mixed-recovery limitation, including their removal requirements.
- **K2 cold-start latency** -- first compile ~2-5s due to class loading
- **No incremental compilation** -- full K2 on all `.kt` sources each time
- **Full Kotlin language accepted** -- K2 doesn't restrict to educational subset
