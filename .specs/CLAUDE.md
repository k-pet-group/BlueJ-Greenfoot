# CLAUDE.md — Coding Guidelines for BlueJ-Greenfoot

## Project Overview

BlueJ and Greenfoot are educational Java IDEs. This is a multi-module Gradle project targeting **Java 21** with **JavaFX 23**. The codebase is mature (1999–present), GPL v2 licensed with Classpath exception.

### Modules

| Module | Purpose |
|---|---|
| `bluej/` | BlueJ IDE — editor, debugger, package manager, compiler integration |
| `greenfoot/` | Greenfoot IDE — game/simulation framework for education |
| `boot/` | JVM bootstrap launcher and splash screen |
| `lang-stride/` | Stride language support (frame-based programming) |
| `threadchecker/` | Compile-time thread safety annotation processor |
| `anns-threadchecker/` | Thread checker annotation definitions |

### Build & Run

```bash
./gradlew build test -Pheadless=true   # Build and test (headless for CI)
./gradlew :bluej:build                 # Build BlueJ only
./gradlew :greenfoot:build             # Build Greenfoot only
```

- Java **21** is required (`options.release = 21`)
- Encoding: **UTF-8** (enforced in build.gradle)
- Tests run headless by default with TestFX + Monocle

---

## Formatting

### Braces — Split Convention

**Classes and methods: Allman style** (opening brace on its own line):
```java
public class MyClass
{
    public void myMethod()
    {
        // body
    }
}
```

**Control structures: K&R style** (opening brace on same line):
```java
if (condition) {
    doSomething();
}

for (int i = 0; i < n; i++) {
    process(i);
}

try {
    riskyOperation();
}
catch (IOException e) {
    handleError(e);
}
```

**`catch` goes on its own line** (not `} catch`):
```java
try {
    // ...
}
catch (IOException e) {
    // ...
}
```

### Indentation

- **4 spaces** — never tabs
- Continuation lines: 4 or 8 additional spaces

### Line Length

- Soft target: ~120 characters
- Longer lines acceptable for Javadoc, string constants, and complex generics
- No strict hard limit enforced

### Blank Lines

- 1 blank line between methods
- 1 blank line between field groups and first method
- 0–1 blank lines within methods for logical grouping

### Braces on Single Statements

Always use braces, even for single-line bodies:
```java
// ✅ Correct
if (x == null) {
    return;
}

// ❌ Avoid
if (x == null) return;
```

### Spaces

- Spaces around binary operators: `x = 1`, `a + b`, `i < n`
- Space after keywords: `if (`, `for (`, `while (`, `catch (`
- No space before semicolons
- Space after commas in parameter lists: `foo(a, b, c)`

---

## Naming Conventions

### Classes & Interfaces

- **PascalCase** — `FlowEditor`, `JdiDebugger`, `FindPanel`
- Descriptive suffixes: `*Manager`, `*Handler`, `*Factory`, `*Listener`, `*Dialog`, `*Panel`
- Interfaces: no `I` prefix — just `DebuggerListener`, `CompileObserver`
- Abstract classes: descriptive names preferred over `Abstract*` prefix

### Methods

- **camelCase** — `getClassName()`, `findIdentifierDefinitions()`
- Accessors: `get*`, `set*`, `is*`, `has*` for booleans

### Fields

- **camelCase, no prefix** — `destDir`, `classPath`, `showingContextMenu`
- No `m_` or `_` prefix on private fields
- Use `this.` only for disambiguation in constructors/setters:
  ```java
  this.destDir = destDir;
  ```

### Constants

- **UPPER_SNAKE_CASE** — `NO_ERROR`, `COMPILER_OPTIONS`, `DEF_WIDTH`

### Enums

- Class: **PascalCase** — `CompileType`, `Focus`
- Constants: **UPPER_SNAKE_CASE** — `EXPLICIT_USER_COMPILE`, `STOPPED`

### Packages

- Lowercase, dot-separated by function: `bluej.editor.flow`, `greenfoot.core`

### Generic Type Parameters

- Single letters for simple cases: `<K, V>`, `<T>`
- **UPPER_SNAKE_CASE descriptive names** for complex cases: `<SLOT_FRAGMENT>`, `<DELEGATE_IDENT>`

---

## Documentation

### Copyright Header (Required on All Java Files)

