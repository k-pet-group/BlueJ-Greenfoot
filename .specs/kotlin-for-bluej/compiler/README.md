# Kotlin Compiler Integration

> Programmatic Kotlin compilation within BlueJ by wrapping `kotlin-compiler-embeddable` (K2) behind the existing `Compiler` interface.

**Parent spec:** [Kotlin for BlueJ -- Architecture Design](../DESIGN_DOC.md)

---

## Architecture

1. **Dispatch** -- `JobQueue.addJob` partitions sources by extension. `.kt` files go to `KotlinCompiler`, everything else to `CompilerAPICompiler`. In a mixed-language package both jobs are submitted: Kotlin first (so its `.class` output is on javac's classpath via `projectDir`), then Java. The `.java` sources are also handed to K2 via `KotlinCompiler.setJavaSymbolSources` so K2 can resolve Kotlin->Java references without their bytecode existing yet. K2 reads but does not emit for `.java`; the second job produces the authoritative bytecode and diagnostics.
2. **Pipeline** (inside `KotlinCompiler.compile()`) -- Validate file structure via `KotlinFileFormValidator` -> configure K2 arguments (Kotlin sources plus any Java symbol sources in `freeArgs`) -> invoke `K2JVMCompiler.exec()` in-process -> collect diagnostics via `MessageCollector` (dropping any whose path ends in `.java` -- javac is the authoritative source for those) -> map to BlueJ `Diagnostic` -> notify `CompileObserver`
3. **Classpath** -- `Boot.java` includes `kotlin-stdlib-*.jar` in `bluejUserJars`; `Job.compile()` passes it to `KotlinCompiler` via `setClasspath()`

No new threads. Runs on existing `CompilerThread`, preserving single-threaded serialization. Mixed-language packages produce two sequential jobs on the same thread.

---

## Key Contracts

- `KotlinCompiler extends Compiler` -- peer to `CompilerAPICompiler`
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
| 2 | File-extension dispatch in `addJob()` | No signature change to `addJob()`; minimal call-site modifications |
| 3 | Shared atomic identifier counter | Diagnostic IDs globally unique across Java and Kotlin |
| 4 | One `KotlinCompiler` instance per `JobQueue` | Mirrors `CompilerAPICompiler` lifecycle; K2 is stateless per invocation |

---

## Known Limitations (MVP)

- **No per-class language mixing** -- each class is wholly Java OR wholly Kotlin (a package may contain both, see `JobQueue` mixed-language dispatch)
- **K2 cold-start latency** -- first compile ~2-5s due to class loading
- **No incremental compilation** -- full K2 on all `.kt` sources each time
- **Full Kotlin language accepted** -- K2 doesn't restrict to educational subset
