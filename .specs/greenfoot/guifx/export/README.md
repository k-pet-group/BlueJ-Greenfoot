---
id: greenfoot-guifx-export
type: submodule-design
title: "Greenfoot Export UI"
status: active
parent: greenfoot-guifx
---

# Greenfoot Export UI

> Auto-generated from code analysis. Review and refine.

Export dialog and publishing UI (9 files). Uses a Strategy pattern with tabs for different export targets: local file, distributable project, and online gallery publishing.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `ExportDialog` | Main export orchestration dialog |
| `ExportTab` (abstract) | Base for export method tabs |
| `ExportLocalTab` | Export to local file system |
| `ExportProjectTab` | Export as distributable project |
| `ExportPublishTab` | Publish to Greenfoot gallery |
| `ImageEditPane`, `ImageEditCanvas` | Scenario icon/image editing |
| `ExportException` | Export-specific exception handling |
| `ProxyAuthDialog` | Proxy authentication for publishing |

---

## Design Decisions

> TODO: Document rationale — cannot be inferred from code alone.
