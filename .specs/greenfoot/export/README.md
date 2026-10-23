---
id: greenfoot-export
type: submodule-design
title: "Scenario Export"
status: active
parent: greenfoot
references: [greenfoot-guifx-export, kotlin-for-bluej-build]
---

# Scenario Export

Owns local project archives and the artifacts/protocol used to publish a scenario to the Greenfoot
gallery. The reachable operations are **PROJECT** and **PUBLISH**. There is no standalone executable
export; the remaining viewer and archive machinery do not constitute a supported offline launcher.

## Artifact boundaries

A project GFAR and the optional gallery source ZIP share the source-archive format: files are stored
under the project-directory prefix, preserving source, assets, project metadata and project libraries
subject to the existing exclusions. Opening an archive uses ordinary extraction and Project opening;
Kotlin sources require a Kotlin-capable receiving Greenfoot installation.

The publishing JAR is a different artifact. It contains compiled scenario output and resources without
Java, Kotlin or Stride source files. This source-free rule applies regardless of the source-sharing
checkbox: that checkbox controls the separate source ZIP. It does not remove bytecode, runtime metadata
or sources hidden inside arbitrary resource/library archives, nor prevent decompilation.

Source filtering follows the supported source-type extension contract. Source archives retain source
files unchanged, and resources whose names merely contain a source extension are not removed. Existing
`+libs` handling differs by artifact: project archives preserve it; publishing excludes it and does not
provide general project-library dependency packaging.

## Kotlin runtime provisioning

Kotlin-source publishing, including mixed-source scenarios, embeds the matching installed student stdlib
classes and resources directly into the runtime JAR. It selects the first main stdlib on Boot's student
runtime classpath, not a newer neighbouring file, an IDE dependency closure, a project library or a
remote gallery location. Java/Stride-only publishing performs no Kotlin runtime or licensing lookup.
Compiled-only Kotlin hidden in third-party libraries is not discovered; general library publishing is
a separate boundary rather than a reason to bundle Kotlin into every Java scenario.

The installed stdlib alone does not provision the server, and the upload protocol has no dependency
sidecar field. Direct merging therefore adds runtime payload to Kotlin uploads; avoiding that difference
requires an agreed alternative provisioning contract, a matching deployed runtime and converter testing.
Compiler, PSI, metadata-tool and other IDE dependencies must not enter the artifact. Source GFARs/ZIPs
receive no automatic stdlib: the receiving Kotlin-capable installation supplies it.

The artifact includes upstream licensing/attribution and a stdlib-only clarification. Before creating
a temporary publishing artifact, setup checks require a readable stdlib with core runtime entries and
intact entry data, plus readable nonempty licensing files. These are integrity/setup checks, not a full
API or deployed-version compatibility test. Failure is logged and reported through the existing
publishing-failure UI, without starting an upload.
This narrow setup check does not change subsequent shared archive-write failure semantics or make reads
and output transactional. Distribution staging is owned by `kotlin-for-bluej-build`.

## Key lifecycles

The FX request snapshots Kotlin presence from existing package/class source types after saving; it does
not use the package's preferred language or add a filesystem source inventory. That final decision is
passed to the worker and is not recomputed from live targets there. It freezes the decision, not all
project file contents, and does not certify mixed-source compilation or Stride/Kotlin execution.

Each Kotlin publishing request creates an immutable runtime-input descriptor after validation. Its only
other construction path is the shared empty descriptor for requests without Kotlin; that path consults
neither Boot nor licensing files. Descriptors contain paths, not open streams: validation closes its
resources, archive creation reopens inputs through the existing writer, and descriptors have no mutable
cache or global per-project lifetime. Source-project export does not create a runtime descriptor.


The UI saves project/editor state before giving a background export operation its metadata and World
snapshot. That is not Save the World's recorded-action insertion and does not add a compilation phase.
Export therefore retains the existing Java behavior for already-produced class files and source edits;
it does not repair compiler source selection, add a whole-scenario compile or introduce a readiness guard.

Project export writes the selected GFAR. Publishing creates a temporary runtime JAR, optionally a source
ZIP and a screenshot, then submits them through the existing client. The student World name is stored in
the artifact properties; the upload's main-class field names the scenario viewer. Temporary upload files
are cleaned by the existing publishing completion/error paths. UI status changes are marshalled to FX.

## Gallery boundary

Both new uploads and updates use multipart **POST** to the same upload endpoint after authentication.
An upload-success response confirms receipt, not successful conversion or playback. Greenfoot's server
converts JVM bytecode using TeaVM and a Greenfoot-specific browser implementation in the observed gallery
artifacts/developer guidance. That backend is outside this checkout; its deployed version/options and
Kotlin compatibility remain unconfirmed. Desktop/JVM execution and a valid artifact do not establish
deployed browser compatibility.

Existing Greenfoot/JLayer/Stride runtime references point to gallery-managed locations. Their presence in
a manifest is not a guarantee that a public URL exists or describes the private converter's classpath.
The client does not deploy shared libraries or negotiate converter capabilities.

## Java parity and verification boundary

Java behavior is the baseline for formats, source-sharing choices, ordinary lifecycle and class-output
handling. Any further Kotlin-specific restriction requires explicit alignment of the concrete scenario,
reason, proposed behavior and requirements for restoring parity; it must not be silently accepted as
part of export support. No backend changes, new compatibility guard or general library-publishing
expansion are implied by client artifact support.

Automated archive tests cover source preservation/extraction/fresh Kotlin compilation and isolated
stdlib-dependent execution from a produced runtime JAR. They do not exercise real Project/UI reopening,
Greenfoot World/Actor runtime behavior, installed export dialogs or gallery conversion/playback.

## Known limitations

Archive creation does not reliably propagate I/O failures to its callers. The writer can log an error
and return normally even when no complete artifact exists; project export can then report completion,
and publishing can proceed toward upload. This is a shared, pre-existing limitation across languages,
not a Kotlin-specific failure. Completion feedback or upload initiation is therefore not proof of
successful archive creation. Writes are nontransactional and can leave incomplete output. Correcting
this requires failure propagation through the writer and callers, with tests proving no false completion
or upload after failed packaging; the runtime setup check is not that broader fix.
