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
| 3. Discovery | `Boot.getKnownJars()` | **Yes** -- add `"kotlin-stdlib-*.jar"` to `bluejUserJars` |
| 4. Classpath | `Project.getClassLoader()` -> `Compiler.setClasspath()` | No -- generic passthrough |
| 5. Distribution | Ant `build.xml` + Gradle packaging | No -- `lib/*.jar` bundles all |

---

## Dual Classpath Separation

| Classpath | Contains | Used by |
|-----------|----------|---------|
| IDE (`runtimeClassPath`) | ALL JARs in lib/ (including kotlin-compiler-embeddable) | BlueJ IDE classes, KotlinCompiler |
| Student (`runtimeUserClassPath`) | bluejcore + javafx + junit + hamcrest + lang-stride + **kotlin-stdlib** | Student code execution, compilation classpath |

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

Add `"kotlin-stdlib-*.jar"` to the `bluejUserJars` array. Wildcard matches any version. The catch-all `"^bluejcore.jar"` pattern handles all other Kotlin JARs for the IDE classpath.

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
- **No Greenfoot Kotlin support** -- `greenfootRuntimeAndUserJars` does not include kotlin-stdlib (deliberate scope boundary)
- **No version alignment validation** -- all 3 Kotlin deps must match; currently relies on visual adjacency in build.gradle
