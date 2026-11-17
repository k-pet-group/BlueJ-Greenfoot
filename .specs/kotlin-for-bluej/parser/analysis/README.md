---
id: kotlin-for-bluej-parser-analysis
type: submodule-design
title: "Kotlin Semantic Analysis"
status: active
parent: kotlin-for-bluej-parser
depends-on:
  - bluej-pkgmgr
  - bluej-utility
  - kotlin-for-bluej-compiler
tags:
  - kotlin
---

# Kotlin Semantic Analysis

> `bluej.parser.kotlin.analysis` -- type-resolved analysis of a project's Kotlin sources, the
> foundation for Kotlin code completion. The PSI-only parsing in the parent spec cannot resolve types.
> Its consumer, the completion engine in the same package, is specified in
> [Kotlin Code Completion](../../editor/completion/README.md).

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | K1 (`BindingContext`) frontend from the bundled `kotlin-compiler-embeddable` | No new dependencies. The Analysis API is not in the embeddable jar and would need the unshaded compiler plus IntelliJ core alongside the shaded PSI. Validated on 2.1.20-2.5.0-Beta2 (see Known Limitations for deprecation). |
| 2 | One environment per project, owned by `Project`, created on first request | Java-only projects pay nothing; switching projects needs no rebuild. |
| 3 | Rebuild the environment when the classpath or the Java sources on disk change | An environment never notices later changes to Java files (edited or new), and the compiler-only environment supports neither PSI cache dropping nor a useful file-system refresh. Rebuilding is cheap once the session's first environment exists (tens of ms). |
| 4 | Create the replacement before disposing the old environment | Environments share one Kotlin application environment, which is torn down when its last user goes; keeping one alive avoids repaying the ~0.3s start-up. |
| 5 | Kotlin sources passed as text on every request | Unsaved editor text is analysed; Kotlin edits never force a rebuild. |
| 6 | Same classpath as compilation, project directory included | Mirrors what the compilers see, and keeps project classes that have no source visible. Sources (Kotlin text and Java files) take precedence over stale bytecode in the project directory. |
| 7 | Requests run one at a time on the shared background pool; only the newest waiting request is kept | No new threads (Kotlin architecture threading model). Superseded requests (e.g. repeated Ctrl+Space) are cancelled instead of queueing analyses nobody will see. |
| 8 | Own sub-package rather than the flat parser package | A separate capability with its own environment lifecycle and threading; keeps the PSI-only parsing classes independent of the analysis frontend. |

---

## Contracts

- **What is analysed**: the requested file's editor text, every other `.kt` file beneath the project
  directory (open editors' text in preference to the saved file), Java sources beneath the project
  directory as saved on disk, and the compilation classpath with the running JDK. Hidden directories
  (e.g. `.git`) are skipped. Unreadable files are left out rather than failing the request.
- **Request snapshot**: built on the FX thread from the project (`KotlinAnalysisRequest.forProject`);
  file reading and analysis happen on a background thread.
- **Task results**: a task runs on the analysis thread while the analysis is valid. It must return
  plain values: PSI and descriptors belong to an environment that may be replaced or disposed later.
- **Futures**: completed on a background thread (requesters move to the FX thread themselves). A
  request completes exceptionally if analysis or its task fails -- the frontend can throw assertion
  errors or overflow the stack on unusual code -- and the queue carries on with the next request.
  A request waiting to start is cancelled by a newer request or by disposal; a started one always
  finishes.

---

## Key Lifecycles

- **Service**: created only by `Project.getKotlinAnalysisService()` (FX thread, first use) and disposed
  only by `Project.cleanUp`, which BlueJ's last package frame and both Greenfoot close paths call when
  a project closes (on application exit the process ends anyway). After disposal, new requests fail
  immediately and the waiting request is cancelled.
- **Environment**: created by the first request; replaced by any request whose classpath or Java
  source stamps (path, modification time, size) differ from the current environment's; released on
  service disposal, after the running request (if any) has finished. Only the running request or the
  final release touches it, so environments are never used concurrently.
- **Request**: submitted -> waiting (may be cancelled by a newer request or disposal) -> running ->
  completed normally or exceptionally. A requester cancelling a waiting future simply skips it.

---

## Known Limitations

- **K1 is deprecated upstream**: marked deprecated (warning) in Kotlin 2.2 and error-level in 2.3+,
  still shipped in 2.5.0-Beta2 with no announced removal date. Java callers only see javac
  deprecation warnings. The environment is isolated here so a future Analysis API implementation can
  replace it. Kotlin 2.4+ also requires registering a compiler-plugin extension storage when creating
  any environment (this affects `KotlinEnvironmentManager` too).
- **Unsaved Java edits are not seen** -- Java sources are read as last saved.
- **No cancellation of a running analysis** -- a superseded or timed-out request still finishes in the
  background; only its result is discarded.
- **Whole-project analysis per request** -- the project directory is scanned and all Kotlin files are
  analysed each time. Fine at student scale; large projects would need narrower analysis.
- **Memory** -- one environment per open project that has requested analysis (tens of MB).
- **Automated tests are deliberately thin** -- `forProject` snapshotting needs a live project and is
  checked by hand; the service is covered with directly constructed requests for the sources contract and
  the queue contract only. Environment reuse and rebuilding are K1 internals that will go with the
  Analysis API integration and are not pinned by tests.
