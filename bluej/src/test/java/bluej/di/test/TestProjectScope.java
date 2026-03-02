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

import bluej.di.BlueJInjectorTestUtils;
import bluej.di.scopes.ProjectScope;
import bluej.parser.context.CompilationUnitContextLoader;
import bluej.pkgmgr.Project;

/**
 * Test utilities for setting up project scope in unit tests.
 *
 * <h3>Quick Start</h3>
 * <pre>
 * TestProjectScope.builder().withScope(() -&gt; {
 *     // scope active — run test
 * });
 * </pre>
 *
 * <h3>With Mocks</h3>
 * <pre>
 * TestProjectScope.builder()
 *         .withProject(mockProject)
 *         .withLoader(mockLoader)
 *         .withScope(() -&gt; {
 *             // test with mocks
 *         });
 * </pre>
 *
 * @see TestProjectScopeBuilder
 * @see TestProjectBuilder
 */
public final class TestProjectScope {

    private TestProjectScope() { /* utility class */ }

    /**
     * Creates a new builder for flexible test scope configuration.
     *
     * @return a new {@link TestProjectScopeBuilder}
     */
    public static TestProjectScopeBuilder builder() {
        return TestProjectScopeBuilder.create();
    }

    /**
     * Sets up a test scope with the specified project and loader.
     *
     * @param project a Project instance (mock or real)
     * @param loader  a CompilationUnitContextLoader instance
     * @return a seeded {@link ProjectScope.ScopeContext}
     * @throws IllegalArgumentException if project or loader is null
     */
    public static ProjectScope.ScopeContext setup(
            Project project,
            CompilationUnitContextLoader loader) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        if (loader == null) {
            throw new IllegalArgumentException("Loader cannot be null");
        }
        return builder()
            .withProject(project)
            .withLoader(loader)
            .build();
    }

    /**
     * Sets up a test scope with only the specified project.
     *
     * @param project a Project instance (mock or real)
     * @return a seeded {@link ProjectScope.ScopeContext}
     * @throws IllegalArgumentException if project is null
     */
    public static ProjectScope.ScopeContext setup(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        return builder()
            .withProject(project)
            .build();
    }

    /**
     * Sets up a test scope with an auto-created temporary project.
     *
     * @return a seeded {@link ProjectScope.ScopeContext}
     */
    public static ProjectScope.ScopeContext setup() {
        return builder().build();
    }

    /**
     * Resets the injector state for testing.
     * Call in {@code @Before} / {@code @After} methods.
     */
    public static void resetInjector() {
        BlueJInjectorTestUtils.reset();
    }
}
