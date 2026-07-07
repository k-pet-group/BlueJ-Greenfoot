---
id: greenfoot-util
type: submodule-design
title: "Greenfoot Utilities"
status: active
parent: greenfoot
---

# Greenfoot Utilities

> Auto-generated from code analysis. Review and refine.

General-purpose utility classes for Greenfoot runtime (9 files). Provides file operations, resource loading, image caching, timing, and platform-specific delegation.

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `GreenfootUtil` | Central static utility hub: file ops, resource lookup, sound enumeration, image caching, MP3 detection, UserInfo delegation |
| `GraphicsUtilities` | Graphics rendering helpers |
| `HDTimer` | High-definition timer for performance-critical timing |
| `ExternalAppLauncher` | Launches external applications (browser, file manager) |
| `DebugUtil` | Debug output utilities |
| `Version` | Version information management |
| `Circle` | Simple geometric circle representation (x, y, radius) |
| `StandalonePropStringManager` | Property string management for standalone exports |
| `GreenfootStorageException` | Custom exception for storage operations |

---

## Dependencies

Uses: `platforms/` (delegates via GreenfootUtilDelegate)

---

## Design Decisions

> TODO: Document rationale — cannot be inferred from code alone.
