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
