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
import bluej.di.modules.ProjectScopeModule;
import bluej.di.scopes.ProjectId;
import bluej.di.scopes.ProjectScope;
import bluej.parser.context.CompilationUnitContextLoader;
import bluej.pkgmgr.Project;

import java.util.function.Supplier;

/**
 * Builder for creating flexible test project scopes.
 *
 * <h3>Default Behaviour (Minimal Setup)</h3>
 * <pre>
 * TestProjectScopeBuilder.create().withScope(() -&gt; {
 *     // scope is active on this thread
 * });
 * </pre>
 *
 * <h3>With Mock Project (Unit Tests)</h3>
 * <pre>
 * TestProjectScopeBuilder.create()
 *         .withProject(mockProject)
 *         .withScope(() -&gt; {
 *             // test with mock project
 *         });
 * </pre>
 *
 * @see TestProjectBuilder
 * @see TestProjectScope
 */
public final class TestProjectScopeBuilder {

    private Project project;
    private CompilationUnitContextLoader loader;

    private TestProjectScopeBuilder() {}

    /**
     * Create a new builder instance.
     *
     * @return a new TestProjectScopeBuilder
     */
    public static TestProjectScopeBuilder create() {
        return new TestProjectScopeBuilder();
    }

    /**
     * Set the Project instance to use in the scope.
     *
     * @param project the Project instance (mock or real)
     * @return this builder for chaining
     * @throws IllegalArgumentException if project is null
     */
    public TestProjectScopeBuilder withProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        this.project = project;
        return this;
    }

    /**
     * Set a CompilationUnitContextLoader to seed in the scope.
     *
     * @param loader the loader instance (mock or real)
     * @return this builder for chaining
     * @throws IllegalArgumentException if loader is null
     */
    public TestProjectScopeBuilder withLoader(
            CompilationUnitContextLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("Loader cannot be null");
        }
        this.loader = loader;
        return this;
    }

    /**
     * Build the scope context, enter it, run the action, and exit.
     *
     * <p>This is the primary test entry point — the scope is active
     * for the duration of the callback and cleaned up afterward.
     *
     * @param action the test code to run within scope
     */
    public void withScope(Runnable action) {
        var ctx = build();
        try (var handle = ProjectScope.enter(ctx.projectId(), ctx)) {
            action.run();
        }
    }

    /**
     * Build the scope context, enter it, run the supplier, exit,
     * and return the result.
     *
     * @param action the test code to run within scope
     * @param <T>    the return type
     * @return the result of the supplier
     */
    public <T> T withScope(Supplier<T> action) {
        var ctx = build();
        try (var handle = ProjectScope.enter(ctx.projectId(), ctx)) {
            return action.get();
        }
    }

    /**
     * Build a {@link ProjectScope.ScopeContext} with the configured
     * settings, seeded and ready to use.
     *
     * <p>If no project was specified, a temporary directory project
     * is created automatically.
     *
     * <p>Prefer {@link #withScope(Runnable)} which handles
     * enter/exit automatically.
     *
     * @return a seeded ScopeContext
     */
    public ProjectScope.ScopeContext build() {
        ensureInjectorInitialized();

        Project projectToUse = this.project;
        if (projectToUse == null) {
            projectToUse = TestProjectBuilder.inTempDirectory();
        }

        ProjectId id = ProjectId.of(projectToUse.getProjectDir());
        var ctx = new ProjectScope.ScopeContext(id);

        ctx.seed(Project.class, projectToUse);

        if (loader != null) {
            ctx.seed(CompilationUnitContextLoader.class, loader);
        }

        return ctx;
    }

    private static void ensureInjectorInitialized() {
        if (!BlueJInjector.isInitialized()) {
            BlueJInjector.initialize(new ProjectScopeModule());
        }
    }
}
