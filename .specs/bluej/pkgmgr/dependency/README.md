---
id: bluej-pkgmgr-dependency
type: submodule-design
title: "Dependency Graph"
status: active
parent: bluej-pkgmgr
---

# Dependency Graph

Models class relationships for the visual dependency diagram (7 files).

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `Dependency` (sealed abstract) | Base for all class relationships; permits 3 subtypes |
| `ExtendsOrImplementsDependency` | Inheritance/interface implementation |
| `UsesDependency` | Usage relationships |
| `PermitsDependency` | Sealed class permits relationships |
