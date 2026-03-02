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
import bluej.di.scopes.ProjectScope;
import bluej.parser.context.CompilationUnitContextLoader;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link TestProjectEnvironment} and its DI integration.
 *
 * <p>These tests verify that the test DI environment correctly
 * wires scoped dependencies and that they are resolvable via
 * {@link BlueJInjector#getInstance}.
 */
public class TestProjectEnvironmentTest {

    @Rule
    public TestProjectEnvironment env = TestProjectEnvironment.withDefaults();

    // =========================================================================
    // Scope lifecycle
    // =========================================================================

    @Test
    public void injectorIsInitialised() {
        assertTrue("Injector should be initialised by the rule",
            BlueJInjector.isInitialized());
    }

    @Test
    public void scopeIsInactiveOutsideOpenProject() {
        assertNull("No scope should be active outside openProject",
            ProjectScope.current());
    }

    // =========================================================================
    // Loader resolution via DI
    // =========================================================================

    @Test
    public void fixtureProject_loaderIsResolvableFromDI() {
        env.openProject().fromFixture("simple").withScope(() -> {
            CompilationUnitContextLoader loader =
                BlueJInjector.getInstance(CompilationUnitContextLoader.class);
            assertNotNull("Loader should be resolvable from DI", loader);
        });
    }

    @Test
    public void tempProject_loaderIsResolvableFromDI() {
        env.openProject().inTempDirectory().withScope(() -> {
            CompilationUnitContextLoader loader =
                BlueJInjector.getInstance(CompilationUnitContextLoader.class);
            assertNotNull("Loader should be resolvable from DI", loader);
        });
    }

    @Test
    public void customLoader_isReturnedByDI() {
        CompilationUnitContextLoader custom =
            new CompilationUnitContextLoader(
                new bluej.parser.context.TestClassLoaderProvider(
                    new java.io.File(System.getProperty("java.io.tmpdir"))));

        env.openProject().inTempDirectory().withLoader(custom).withScope(() -> {
            CompilationUnitContextLoader fromDI =
                BlueJInjector.getInstance(CompilationUnitContextLoader.class);
            assertSame("Custom loader should be returned by DI",
                custom, fromDI);
        });
    }

    // =========================================================================
    // Multi-project scope isolation
    // =========================================================================

    @Test
    public void twoProjects_getDifferentLoaders() {
        env.openProject().fromFixture("simple").withScope(() -> {
            CompilationUnitContextLoader loader1 =
                BlueJInjector.getInstance(CompilationUnitContextLoader.class);

            env.openProject().fromFixture("simple").withScope(() -> {
                CompilationUnitContextLoader loader2 =
                    BlueJInjector.getInstance(CompilationUnitContextLoader.class);

                assertNotSame(
                    "Different projects should get different loaders",
                    loader1, loader2);
            });

            // Outer scope restored
            CompilationUnitContextLoader stillLoader1 =
                BlueJInjector.getInstance(CompilationUnitContextLoader.class);
            assertSame("Outer scope should be restored", loader1, stillLoader1);
        });
    }

    @Test
    public void twoProjects_haveDifferentScopeContexts() {
        env.openProject().fromFixture("simple").withScope(() -> {
            ProjectScope.ScopeContext ctx1 = ProjectScope.current();
            assertNotNull(ctx1);

            env.openProject().fromFixture("simple").withScope(() -> {
                ProjectScope.ScopeContext ctx2 = ProjectScope.current();
                assertNotNull(ctx2);
                assertNotSame("Nested project has different context",
                    ctx1, ctx2);
            });

            assertSame("Outer context restored",
                ctx1, ProjectScope.current());
        });
    }

    // =========================================================================
    // Scope inactive after withScope exits
    // =========================================================================

    @Test
    public void scopeIsInactiveAfterWithScopeCompletes() {
        env.openProject().fromFixture("simple").withScope(() -> {
            assertNotNull("Scope active inside", ProjectScope.current());
        });

        assertNull("Scope inactive after withScope",
            ProjectScope.current());
    }
}