Every Java file must begin with the GPL copyright block. When modifying a file, **update the copyright year** to include the current year. For newer files, use only the current year (e.g., `2026`) rather than accumulating past years — existing year lists like `1999-2009,...,2026` on long-lived files should just have the current year appended. The CI runs `check-copyright.sh` to enforce this.

```java
/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,...,2026  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
 */
```

For Greenfoot files, use "the Greenfoot program" and "Poul Henriksen and Michael Kolling".

### Javadoc

- **Public classes**: always documented (no `@author` tags — these have been removed from the codebase)
- **Public methods**: always documented with `@param`, `@return`, `@throws`
- **Private/package methods**: use `//` inline comments when explanation is needed; Javadoc not required
- **Fields**: Javadoc (`/** ... */`) for public/protected; `//` inline for private
- Use `<p>` to separate paragraphs within Javadoc

**Keep Javadoc concise** — class-level Javadoc should be 1–5 lines of description, not 15–30 line essays. Capture the "what" and "why" briefly; implementation details belong in inline comments:

```java
// ✅ Good — concise, informative
/**
 * Kotlin tokenizer wrapping the PSI KotlinLexer. Produces LocatableTokens
 * compatible with BlueJ's parser infrastructure.
 */

// ❌ Too verbose — multi-paragraph design rationale in class Javadoc
/**
 * This class is responsible for tokenizing Kotlin source code. It wraps
 * the PSI KotlinLexer from kotlin-compiler-embeddable and maps each PSI
 * token type to a BlueJ integer constant...
 *
 * <p>The design decision to wrap rather than extend was made because...
 *
 * <p>Thread safety: this class is safe to use from any thread because...
 *
 * <p>Performance considerations: the PSI lexer operates in O(n) time...
 * [27 more lines]
 */
```

### Inline Comments

- Use `//` style — explain "why", not "what"
- Self-documenting code is preferred over heavy commenting
- `TODO` and `XXX` comments are acceptable for incomplete work or known limitations, in the form: `// TODO explanation` or `// XXX explanation`
- **Preserve original comments during refactoring** — when moving or extracting code from one class to another, carry over the original inline comments. They capture edge-case reasoning that is easy to lose

### No Banner Comment Separators

Do **not** use decorative banner separators to divide sections within a file:

```java
// ❌ Avoid these
// ========================================================================
// Token Type Constants
// ========================================================================

// ========================================
// PSI → BlueJ mapping
// ========================================

// ❌ Also avoid box-drawing / heavy rule separators
// ╔══════════════════════════════════════╗
// ║  Section Name                        ║
// ╚══════════════════════════════════════╝
```

Instead, use a **single-line group header** when needed:

```java
// ✅ OK — lightweight section header
// --- Keyword tokens (100–149) ---
public static final int KW_FUN = 100;
```

---

## Imports

### Organization

Group imports in this order, separated by blank lines:

1. `java.*` / `javax.*` (standard library)
2. `bluej.*` / `greenfoot.*` (project packages)
3. Third-party (`com.google.*`, `javafx.*`, `nu.xom.*`, `org.*`)
4. `threadchecker.*`

### Wildcard Imports

Wildcard imports **are used** and acceptable in this project:
```java
import java.util.*;
import bluej.compiler.*;
import bluej.utility.*;
```

### Always Import — Never Use Fully-Qualified Names Inline

**Always add an import** and use the short class name. Never use fully-qualified names in method bodies, field declarations, or type annotations:

```java
// ✅ Correct — import at top, short name in code
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;

if (child instanceof PsiWhiteSpace) { ... }

// ❌ Wrong — FQ name inline
if (child instanceof org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace) { ... }
```

The only exception is disambiguation when two classes share the same simple name (e.g., `java.util.List` vs `java.awt.List`) — in that case, import the more frequently used one and qualify the other.

---

## Architecture & Patterns

### Threading — `@OnThread` Annotations

This project uses a custom compile-time thread checker (`threadchecker/` module). **Always annotate** thread-sensitive code. The checker runs during the ANALYZE phase of javac and validates all call chains respect thread contracts.

```java
@OnThread(Tag.FXPlatform)
public void updateUI() { ... }

@OnThread(Tag.Any)
public String getName() { ... }

@OnThread(value = Tag.Any, requireSynchronized = true)
private boolean paused;  // Requires synchronized(this) to access
```

**Thread tags** (in order of prevalence — 2,889 annotations across 616 files):

