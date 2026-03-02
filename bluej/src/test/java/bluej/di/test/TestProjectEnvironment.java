/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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
package bluej.di.test;

import bluej.di.BlueJInjector;
import bluej.di.BlueJInjectorTestUtils;
import bluej.di.modules.ProjectScopeModule;
import bluej.di.scopes.ProjectId;
import bluej.di.scopes.ProjectScope;
import bluej.di.scopes.ProjectScopeTestUtils;
import bluej.parser.context.CompilationUnitContextLoader;
import bluej.parser.context.TestClassLoaderProvider;
import com.google.inject.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * JUnit 4 {@link TestRule} that manages a test DI environment.
 *
 * <p>Initialises the {@link BlueJInjector} singleton before each test
 * and resets it afterwards.  Provides a fluent API for opening test
 * projects inside a properly wired DI scope.
 *
 * <h3>Basic usage</h3>
 * <pre>
 * &#64;Rule
 * public TestProjectEnvironment env =
 *         TestProjectEnvironment.withDefaults();
 *
 * &#64;Test
 * public void loaderIsResolvable() {
 *     env.openProject().fromFixture("simple").withScope(() -&gt; {
 *         var loader = BlueJInjector.getInstance(
 *                 CompilationUnitContextLoader.class);
 *         assertNotNull(loader);
 *     });
 * }
 * </pre>
 *
 * <h3>Custom modules</h3>
 * <pre>
 * &#64;Rule
 * public TestProjectEnvironment env = TestProjectEnvironment.builder()
 *         .withModule(new ProjectScopeModule())
 *         .withModule(myOverrideModule)
 *         .build();
 * </pre>
 *
 * @see BlueJInjector
 * @see ProjectScope
 */
public class TestProjectEnvironment implements TestRule {

    private final List<Module> modules;
    private final List<Path> tempDirs = new ArrayList<>();

    private TestProjectEnvironment(List<Module> modules) {
        this.modules = modules;
    }

    // ── factory methods ──────────────────────────────────────────────

    /**
     * Create an environment with default configuration
     * ({@link ProjectScopeModule} only).
     */
    public static @NotNull TestProjectEnvironment withDefaults() {
        return builder().build();
    }

    /**
     * Start building a custom environment.
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    // ── TestRule lifecycle ────────────────────────────────────────────

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                setUp();
                try {
                    base.evaluate();
                } finally {
                    tearDown();
                }
            }
        };
    }

    private void setUp() {
        BlueJInjectorTestUtils.reset();
        ProjectScopeTestUtils.resetThreadLocal();
        BlueJInjector.initialize(modules.toArray(new Module[0]));
    }

    private void tearDown() {
        ProjectScopeTestUtils.resetThreadLocal();
        BlueJInjectorTestUtils.reset();
        cleanupTempDirs();
    }

    private void cleanupTempDirs() {
        for (Path dir : tempDirs) {
            try {
                deleteRecursively(dir);
            } catch (IOException ignored) {
                // Best-effort cleanup
            }
        }
        tempDirs.clear();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc)
                    throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ── project opening API ──────────────────────────────────────────

    /**
     * Start building a test project to open within a DI scope.
     *
     * @return a new {@link ProjectBuilder}
     */
    public @NotNull ProjectBuilder openProject() {
        return new ProjectBuilder();
    }

    // ── ProjectBuilder ───────────────────────────────────────────────

    /**
     * Fluent builder for opening a test project inside a DI scope.
     *
     * <p>Use one of the source methods ({@link #fromFixture},
     * {@link #inTempDirectory}) to specify where the project
     * directory comes from, then call {@link #withScope} to
     * execute test code inside the scope.
     */
    public class ProjectBuilder {

        private Path projectDir;
        private @Nullable CompilationUnitContextLoader loader;

        private ProjectBuilder() {}

