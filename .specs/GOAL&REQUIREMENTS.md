# Kotlin for BlueJ

* Project\Feature name: Kotlin for BlueJ
* Category: new-feature
* Priority: high

## Goal

Add Kotlin language support to BlueJ with a focused MVP subset of syntax covering variables, control flow, functions, simple classes, and basic inheritance — enabling beginner CS students and instructors to teach and learn foundational programming and OOP concepts using Kotlin's concise, modern syntax alongside Java.

## Description

BlueJ currently supports Java and Stride as teaching languages. This feature adds Kotlin as a third language option, leveraging BlueJ's existing multi-language architecture (SourceType enum, compiler abstraction, FlowEditor). Rather than supporting the full Kotlin language specification, the MVP focuses on an educational subset — variables with explicit types, control flow, top-level functions, simple classes with primary constructors, and basic inheritance (open/override, abstract classes) — sufficient for teaching foundational programming and introductory OOP concepts in a university course.

Kotlin files (.kt) will be compiled via `kotlin-compiler-embeddable` invoked programmatically (mirroring how BlueJ uses `javax.tools.JavaCompiler` for Java). The editor will provide Kotlin-aware syntax highlighting, basic code completion, class templates, and compiler error reporting. Students can create Kotlin classes from the class diagram, instantiate objects on the object bench, and call methods — preserving BlueJ's signature interactive, visual approach to learning.

## Requirements

### Business Requirements

| Requirement | Priority | Rationale |
| --- | --- | --- |
| Basic I/O: `println()`, string templates (`"Hello, $name!"`) with syntax highlighting | critical | Immediate feedback is essential for beginners — first thing students do |
| Variables: `val`/`var` with explicit types (`Int`, `Double`, `Boolean`, `String`), operators | critical | Data manipulation is the foundation of all programming exercises |
| Control flow: `if`/`else`, `when`, `for`, `while`, ranges (`1..n`), nested constructs | critical | Decision-making and iteration are core CS1 concepts |
| Functions: top-level `fun`, return values, scope and local variables | critical | Modularity and decomposition — key to structured programming |
| Simple classes: primary constructor, properties, `toString()`, method calls | critical | Object-first learning — students create and interact with objects on the bench |
| Inheritance: `open`/`override`, abstract classes, subclassing | high | Core OOP concept — extends the class model to teach polymorphism basics |
| Object bench: create Kotlin objects, call methods (no object inspection in MVP) | critical | BlueJ's signature feature — must work with Kotlin classes |
| Collections: basic `listOf` (read-only) | high | Grouping data — essential for meaningful exercises |
| Type inference: `val x = 5` without explicit type annotation | medium | Conciseness is a key Kotlin advantage; parser complexity is the risk |
| Null safety: safe calls (`?.`), Elvis (`?:`), nullable types | low | Safety awareness — nice-to-have for MVP, important for post-MVP |

### Technical Requirements

#### Technology Stack

| Component | Choice |
| --- | --- |
| Host language | Java 21 (existing BlueJ codebase) |
| Target language | Kotlin 2.1.x (latest stable, K2 compiler) |
| Compiler integration | `kotlin-compiler-embeddable` — programmatic invocation mirroring `javax.tools.JavaCompiler` |
| UI framework | JavaFX 23.0.2 (existing) |
| Build system | Gradle (existing) |
| Testing | JUnit — unit tests + integration tests |

#### IDE Integration (MVP)

| Feature | Description |
| --- | --- |
| Syntax highlighting | Kotlin keyword coloring, string/comment styling in FlowEditor |
| Class creation from diagram | Right-click → New Kotlin Class with templates (class, open class, abstract class, data class) |
| Basic code completion | Auto-complete for Kotlin keywords and class members |
| Error reporting | Show `kotlinc` compiler errors in the editor with line markers |

#### Constraints

| Constraint | Type |
| --- | --- |
| Minimal BlueJ core changes — extend existing abstractions (`SourceType`, `Compiler`) over rewriting | technical |
| Backward compatible — existing Java/Stride projects must continue working unchanged | technical |
| Kotlin stdlib must be bundled — no external Kotlin SDK install required by students | technical |

#### Non-Functional Requirements

| Category | Requirement | Priority |
| --- | --- | --- |
| Usability | Kotlin workflow feels as natural as Java — same class diagram, object bench, compile flow | critical |
| Maintainability | Clean code separation — Kotlin support extensible post-MVP without major refactoring | high |
| Reliability | Compiler errors never crash BlueJ — graceful error handling and clear student-friendly messages | high |

### Post-MVP Roadmap

| Feature | Notes |
| --- | --- |
| Interfaces and polymorphism | Full OOP model |
| Lambdas and higher-order functions | `list.filter { it > 5 }` |
| Mutable collections | `mutableListOf`, iteration |
| Visibility modifiers | `private`, `protected`, `internal` |
| Packages | Kotlin package structure |
| Default and named parameters | Kotlin-specific convenience |
| `break` & `continue` | Loop control |
| Object inspection in BlueJ object view | Inspect/modify Kotlin property values |
| Java ↔ Kotlin interop | Mixed projects with cross-language references |

### Reference

See [.specs/external notes/Kotlin in BlueJ.md](.specs/external%20notes/Kotlin%20in%20BlueJ.md) for the original feature subset analysis and pedagogical comparison table.