| Tag | Usage | Meaning |
|-----|-------|---------|
| `Tag.FXPlatform` | 58% | Strictly the JavaFX Application Thread |
| `Tag.Any` | 26% | Safe to call from any thread |
| `Tag.FX` | 8% | FX thread OR an FX loading thread (superset of FXPlatform) |
| `Tag.VMEventHandler` | 3% | JDI debugger event-handler thread |
| `Tag.Worker` | 3% | Background worker threads |
| `Tag.Simulation` | 2% | Greenfoot simulation thread (debug VM only) |
| `Tag.Swing` | <1% | Swing EDT (legacy, nearly eliminated) |

**Key calling rules:**
- `FXPlatform` can call `FX` methods, but NOT vice versa
- `Any` destinations are callable from anywhere
- `@SuppressWarnings("threadchecker")` suppresses checking for a method
- Package-info files declare default thread context for all classes in a package

**Cross-thread dispatch patterns:**
- `Platform.runLater()` — fire-and-forget FX dispatch
- `JavaFXUtil.runNowOrLater()` — runs now if on FX thread, otherwise `Platform.runLater()`
- `JavaFXUtil.runPlatformFuture()` — returns `CompletableFuture<T>` completed on FX thread
- `JavaFXUtil.runPlatformAndWait()` — blocks calling thread until FX completes the task
- `Utility.runBackground()` — submits to an 8-thread `ScheduledExecutorService` pool

**Critical deadlock avoidance rules:**
- Never hold the `Simulation` monitor during user code execution (`world.act()`, `actor.act()`)
- Release `JdiDebugger.this` before calling `VMReference.newClassLoader()`
- Release `SoundClip`'s lock before calling `soundClip.stop()` (JDK callback deadlock)
- `getVMNoWait()` (not `getVM()`) when already holding other locks

See `.specs/anns-threadchecker/README.md` for full compatibility matrix and `.specs/threadchecker/README.md` for checker details.

### Singleton Pattern

Central managers use `getInstance()`:
```java
Boot.getInstance()
WorldHandler.getInstance()
Simulation.getInstance()
ExtensionsManager.getInstance()
```

### Listener/Observer Pattern

Event handling uses interfaces with `*Listener` suffix:
```java
public interface DebuggerListener {
    void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate);
}
```

### Error Handling

- Mix of checked and unchecked exceptions
- Custom exception classes (e.g., `GreenfootStorageException`, `ExtensionException`)
- **No standard logging framework** — use `Debug.message()`, `Debug.reportError()` from `bluej.utility.Debug`
- `Debug.reportError(String, Throwable)` for errors with stack traces

### Collections

- Declare with interface types: `List<File>`, `Map<String, Object>`
- Guava is available and used: `ImmutableSet`, `ImmutableList`, `Sets`, `Cache`/`CacheBuilder`
- Prefer `Collections.unmodifiable*()` or Guava immutables for read-only views
- `List.of()`, `Set.of()`, `Map.of()` for small immutable collections

### Null Handling

- Defensive null checks are the primary pattern: `if (x != null) { ... }`
- `@NotNull` (JetBrains annotations) used on method signatures when helpful
- `Optional` is **not widely used** — prefer null checks to match existing style

### Lambdas & Streams

Used extensively — prefer them for collection processing and event handlers:
```java
items.stream()
    .filter(item -> item.isValid())
    .map(Item::getName)
    .collect(Collectors.toList());

Platform.runLater(() -> updateDisplay());
```

### Access Modifiers

- Default to `private` for fields
- Use `final` on fields where possible
- Package-private access is used for framework internals (e.g., Greenfoot `Actor.x`, `Actor.y`)

### Resource Cleanup Ordering

When disposing resources, **release/dispose first, then null references**. This prevents other threads from seeing null fields and trying to recreate resources while the old ones are still being torn down:

```java
// ✅ Correct — dispose before nulling
public static synchronized void dispose()
{
    if (resource != null)
    {
        Disposer.dispose(resource);
        cachedValue = null;
        resource = null;
    }
}

// ❌ Wrong — nulling before dispose creates a race window
public static synchronized void dispose()
{
    if (resource != null)
    {
        cachedValue = null;  // Other thread sees null, tries to recreate
        resource = null;
        Disposer.dispose(resource);  // Now resource is null!
    }
}
```

### No Duplicated Utility Methods

