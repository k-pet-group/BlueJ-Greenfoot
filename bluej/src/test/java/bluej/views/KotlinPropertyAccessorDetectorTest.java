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
package bluej.views;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import bluej.compiler.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link KotlinPropertyAccessorDetector}.
 */
public class KotlinPropertyAccessorDetectorTest
{
    private KotlinCompiler compiler;
    private Path outputDir;
    private Path sourceDir;

    @Before
    public void setUp() throws IOException
    {
        compiler = new KotlinCompiler();
        outputDir = Files.createTempDirectory("kotlin-detector-test-out");
        sourceDir = Files.createTempDirectory("kotlin-detector-test-src");
        compiler.setDestDir(outputDir.toFile());
        compiler.setClasspath(getKotlinStdlibFromTestClasspath());
    }

    @After
    public void tearDown()
    {
        deleteRecursive(outputDir.toFile());
        deleteRecursive(sourceDir.toFile());
    }

    @Test
    public void testValProperty_detectsGetterOnly()
    {
        Set<String> names = compileAndDetect("ValProp.kt", """
            class ValProp {
                val name: String = "Alice"
            }
            """, "ValProp");

        assertTrue("Getter for val property should be detected",
                names.contains("getName"));
        assertFalse("val property should not generate a setter",
                names.contains("setName"));
    }

    @Test
    public void testVarProperty_detectsGetterAndSetter()
    {
        Set<String> names = compileAndDetect("VarProp.kt", """
            class VarProp {
                var age: Int = 0
            }
            """, "VarProp");

        assertTrue("Getter for var property should be detected",
                names.contains("getAge"));
        assertTrue("Setter for var property should be detected",
                names.contains("setAge"));
    }

    @Test
    public void testMultipleProperties_detectsAllAccessors()
    {
        Set<String> names = compileAndDetect("MultiProp.kt", """
            class MultiProp {
                val name: String = "Bob"
                var age: Int = 25
            }
            """, "MultiProp");

        assertTrue(names.contains("getName"));
        assertFalse("val should not have setter", names.contains("setName"));
        assertTrue(names.contains("getAge"));
        assertTrue(names.contains("setAge"));
        assertEquals(3, names.size());
    }

    @Test
    public void testBooleanIsVar_usesIsPrefixConvention()
    {
        Set<String> names = compileAndDetect("BoolProp.kt", """
            class BoolProp {
                var isActive: Boolean = false
            }
            """, "BoolProp");

        assertTrue("Boolean is-prefix getter should be the property name itself",
                names.contains("isActive"));
        assertFalse("Should NOT generate getIsActive",
                names.contains("getIsActive"));
        assertTrue("Boolean is-prefix setter should be setActive",
                names.contains("setActive"));
        assertFalse("Should NOT generate setIsActive",
                names.contains("setIsActive"));
    }

    @Test
    public void testBooleanIsVal_detectsGetterOnly()
    {
        Set<String> names = compileAndDetect("BoolValProp.kt", """
            class BoolValProp {
                val isReady: Boolean = true
            }
            """, "BoolValProp");

        assertTrue(names.contains("isReady"));
        assertFalse("val should not have setter", names.contains("setReady"));
    }

    @Test
    public void testExplicitMethod_notDetected()
    {
        Set<String> names = compileAndDetect("ExplicitMethod.kt", """
            class ExplicitMethod {
                val name: String = "test"

                fun getFullName(): String {
                    return "Mr. $name"
                }
            }
            """, "ExplicitMethod");

        assertTrue("Property accessor getName should be detected",
                names.contains("getName"));
        assertFalse("Explicit method getFullName should NOT be detected",
                names.contains("getFullName"));
    }

    @Test
    public void testJavaClass_returnsEmptySet()
    {
        Set<String> names = KotlinPropertyAccessorDetector
                .getAutoGeneratedAccessorNames(String.class);

        assertTrue(names.isEmpty());
    }

    @Test
    public void testJavaClassWithGetters_returnsEmptySet()
    {
        Set<String> names = KotlinPropertyAccessorDetector
                .getAutoGeneratedAccessorNames(java.util.Date.class);

        assertTrue("Java class should return empty set even with getter/setter methods",
                names.isEmpty());
    }

    @Test
    public void testCustomGetterBody_stillDetected()
    {
        Set<String> names = compileAndDetect("CustomGetter.kt", """
            class CustomGetter {
                var age: Int = 0
                    get() = field + 1
            }
            """, "CustomGetter");

        assertTrue(names.contains("getAge"));
        assertTrue(names.contains("setAge"));
    }

