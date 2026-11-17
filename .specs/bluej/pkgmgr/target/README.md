---
id: bluej-pkgmgr-target
type: submodule-design
title: "Class Targets & Roles"
status: active
parent: bluej-pkgmgr
---

# Class Targets & Roles

Represents compilable classes and other project elements in the class diagram. Uses a hierarchy of `Target` -> `EditableTarget` -> `ClassTarget`, with a Strategy pattern for class roles.

---

## Target Hierarchy

| Class | Purpose |
|-------|---------|
| `Target` (abstract) | Base for all diagram elements |
| `EditableTarget` | Base for code-editable targets; implements `EditorWatcher` |
| `ClassTarget` | Compilable Java class with editor/debugger integration |
| `PackageTarget` | Sub-package within the project |
| `ReadmeTarget` | Project README file |

---

## Roles (`target/role/`) -- Strategy Pattern

| Class | Purpose |
|-------|---------|
| `ClassRole` (abstract) | Base role defining behavior for a class kind |
| `StdClassRole`, `InterfaceClassRole`, `AbstractClassRole`, `EnumClassRole`, `UnitTestClassRole` | Concrete roles |

---

## Actions (`target/actions/`) -- Command Pattern

`ClassTargetOperation` (abstract) base with concrete implementations: `CompileAction`, `EditAction`, `InspectAction`, `ConvertToJavaAction`, `ConvertToStrideAction`, `CreateTestAction`, `DuplicateClassAction`, etc.

## Key Contracts

- **`ClassTarget` class initialisation does not depend on `Config`.** Its static text helpers (e.g.
  `replaceSelection`, used by `enforcePackage`) are unit-tested without the application initialised,
  so eager `Config`-backed statics in `ClassTarget` make those tests order-dependent across Gradle
  forks. The J/K/S source-type marker images are therefore loaded on first use through
  `bluej.utility.javafx.SourceTypeIcons`, which also serves `FXTabbedEditor` so the images exist once
  (callers compare them by identity).
