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
package bluej.di.modules;

import bluej.di.scopes.ProjectScope;
import bluej.di.scopes.ProjectScoped;
import bluej.parser.context.CompilationUnitContextLoader;
import bluej.pkgmgr.Project;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Guice module that configures the {@link ProjectScope} binding.
 *
 * <p>Bindings:
 * <ul>
 *   <li>{@link Project} — seeded into the scope by
 *       {@link bluej.pkgmgr.ProjectFactory#open}</li>
 *   <li>{@link CompilationUnitContextLoader} — created from the
 *       seeded {@code Project} (or from a test-seeded override)</li>
 * </ul>
 *
 * @see ProjectScope
 * @see ProjectScoped
 */
public class ProjectScopeModule extends AbstractModule {

    private final ProjectScope projectScope = new ProjectScope();

    @Override
    protected void configure() {
        bindScope(ProjectScoped.class, projectScope);

        bind(Project.class)
            .toProvider(ProjectScopeModule::provideProject)
            .in(ProjectScoped.class);

        bind(CompilationUnitContextLoader.class)
            .toProvider(ProjectScopeModule::provideContextLoader)
            .in(ProjectScoped.class);
    }

    /**
     * Returns the {@link ProjectScope} instance used by this module.
     */
    public @NotNull ProjectScope getProjectScope() {
        return projectScope;
    }

    // ── providers ────────────────────────────────────────────────────

    /**
     * Provides the {@link Project} from the current scope context.
     * The instance is seeded during project opening.
     */
    private static @NotNull Project provideProject() {
        var ctx = requireContext("Project");
        Project project = ctx.get(Key.get(Project.class));
        if (project == null) {
            throw new IllegalStateException(
                "Project has not been seeded into the current scope. " +
                "This usually means a @ProjectScoped dependency was resolved " +
                "during Project construction, which is not supported. " +
                "Use @Inject fields instead (populated after construction by " +
                "ProjectFactory.open).");
        }
        return project;
    }

    /**
     * Provides the {@link CompilationUnitContextLoader}.
     *
     * <p>If a loader has been manually seeded (e.g. by test code),
     * that instance is returned.  Otherwise a new one is created
     * from the seeded {@link Project}.
     */
    private static @NotNull CompilationUnitContextLoader provideContextLoader() {
        var ctx = requireContext("CompilationUnitContextLoader");

        CompilationUnitContextLoader seeded =
            ctx.get(Key.get(CompilationUnitContextLoader.class));
        if (seeded != null) {
            return seeded;
        }

        Project project = ctx.get(Key.get(Project.class));
        if (project == null) {
            throw new IllegalStateException(
                "Project has not been seeded — " +
                "cannot create CompilationUnitContextLoader.");
        }
        return new CompilationUnitContextLoader(project);
    }

    // ── helpers ──────────────────────────────────────────────────────

    private static @NotNull ProjectScope.ScopeContext requireContext(@NotNull String what) {
        ProjectScope.ScopeContext ctx = ProjectScope.current();
        if (ctx == null) {
            throw new IllegalStateException(
                "Attempted to inject " + what +
                " outside of project scope. " +
                "Ensure this code runs inside an active project scope.");
        }
        return ctx;
    }
}
