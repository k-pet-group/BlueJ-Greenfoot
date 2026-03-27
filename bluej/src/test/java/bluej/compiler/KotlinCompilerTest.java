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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for KotlinCompiler — verifies that the K2 compiler wrapper correctly
 * compiles Kotlin source files, produces diagnostics, and respects compile types.
 */
public class KotlinCompilerTest
{
    private KotlinCompiler compiler;
    private Path tempDir;
    private Path sourceDir;

    @Before
    public void setUp() throws IOException
    {
        compiler = new KotlinCompiler();
        tempDir = Files.createTempDirectory("kotlin-compiler-test");
        sourceDir = Files.createTempDirectory("kotlin-source-test");
        compiler.setDestDir(tempDir.toFile());
        // The Kotlin compiler needs kotlin-stdlib on the classpath to compile
        // even basic Kotlin code. Extract it from the test runtime classpath.
        compiler.setClasspath(getKotlinStdlibFromTestClasspath());
    }

    /**
     * Finds kotlin-stdlib JAR(s) on the current test classpath, so that the
     * KotlinCompiler can resolve standard library types during compilation.
     */
    private static List<File> getKotlinStdlibFromTestClasspath()
    {
        String cp = System.getProperty("java.class.path");
        if (cp == null)
        {
            return Collections.emptyList();
        }
        return Arrays.stream(cp.split(File.pathSeparator))
                .filter(entry -> {
                    String name = new File(entry).getName();
                    return name.startsWith("kotlin-stdlib");
                })
                .map(File::new)
                .collect(Collectors.toList());
    }

    @After
    public void tearDown()
    {
        deleteRecursive(tempDir.toFile());
        deleteRecursive(sourceDir.toFile());
    }

    @Test
    public void testCompileValidKotlinFile() throws IOException
    {
        // Write a simple Kotlin source file
        Path sourceFile = sourceDir.resolve("Hello.kt");
        Files.writeString(sourceFile, """
            class Hello {
                fun greet(): String {
                    return "Hello, World!"
                }
            }
            """);

        TestObserver observer = new TestObserver();
        boolean success = compiler.compile(
                new File[]{sourceFile.toFile()},
                observer, false, Collections.emptyList(),
                StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Compilation of valid Kotlin file should succeed", success);
        assertEquals("No errors expected", 0, observer.errors.size());

        // Verify .class file was created
        File classFile = new File(tempDir.toFile(), "Hello.class");
        assertTrue("Hello.class should be generated", classFile.exists());
    }

    @Test
    public void testCompileSyntaxErrorProducesDiagnostic() throws IOException
    {
        Path sourceFile = sourceDir.resolve("Bad.kt");
        Files.writeString(sourceFile, """
            class Bad {
                fun broken( {
                    return "missing paren"
                }
            }
            """);

        TestObserver observer = new TestObserver();
        boolean success = compiler.compile(
                new File[]{sourceFile.toFile()},
                observer, false, Collections.emptyList(),
                StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE);

        assertFalse("Compilation with syntax error should fail", success);
        assertTrue("At least one error diagnostic expected", observer.errors.size() > 0);

        // Verify the diagnostic has Kotlin origin
        Diagnostic firstError = observer.errors.get(0);
        assertEquals("Diagnostic should have KOTLIN origin",
                "kotlin", firstError.getOrigin());
        assertEquals("Diagnostic should be ERROR type",
                Diagnostic.ERROR, firstError.getType());
    }

    @Test
    public void testCompileErrorHasLineNumber() throws IOException
    {
        Path sourceFile = sourceDir.resolve("LineError.kt");
        Files.writeString(sourceFile, """
            class LineError {
                fun test() {
                    val x: Int = "not an int"
                }
            }
            """);

        TestObserver observer = new TestObserver();
        compiler.compile(
                new File[]{sourceFile.toFile()},
                observer, false, Collections.emptyList(),
                StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Error diagnostics expected", observer.errors.size() > 0);

        Diagnostic error = observer.errors.get(0);
        assertTrue("Error should have a valid line number (> 0)",
                error.getStartLine() > 0);
    }

    @Test
    public void testCompileMultipleFiles() throws IOException
    {
        Path file1 = sourceDir.resolve("Person.kt");
        Files.writeString(file1, """
            open class Person(val name: String) {
                fun greet(): String = "Hello, $name"
            }
            """);

        Path file2 = sourceDir.resolve("Student.kt");
        Files.writeString(file2, """
            class Student(name: String, val grade: Int) : Person(name) {
                fun info(): String = "${greet()} (grade $grade)"
            }
            """);

        // Need to set classpath to include destDir + kotlin-stdlib
        List<File> cp = new ArrayList<>(getKotlinStdlibFromTestClasspath());
        cp.add(tempDir.toFile());
        compiler.setClasspath(cp);

        TestObserver observer = new TestObserver();
        boolean success = compiler.compile(
                new File[]{file1.toFile(), file2.toFile()},
                observer, false, Collections.emptyList(),
                StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE);

        assertTrue("Compilation of multiple valid files should succeed", success);

        File personClass = new File(tempDir.toFile(), "Person.class");
        File studentClass = new File(tempDir.toFile(), "Student.class");
        assertTrue("Person.class should be generated", personClass.exists());
        assertTrue("Student.class should be generated", studentClass.exists());
    }

    @Test
    public void testErrorCheckOnlyDoesNotRetainClasses() throws IOException
    {
        Path sourceFile = sourceDir.resolve("Temp.kt");
        Files.writeString(sourceFile, """
            class Temp {
                fun value(): Int = 42
            }
            """);

        TestObserver observer = new TestObserver();
        boolean success = compiler.compile(
                new File[]{sourceFile.toFile()},
                observer, false, Collections.emptyList(),
                StandardCharsets.UTF_8, CompileType.ERROR_CHECK_ONLY);

        assertTrue("Error-check-only compilation should succeed for valid code", success);

        // The destDir should NOT have the class file — a temp dir was used instead
        File classFile = new File(tempDir.toFile(), "Temp.class");
        assertFalse("ERROR_CHECK_ONLY should not write classes to destDir", classFile.exists());
    }

    @Test
    public void testDebugAndDeprecationDefaults()
    {
        assertTrue("Debug should be enabled by default", compiler.isDebug());
        assertTrue("Deprecation should be enabled by default", compiler.isDeprecation());
    }

    // ---- Test infrastructure ----

    /**
     * Simple CompileObserver that collects diagnostics.
     */
    private static class TestObserver implements CompileObserver
    {
        final List<Diagnostic> errors = new ArrayList<>();
        final List<Diagnostic> warnings = new ArrayList<>();
        final List<Diagnostic> notes = new ArrayList<>();
        boolean compilationStarted = false;
        boolean compilationEnded = false;
        boolean wasSuccessful = false;

        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason,
                CompileType type, int compilationSequence)
        {
            compilationStarted = true;
        }

        @Override
        public void compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            if (diagnostic.getType() == Diagnostic.ERROR)
            {
                errors.add(diagnostic);
            }
            else if (diagnostic.getType() == Diagnostic.WARNING)
            {
                warnings.add(diagnostic);
            }
            else
            {
                notes.add(diagnostic);
            }
        }

        @Override
        public void endCompile(CompileInputFile[] sources, boolean succesful,
                CompileType type, int compilationSequence)
        {
            compilationEnded = true;
            wasSuccessful = succesful;
        }
    }

    private static void deleteRecursive(File file)
    {
        if (file.isDirectory())
        {
            File[] children = file.listFiles();
            if (children != null)
            {
                for (File child : children)
                {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
