---
id: greenfoot-guifx-export
type: submodule-design
title: "Greenfoot Export UI"
status: active
parent: greenfoot-guifx
references: [greenfoot-export]
---

# Greenfoot Export UI

Owns the Share dialog, project-archive destination selection, gallery publishing metadata and progress
presentation. It exposes two operations: **PROJECT** (GFAR) and **PUBLISH** (gallery). `ExportLocalTab`
is shared destination-selection machinery, not an additional runnable-export format.

## User contract

The existing active-World/no-argument-constructor checks apply to the dialog. The project destination
and gallery title/account/source-sharing/screenshot choices retain their existing behavior. Opening a
publishing tab can query gallery tags and existing scenarios; constructing this UI is not an offline
artifact-only test.

The source-sharing checkbox controls a separate project-source ZIP, not source inclusion in the runtime
JAR. Archive contents and dependency provisioning belong to `greenfoot-export`, which also owns the
known archive-creation failure-reporting limitation. A successful upload response is not a successful
server conversion or browser-playback result.

## Lifecycle and threading

The FX thread gathers export metadata, saves project/editor state through the existing callback, and
snapshots whether existing package/class targets contain Kotlin source for a publishing request, then
starts a background export operation with that final decision. PROJECT requests skip this lookup.
This does not insert recorded Save-the-World actions or add a compilation step. The export controls
are disabled while that operation is active; progress and completion
updates return to FX. Publishing outcomes retain the existing scenario metadata/screenshot bookkeeping.
Missing/damaged installed Kotlin runtime inputs use that existing failure presentation before packaging;
there is no additional dialog or user-supplied SDK/dependency chooser.

This UI does not add a language-specific compatibility gate, backend capability negotiation, a new export
format, or an unsupported-workflow guard. Kotlin-specific content decisions must preserve the existing
Java interaction model; the artifact implementation and its limits live in `greenfoot-export`.
