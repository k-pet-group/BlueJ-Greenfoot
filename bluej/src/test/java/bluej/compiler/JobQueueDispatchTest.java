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
package bluej.compiler;

import java.io.File;
import java.lang.reflect.Method;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the JobQueue's compiler dispatch logic — verifies that .kt files
 * are routed to KotlinCompiler and .java files to CompilerAPICompiler.
 */
public class JobQueueDispatchTest
{
    @Test
    public void testHasKotlinSourcesWithKtFile() throws Exception
    {
        CompileInputFile ktFile = new CompileInputFile(
                new File("Hello.kt"), new File("Hello.kt"));

        boolean result = invokeHasKotlinSources(new CompileInputFile[]{ktFile});
        assertTrue(".kt file should be detected as Kotlin source", result);
    }

    @Test
    public void testHasKotlinSourcesWithJavaFile() throws Exception
    {
        CompileInputFile javaFile = new CompileInputFile(
                new File("Hello.java"), new File("Hello.java"));

        boolean result = invokeHasKotlinSources(new CompileInputFile[]{javaFile});
        assertFalse(".java file should not be detected as Kotlin source", result);
    }

    @Test
    public void testHasKotlinSourcesMixedFiles() throws Exception
    {
        CompileInputFile javaFile = new CompileInputFile(
                new File("Hello.java"), new File("Hello.java"));
        CompileInputFile ktFile = new CompileInputFile(
                new File("World.kt"), new File("World.kt"));

        boolean result = invokeHasKotlinSources(new CompileInputFile[]{javaFile, ktFile});
        assertTrue("Array with any .kt file should be detected as Kotlin", result);
    }

    @Test
    public void testHasKotlinSourcesEmptyArray() throws Exception
    {
        boolean result = invokeHasKotlinSources(new CompileInputFile[]{});
        assertFalse("Empty array should not be detected as Kotlin", result);
    }

    @Test
    public void testHasKotlinSourcesWithPath() throws Exception
    {
        CompileInputFile ktFile = new CompileInputFile(
                new File("/path/to/project/Hello.kt"), new File("/path/to/project/Hello.kt"));

        boolean result = invokeHasKotlinSources(new CompileInputFile[]{ktFile});
        assertTrue(".kt file with full path should be detected", result);
    }

    /**
     * Invoke the private static hasKotlinSources method via reflection for testing.
     */
    private static boolean invokeHasKotlinSources(CompileInputFile[] sources) throws Exception
    {
        Method method = JobQueue.class.getDeclaredMethod("hasKotlinSources", CompileInputFile[].class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, (Object) sources);
    }
}