When the same helper logic is needed in multiple classes, **extract it to a shared utility class** rather than duplicating the method. Check for existing utilities before writing new ones:

- `bluej.utility.Utility` — general utilities
- `bluej.utility.JavaUtils` — Java reflection and naming helpers
- `bluej.parser.kotlin.KotlinParserUtils` — shared Kotlin parser helpers

### Static Methods Belong to the Right Class

Place static methods on the class that logically owns the concept, not on an arbitrary caller:

```java
// ✅ Correct — error IDs are a compiler concern, live on Compiler base class
Compiler.getNewErrorIdentifier()

// ❌ Wrong — error IDs placed on one specific subclass, forcing cross-class coupling
CompilerAPICompiler.getNewErrorIdentifier()  // KotlinCompiler has to reach into a sibling
```

### Language-Aware Code

When adding support for a new source language (e.g., Kotlin alongside Java/Stride), check that **every branch** in switch/if-else chains that routes by language or file extension handles the new language. Common locations to audit:

- Template generation (e.g., package declarations — Kotlin has no semicolons)
- File cleanup and rename paths in `ClassTarget`
- Class file filters (e.g., `BasenameKt$` pattern for Kotlin facade inner classes)
- Skeleton/template generation in `ClassRole`

---

## Testing

### Running Tests

```bash
# Full build and test (headless for CI/no display)
./gradlew build test -Pheadless=true

# Build and test a single module
./gradlew :bluej:test
./gradlew :greenfoot:test
./gradlew :threadchecker:test

# Run a single test class
./gradlew :bluej:test --tests "bluej.parser.kotlin.KotlinPsiScopeBuilderTest"

# Run all tests in a package
./gradlew :bluej:test --tests "bluej.parser.kotlin.*"

# Run a single test method
./gradlew :bluej:test --tests "bluej.parser.kotlin.KotlinPsiScopeBuilderTest.testIfBodyHasInnerNode"

# Compile without running tests (quick build check)
./gradlew :bluej:compileJava
```

### Framework

- **JUnit 4** (`org.junit.*`) — used throughout the entire codebase
- GUI tests use **TestFX** (`ApplicationTest`)
- **No Mockito** — use manual stubs and helper classes

### Naming

- Test classes: `*Test` suffix — `LexerTest`, `CompletionTest`
- Test methods: `test*` prefix with camelCase:
  ```java
  @Test public void testFieldAccess() { ... }
  ```

### Assertions

- Direct assertions: `assertEquals`, `assertTrue`, `assertNotNull` (from `org.junit.Assert`)
- Hamcrest matchers available but not required
- Include failure messages for non-obvious assertions:
  ```java
  assertTrue("Lambda should inherit FXPlatform tag", result.success);
  ```

### Test Data

- Inline source strings and constructors preferred
- Use helper classes/factories for complex setup (`WorldCreator`, `TestEntityResolver`)
- Temporary files: use `@TempDir` (JUnit 5) or `File.createTempFile()` with `deleteOnExit()`

---

## Specifications

Design documentation is maintained in `.specs/`.

### Spec Writing Rules

#### What a Spec Is For

A spec captures what is **hard to recover from the code and the file system** — the *why*, not the *what*. Code, signatures, and directory structure are already visible directly; duplicating them in a spec only creates a second copy that drifts out of date.

- **Focus on decisions, contracts, and limitations.** Record the design decisions and their rationale, the invariants/contracts a component must uphold, and the known limitations and out-of-scope boundaries. These are the things a reader cannot infer by opening the source.
- **Don't duplicate — refer.** If information already lives in another spec or in the code, link to it rather than restating it. One source of truth; the spec points to it.
- **No file trees, no copied code.** Do not paste directory listings or verbatim code blocks — both are duplications of things already plainly visible. When a snippet genuinely aids understanding (e.g. the shape of a decision branch), use short **pseudocode** that conveys intent, not a copy of the implementation.

#### Key Lifecycles (REQUIRED for module-design specs)

**Every module spec that introduces a new entity type** (role, target, handler, state, filter, etc.) **MUST include a "Key Lifecycles" section** that traces ALL code paths that create, mutate, or destroy that entity. Do not document only the "canonical" assignment path — search for every site that instantiates or sets the entity and verify each path produces the correct state.

