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
import bluej.parser.context.CompilationUnitContextLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 * Tests for {@link TestProjectScopeBuilder}.
 * 
 * <p>These tests verify the builder's fluent API and scope management.
 * Note: Tests requiring real Project instances are limited because
 * Project.openProject requires the full BlueJ environment.
 */
public class TestProjectScopeBuilderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        // Ensure injector is reset for clean test isolation
        BlueJInjectorTestUtils.reset();
    }
    
    @After
    public void tearDown() {
        BlueJInjectorTestUtils.reset();
    }

    // =========================================================================
    // Parameter validation tests
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void withProject_nullThrowsException() {
        TestProjectScopeBuilder.create().withProject(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void withLoader_nullThrowsException() {
        TestProjectScopeBuilder.create().withLoader(null);
    }

    // =========================================================================
    // Fluent API tests
    // =========================================================================

    @Test
    public void create_returnsNewBuilder() {
        TestProjectScopeBuilder builder = TestProjectScopeBuilder.create();
        assertNotNull("Builder should not be null", builder);
    }

    // =========================================================================
    // TestProjectScope convenience methods validation tests
    // =========================================================================

    @Test
    public void testProjectScope_builder_returnsNonNull() {
        TestProjectScopeBuilder builder = TestProjectScope.builder();
        assertNotNull("Builder should not be null", builder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProjectScope_setup_nullProjectThrowsException() {
        TestProjectScope.setup(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProjectScope_setup_nullProjectWithLoaderThrowsException() {
        // Create a simple stub loader
        CompilationUnitContextLoader stubLoader = new StubCompilationUnitContextLoader();
        TestProjectScope.setup(null, stubLoader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProjectScope_setup_nullLoaderThrowsException() {
        // Need to create a real project for this test
        // Since we can't, we test via builder directly
        TestProjectScopeBuilder.create().withLoader(null);
    }

    /**
     * Minimal stub for CompilationUnitContextLoader to use in tests.
     */
    private static class StubCompilationUnitContextLoader extends CompilationUnitContextLoader {
        public StubCompilationUnitContextLoader() {
            super(null); // Pass null project - just for testing parameter validation
        }
    }
}
