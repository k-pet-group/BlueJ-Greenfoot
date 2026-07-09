---
id: greenfoot-collision
type: submodule-design
title: "Collision Detection"
status: active
parent: greenfoot
---

# Collision Detection

> Auto-generated from code analysis. Review and refine.

Collision detection system (16 files) using spatial partitioning for efficient queries. Supports the Actor API methods like `getIntersectingObjects()`, `getObjectsAtOffset()`, and `getObjectsInRange()`.

---

## Key Interfaces and Classes

| Class | Purpose |
|-------|---------|
| `CollisionChecker` (interface) | Abstract collision detection algorithm |
| `ColManager` | Manager that delegates to the appropriate checker; handles lazy init and optimization |
| `ClassQuery` | Queries for actors of a specific class within a region |

---

## IBSP Sub-package

The `ibsp/` (Improved Binary Space Partition) sub-package provides the primary collision algorithm:

| Class | Purpose |
|-------|---------|
| `IBSPColChecker` | Optimized spatial partitioning for broad-phase collision |
| `BSPNode` | Binary space partition tree node |
| `ActorNode` | Actor wrapper for BSP tree storage |
| `BSPNodeCache` | Reusable BSP node pool for performance |
| `Rect` | Rectangle representation for spatial queries |

---

## Dependencies

Uses: Actor/World from root greenfoot package

---

## Design Decisions

> TODO: Document rationale — cannot be inferred from code alone.