For each new entity type, trace:
1. **Creation** — List ALL instantiation paths: constructors, factories, deserialization, template-based creation, reflection. For EACH path: does it set the correct initial state?
2. **Mutation** — What triggers state changes? Are there "early guess" mechanisms that set a temporary value before the "proper" assignment? Is the proper assignment **guaranteed to run**, or can it be skipped?
3. **Destruction / cleanup** — How is it removed? Dangling references?

**How to discover lifecycles:**
- Search for `new {EntityType}(` to find all instantiation sites
- Search for setter calls (`set{Entity}`) to find all mutation sites
- Trace each call chain back to its trigger (user action, file load, compilation)
- Check for "fallback" or "default" branches in switch/if-else chains that might silently apply to the new entity

**Why:** A bug in the Top-Level Functions module went undetected because the spec documented role assignment via `determineRole()` but missed a separate "early guess" in the `ClassTarget` constructor that defaulted to `StdClassRole`. The implementation followed the spec exactly — inheriting the gap.

#### Marking Components as "Unchanged"

When a module spec notes a component is "unchanged", verify this by tracing the new entity through that component's internal routing (switch/if-else chains, fallback defaults). A component unchanged at the API level may still need updates for new entity types.

### Key Specs

### Architecture & Cross-Cutting

| Spec | Path | Key Content |
|------|------|-------------|
| Architecture Design | `.specs/DESIGN_DOC.md` | System overview, threading architecture, all named threads, synchronization strategy, deadlock avoidance |
| Thread-Checker Annotations | `.specs/anns-threadchecker/README.md` | Tag enum semantics, calling/override compatibility matrix, `@OnThread` parameters |
| Thread-Safety Checker | `.specs/threadchecker/README.md` | Plugin lifecycle, tag resolution priority, lambda inference, built-in library knowledge |

### BlueJ Core Modules

| Spec | Path | Key Content |
|------|------|-------------|
| Debugger (JDI) | `.specs/bluej/debugger/README.md` | JDI field rendezvous protocol, 6-thread model, monitor hierarchy, VMEventHandler |
| Debug Manager (UI) | `.specs/bluej/debugmgr/README.md` | Shell generation, Invoker lifecycle, ResultWatcher hierarchy, ObjectBench |
| Runtime (ExecServer) | `.specs/bluej/runtime/README.md` | Debug VM two-thread model, static field IPC, breakpoint sync, thread-target dispatch |
| Compiler | `.specs/bluej/compiler/README.md` | Single-thread compilation queue, producer-consumer, observer adapters |
| Package Manager | `.specs/bluej/pkgmgr/README.md` | Central orchestrator, compilation batching, 42 synchronized blocks |
| Source Code Editor | `.specs/bluej/editor/README.md` | Dual editor modes, FXPlatform enforcement, EditorWatcher contract |
| Utility Library | `.specs/bluej/utility/README.md` | 8-thread background pool, FX/Worker bridge, CompletableFuture patterns |
| Terminal | `.specs/bluej/terminal/README.md` | Cross-thread I/O, InputBuffer thread safety |

### Greenfoot Modules

| Spec | Path | Key Content |
|------|------|-------------|
| Simulation Core | `.specs/greenfoot/core/README.md` | SimulationThread, two-lock architecture, act cycle, pause/resume protocol |
| VM Communication | `.specs/greenfoot/vmcomm/README.md` | Shared memory IPC, 3-lock protocol, image double-buffering, command reliability |
| Audio System | `.specs/greenfoot/sound/README.md` | 6 thread types, ClipProcess/ClipCloser threads, deadlock avoidance |

Run `ls .specs/` for the full list of 54 specifications covering all modules and submodules.

---

## Checklist Before Committing

1. ✅ **Copyright year updated** in modified files — include the current year (e.g., `2026`)
2. ✅ **`@OnThread` annotations** added/updated for new thread-sensitive code
3. ✅ **Javadoc** present on all new public classes (no `@author` tags) and methods
4. ✅ **Braces style** follows the split convention (Allman for class/method, K&R for control flow)
5. ✅ **4-space indentation**, no tabs
6. ✅ **No FQ names inline** — all types are imported, short names used in code
7. ✅ **Import order** — `java.*` → `bluej.*`/`greenfoot.*` → third-party → `threadchecker.*`
8. ✅ **No duplicated code** — shared helpers extracted to utility classes
9. ✅ **No banner separators** — use single-line `// ---` headers if grouping is needed
10. ✅ **Tests pass**: `./gradlew build test -Pheadless=true`
