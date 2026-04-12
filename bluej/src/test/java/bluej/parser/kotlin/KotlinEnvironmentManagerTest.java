/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin;

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinEnvironmentManager — singleton lifecycle, PSI factory,
 * and thread safety.
 */
public class KotlinEnvironmentManagerTest
{
    @Test
    public void testGetEnvironmentReturnsNonNull()
    {
        KotlinCoreEnvironment env = KotlinEnvironmentManager.getEnvironment();
        assertNotNull("getEnvironment() should return non-null", env);
    }

    @Test
    public void testGetEnvironmentReturnsSameInstance()
    {
        KotlinCoreEnvironment env1 = KotlinEnvironmentManager.getEnvironment();
        KotlinCoreEnvironment env2 = KotlinEnvironmentManager.getEnvironment();
        assertSame("Subsequent calls should return the same instance", env1, env2);
    }

    @Test
    public void testGetProjectReturnsNonNull()
    {
        Project project = KotlinEnvironmentManager.getProject();
        assertNotNull("getProject() should return non-null", project);
    }

    @Test
    public void testGetPsiFactoryReturnsNonNull()
    {
        KtPsiFactory factory = KotlinEnvironmentManager.getPsiFactory();
        assertNotNull("getPsiFactory() should return non-null", factory);
    }

    @Test
    public void testPsiFactoryCanParseFile()
    {
        KtPsiFactory factory = KotlinEnvironmentManager.getPsiFactory();
        KtFile ktFile = factory.createFile("class Foo { fun bar() {} }");
        assertNotNull("createFile should return non-null", ktFile);
        assertFalse("Parsed file should have declarations",
            ktFile.getDeclarations().isEmpty());
    }

    @Test
    public void testPsiFactoryParsesEmptyFile()
    {
        KtPsiFactory factory = KotlinEnvironmentManager.getPsiFactory();
        KtFile ktFile = factory.createFile("");
        assertNotNull(ktFile);
        assertTrue("Empty file should have no declarations",
            ktFile.getDeclarations().isEmpty());
    }

    @Test
    public void testIsInitializedAfterFirstCall()
    {
        // Force initialization
        KotlinEnvironmentManager.getEnvironment();
        assertTrue("Should be initialized after getEnvironment()",
            KotlinEnvironmentManager.isInitialized());
    }

    @Test
    public void testConcurrentAccessIsSafe() throws Exception
    {
        // Launch multiple threads that call getProject() simultaneously
        int threadCount = 4;
        Thread[] threads = new Thread[threadCount];
        Project[] results = new Project[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++)
        {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    results[idx] = KotlinEnvironmentManager.getProject();
                } catch (Exception e) {
                    exceptions[idx] = e;
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join(10000); // 10s timeout
        }

        for (int i = 0; i < threadCount; i++)
        {
            assertNull("Thread " + i + " should not throw", exceptions[i]);
            assertNotNull("Thread " + i + " should get a project", results[i]);
            assertSame("All threads should get same Project instance",
                results[0], results[i]);
        }
    }
}