    @Test
    public void testTopLevelProperties_detectedInFileFacade()
    {
        Set<String> names = compileAndDetect("TopLevel.kt", """
            var topName: String = "hello"
            val topCount: Int = 42
            """, "TopLevelKt");

        assertTrue("Top-level var getter should be detected",
                names.contains("getTopName"));
        assertTrue("Top-level var setter should be detected",
                names.contains("setTopName"));
        assertTrue("Top-level val getter should be detected",
                names.contains("getTopCount"));
        assertFalse("Top-level val should NOT have setter",
                names.contains("setTopCount"));
    }

    @Test
    public void testObjectDeclaration_detectsAccessors()
    {
        Set<String> names = compileAndDetect("MySingleton.kt", """
            object MySingleton {
                var instanceName: String = "singleton"
                val count: Int = 0
            }
            """, "MySingleton");

        assertTrue(names.contains("getInstanceName"));
        assertTrue(names.contains("setInstanceName"));
        assertTrue(names.contains("getCount"));
        assertFalse(names.contains("setCount"));
    }

    @Test
    public void testCompanionObjectProperties_detectedOnCompanion()
    {
        // Companion properties live on the Companion class, not the outer class.
        // The outer class has bridge methods which are separate.
        compileAndDetect("WithCompanion.kt", """
            class WithCompanion {
                var instanceProp: String = ""

                companion object {
                    var companionProp: String = "hello"
                }
            }
            """, "WithCompanion");

        // Now check the companion class
        Set<String> companionNames = loadAndDetect("WithCompanion$Companion");

        assertTrue("Companion property getter should be detected",
                companionNames.contains("getCompanionProp"));
        assertTrue("Companion property setter should be detected",
                companionNames.contains("setCompanionProp"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReturnedSet_isUnmodifiable()
    {
        Set<String> names = KotlinPropertyAccessorDetector
                .getAutoGeneratedAccessorNames(String.class);

        names.add("test");
    }

    @Test
    public void testMixedPropertiesAndMethods_onlyAccessorsDetected()
    {
        Set<String> names = compileAndDetect("Mixed.kt", """
            class Mixed {
                var name: String = ""
                val count: Int = 0
                var isVisible: Boolean = true

                fun doSomething(): String = "result"
                fun calculate(x: Int): Int = x * 2
                fun getName2(): String = name + "!"
            }
            """, "Mixed");

        // Property accessors should be detected
        assertTrue(names.contains("getName"));
        assertTrue(names.contains("setName"));
        assertTrue(names.contains("getCount"));
        assertTrue(names.contains("isVisible"));
        assertTrue(names.contains("setVisible"));

        // Explicit methods should NOT be detected
        assertFalse(names.contains("doSomething"));
        assertFalse(names.contains("calculate"));
        assertFalse(names.contains("getName2"));

        assertEquals(5, names.size());
    }

    // --- Test infrastructure ---

    /**
     * Compiles a Kotlin source file and runs the detector on the resulting class.
     */
    private Set<String> compileAndDetect(String fileName, String source, String className)
    {
        try {
            Path sourceFile = sourceDir.resolve(fileName);
            Files.writeString(sourceFile, source);

            SilentObserver observer = new SilentObserver();
            boolean success = compiler.compile(
                    new File[]{sourceFile.toFile()},
                    observer, false, Collections.emptyList(),
                    StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE);

            assertTrue("Kotlin compilation should succeed for: " + fileName
                    + ". Errors: " + observer.errors, success);

            return loadAndDetect(className);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to compile and detect: " + fileName, e);
        }
    }

    /**
     * Loads an already-compiled class from the output directory and runs the detector.
     */
    private Set<String> loadAndDetect(String className)
    {
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{outputDir.toUri().toURL()},
                    getClass().getClassLoader());

            Class<?> cl = classLoader.loadClass(className);
            return KotlinPropertyAccessorDetector.getAutoGeneratedAccessorNames(cl);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load and detect: " + className, e);
        }
    }

    private static List<File> getKotlinStdlibFromTestClasspath()
    {
        String cp = System.getProperty("java.class.path");
        if (cp == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(cp.split(File.pathSeparator))
                .filter(entry -> new File(entry).getName().startsWith("kotlin-stdlib"))
                .map(File::new)
                .collect(Collectors.toList());
    }

    private static class SilentObserver implements CompileObserver
    {
        final List<Diagnostic> errors = new ArrayList<>();

        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason,
                CompileType type, int compilationSequence)
        {
        }

        @Override
        public void compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            if (diagnostic.getType() == Diagnostic.ERROR) {
                errors.add(diagnostic);
            }
        }

        @Override
        public void endCompile(CompileInputFile[] sources, boolean succesful,
                CompileType type, int compilationSequence)
        {
        }
    }

    private static void deleteRecursive(File file)
    {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
