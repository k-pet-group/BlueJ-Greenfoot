---
id: bluej-collect
type: submodule-design
title: "Data Collection"
status: active
parent: bluej
---

# Data Collection

Anonymous, opt-in usage statistics collection system (15 files). Tracks IDE interactions (compilations, method calls, test runs, edits) and submits anonymized events to a remote server via HTTP.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `DataCollector` (static proxy) | Public API; all-static methods check user opt-in before delegating to `DataCollectorImpl` |
| `DataCollectorImpl` | Actual event recording; tracks projects, edits, compilations, tests |
| `Event` (interface) | Represents a submittable event; creates `MultipartEntity` payloads |
| `DataSubmitter` | HTTP submission of collected events to server |
| `CodeAnonymiser` | Anonymizes source code before submission |

---

## Dependencies

**External:** Apache HTTP Client (multipart submission), DiffUtils (file diffs)

Uses: `compiler/` (CompileObserver integration), `utility/`