        /**
         * Use a named fixture from classpath resources as the
         * project directory.  The fixture is copied to a fresh
         * temp directory (auto-cleaned after the test).
         *
         * @param fixtureName e.g. "simple"
         * @return this builder
         */
        public @NotNull ProjectBuilder fromFixture(@NotNull String fixtureName) {
            try {
                String suffix = UUID.randomUUID().toString().substring(0, 8);
                Path tempDir = Files.createTempDirectory(
                        "test-" + fixtureName + "-" + suffix);
                tempDirs.add(tempDir);
                TestProjectBuilder.copyFixtureToDirectory(fixtureName, tempDir);
                this.projectDir = tempDir;
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to copy fixture: " + fixtureName, e);
            }
            return this;
        }

        /**
         * Create the project in a fresh temp directory
         * (auto-cleaned after the test).
         *
         * @return this builder
         */
        public @NotNull ProjectBuilder inTempDirectory() {
            try {
                String suffix = UUID.randomUUID().toString().substring(0, 8);
                Path tempDir = Files.createTempDirectory(
                        "test-project-" + suffix);
                tempDirs.add(tempDir);
                this.projectDir = tempDir;
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create temp directory", e);
            }
            return this;
        }

        /**
         * Use an existing directory as the project root.
         * The directory is NOT auto-cleaned.
         *
         * @param dir the project directory
         * @return this builder
         */
        public @NotNull ProjectBuilder inDirectory(@NotNull Path dir) {
            this.projectDir = dir;
            return this;
        }

        /**
         * Override the {@link CompilationUnitContextLoader} seeded
         * into the scope.  If not called, a loader is created
         * automatically from the project directory.
         *
         * @param loader the loader to seed
         * @return this builder
         */
        public @NotNull ProjectBuilder withLoader(@NotNull CompilationUnitContextLoader loader) {
            this.loader = loader;
            return this;
        }

        /**
         * Enter the project scope, run the action, and exit.
         *
         * <p>Creates a {@link ProjectId} from the project directory,
         * seeds a {@link CompilationUnitContextLoader} (auto-created
         * or explicitly set), enters the scope, runs the action,
         * then exits the scope.
         *
         * @param action the test code to run within scope
         */
        public void withScope(@NotNull Runnable action) {
            var ctx = buildContext();
            try (var handle = ProjectScope.enter(ctx.projectId(), ctx)) {
                action.run();
            }
        }

        /**
         * Enter the project scope, run the supplier, exit, and
         * return the result.
         *
         * @param action the test code to run within scope
         * @param <T>    the return type
         * @return the result of the supplier
         */
        public <T> T withScope(@NotNull Supplier<T> action) {
            var ctx = buildContext();
            try (var handle = ProjectScope.enter(ctx.projectId(), ctx)) {
                return action.get();
            }
        }

        private @NotNull ProjectScope.ScopeContext buildContext() {
            if (projectDir == null) {
                inTempDirectory();
            }

            ProjectId id = ProjectId.of(projectDir.toFile());
            var ctx = new ProjectScope.ScopeContext(id);

            CompilationUnitContextLoader loaderToSeed = this.loader;
            if (loaderToSeed == null) {
                var provider = new TestClassLoaderProvider(projectDir.toFile());
                loaderToSeed = new CompilationUnitContextLoader(provider);
            }
            ctx.seed(CompilationUnitContextLoader.class, loaderToSeed);

            return ctx;
        }
    }

    // ── Builder ──────────────────────────────────────────────────────

    /**
     * Builder for configuring a {@link TestProjectEnvironment}.
     */
    public static class Builder {

        private final List<Module> modules = new ArrayList<>();

        private Builder() {}

        /**
         * Add a Guice module to the test injector.
         *
         * @param module the module to install
         * @return this builder
         */
        public @NotNull Builder withModule(@NotNull Module module) {
            modules.add(module);
            return this;
        }

        /**
         * Build the environment.  If no modules were added,
         * {@link ProjectScopeModule} is used as the default.
         *
         * @return a configured {@link TestProjectEnvironment}
         */
        public @NotNull TestProjectEnvironment build() {
            if (modules.isEmpty()) {
                modules.add(new ProjectScopeModule());
            }
            return new TestProjectEnvironment(modules);
        }
    }
}
