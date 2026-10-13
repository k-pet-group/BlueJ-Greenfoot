---
id: kotlin-for-bluej-build
type: module-design
title: "Build & Distribution"
status: active
parent: kotlin-for-bluej
depends-on:
  - boot
tags:
  - kotlin
---

# Build & Distribution

> How Kotlin dependencies are declared, assembled, discovered at runtime, and shipped to end users.

**Parent spec:** [Kotlin for BlueJ -- Architecture Design](../DESIGN_DOC.md)

---

## Pipeline

| Stage | Component | Kotlin-specific change? |
|-------|-----------|------------------------|
| 1. Declaration | `bluej/build.gradle` | **Yes** -- add 3 Kotlin deps |
| 2. Assembly | `copyToLib` Gradle task | No -- copies all `runtimeClasspath` JARs |
| 3. Discovery | `Boot.getKnownJars()` | **Yes** -- separate IDE dependencies from student stdlib for both IDEs |
| 4. Classpath | `Project.getClassLoader()` -> `Compiler.setClasspath()` | No -- generic passthrough |
| 5. Distribution | Ant `build.xml` + Gradle packaging | No -- `lib/*.jar` bundles all |

---

## Dual Classpath Separation

| Classpath | Contains | Used by |
|-----------|----------|---------|
| IDE (`runtimeClassPath`) | BlueJ's catch-all jars; Greenfoot's explicit IDE whitelist, including the compiler/PSI/metadata closure | IDE classes, KotlinCompiler |
| Student (`runtimeUserClassPath`) | Existing language/runtime jars plus **kotlin-stdlib** (and Greenfoot API for scenarios) | Student code execution, compilation classpath |

`kotlin-compiler-embeddable` is intentionally NOT on the student classpath -- only `kotlin-stdlib`.

---

## Gradle Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `kotlin-compiler-embeddable` | 2.1.20 | K2 compilation + bundled PSI lexer/parser |
| `kotlin-stdlib` | 2.1.20 | Kotlin standard library (bundled for student projects) |
| `kotlin-metadata-jvm` | 2.1.20 | Read Kotlin metadata from compiled `.class` files |

All use `implementation` scope (needed at both compile time and runtime).

---

## Boot.java Change

Both student whitelists include `kotlin-stdlib-*.jar`. BlueJ's catch-all `^bluejcore.jar` pattern
supplies IDE dependencies. Greenfoot instead extends its existing whitelist with compiler-embeddable,
daemon, script-runtime, reflect, metadata-jvm, coroutines, trove4j and JetBrains annotations. These
compiler dependencies must not be added to the shared IDE/student list. The existing Greenfoot Gradle
runtime copy already stages them; no external Kotlin SDK or new dependency versions are needed.

IDE and project loaders have the boot loader as parent, not each other. Jar presence or development
launch success alone does not verify installed loading; tests must use these isolated classpaths,
including compilation, stdlib execution and source/compiled metadata readers. The
[views metadata reader](../../bluej/views/README.md#kotlin-metadata-across-loaders) adapts annotation values
across these loaders without sharing compiler dependencies. Kotlin export dependency
selection is a separate boundary and is unchanged by local scenario support.

---

## Design Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | kotlin-compiler-embeddable on IDE classpath only | Students shouldn't access compiler internals |
| 2 | Catch-all `^bluejcore.jar` for IDE JARs | New Gradle deps auto-available without Boot.java changes |
| 3 | Wildcard `kotlin-stdlib-*.jar` | Version upgrades in build.gradle need no Boot.java change |
| 4 | No Kotlin exclusion in Ant distribution | Kotlin must be bundled (students can't install SDK) |
| 5 | Package.java and Project.java unchanged | Classpath flows through Boot -> BPClassLoader via existing generic mechanisms |

---

## Known Limitations

- **~30-40 MB distribution size increase** -- acceptable for educational tool
- Greenfoot's IDE whitelist must be checked against the transitive closure when upgrading Kotlin; new dependencies are not picked up by a catch-all as they are in BlueJ.
- **No version alignment validation** -- all 3 Kotlin deps must match; currently relies on visual adjacency in build.gradle
