---
id: bluej-classmgr
type: submodule-design
title: "Class Manager"
status: active
parent: bluej
---

# Class Manager

Dynamic class loading for BlueJ projects (3 files).

---

## Key Classes

| Class | Purpose |
|-------|---------|
| `BPClassLoader` | Project-specific URLClassLoader; replaced on each compilation for fresh class loading |
| `ClassPathEntry` | Represents a single classpath entry (JAR or directory) |
| `ClassMgrPrefPanel` | Preferences panel for classpath configuration |

---

## Key Invariants

- **Per-project isolation**: Each project gets its own class loader to avoid namespace conflicts
- **Refresh on compile**: Class loader is replaced after compilation to load updated bytecode
