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
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import bluej.compiler.*;
import bluej.parser.kotlin.KotlinInfoParser;
import bluej.parser.symtab.ClassInfo;
import bluej.views.KotlinCompanionDetector.CompanionInfo;
import bluej.views.ViewFilter.StaticOrInstance;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link KotlinCompanionDetector} and the surfacing of Kotlin
 * companion-object methods as static-style operations on the enclosing class
 * (via {@link View} and {@link ViewFilter}).
 */
public class KotlinCompanionDetectorTest
{
    private KotlinCompiler compiler;
    private Path outputDir;
    private Path sourceDir;
    private URLClassLoader classLoader;

    @Before
    public void setUp() throws IOException
    {
        compiler = new KotlinCompiler();
        outputDir = Files.createTempDirectory("kotlin-companion-test-out");
        sourceDir = Files.createTempDirectory("kotlin-companion-test-src");
        compiler.setDestDir(outputDir.toFile());
        compiler.setClasspath(getKotlinStdlibFromTestClasspath());
    }

    @After
    public void tearDown() throws IOException
    {
        if (classLoader != null) {
            classLoader.close();
        }
        deleteRecursive(outputDir.toFile());
        deleteRecursive(sourceDir.toFile());
    }

    // --- Detector ---

    @Test
    public void testDefaultCompanion_detected()
    {
        Class<?> cl = compileAndLoad("WithCompanion.kt", """
            class WithCompanion {
                companion object {
                    fun bar(): Int = 42
                }
            }
            """, "WithCompanion");

        CompanionInfo info = KotlinCompanionDetector.getCompanion(cl);
        assertNotNull("Companion should be detected", info);
        assertEquals("Companion", info.getName());
        assertEquals("Companion", info.getCompanionClass().getSimpleName());
    }

    @Test
    public void testNamedCompanion_detected()
    {
        Class<?> cl = compileAndLoad("NamedCompanion.kt", """
            class NamedCompanion {
                companion object Factory {
                    fun make(): NamedCompanion = NamedCompanion()
                }
            }
            """, "NamedCompanion");

        CompanionInfo info = KotlinCompanionDetector.getCompanion(cl);
        assertNotNull(info);
        assertEquals("Factory", info.getName());
        assertEquals("Factory", info.getCompanionClass().getSimpleName());
    }

    @Test
    public void testNoCompanion_returnsNull()
    {
        Class<?> cl = compileAndLoad("Plain.kt", """
            class Plain {
                fun hello(): String = "hi"
            }
            """, "Plain");

        assertNull(KotlinCompanionDetector.getCompanion(cl));
    }

    @Test
    public void testJavaClass_returnsNull()
    {
        assertNull(KotlinCompanionDetector.getCompanion(String.class));
    }

    // --- View / ViewFilter integration ---

    @Test
    public void testCompanionMethodSurfacedAsStatic()
    {
        Class<?> cl = compileAndLoad("Surfaced.kt", """
            class Surfaced {
                companion object {
                    fun bar(): Int = 42
                }
            }
            """, "Surfaced");

        MethodView bar = findMethod(cl, "bar");
        assertNotNull("Companion method bar() should be surfaced on the class", bar);
        assertTrue("bar() should be flagged as a companion method", bar.isKotlinCompanionMethod());
        assertEquals("Companion", bar.getCompanionReceiverName());
        // Callable without an instance, so it reports as static here...
        assertTrue("Companion method is callable in a static context", bar.isStatic());
        // ...while the reflected modifiers stay truthful (the JVM method is non-static).
        assertFalse("Reflected modifiers remain non-static",
                Modifier.isStatic(bar.getModifiers()));
    }

    @Test
    public void testCompanionMethodInStaticFilterBucketOnly()
    {
        Class<?> cl = compileAndLoad("Filtered.kt", """
            class Filtered {
                companion object {
                    fun bar(): Int = 1
                }
            }
            """, "Filtered");

        MethodView bar = findMethod(cl, "bar");
        ViewFilter staticFilter = new ViewFilter(StaticOrInstance.STATIC, null);
        ViewFilter instanceFilter = new ViewFilter(StaticOrInstance.INSTANCE, null);

        assertTrue("Companion method belongs in the STATIC bucket", staticFilter.test(bar));
        assertFalse("Companion method must not appear in the INSTANCE bucket",
                instanceFilter.test(bar));
    }

    @Test
    public void testJvmStaticNotDuplicated()
    {
        Class<?> cl = compileAndLoad("JvmStaticCompanion.kt", """
            class JvmStaticCompanion {
                companion object {
                    @JvmStatic
                    fun bar(): Int = 7
                }
            }
            """, "JvmStaticCompanion");

        List<MethodView> bars = findMethods(cl, "bar");
        assertEquals("@JvmStatic companion method must appear exactly once", 1, bars.size());
        assertTrue("The retained bar() should be the real JVM static", bars.get(0).isStatic());
        assertFalse("The retained bar() should not be the companion duplicate",
                bars.get(0).isKotlinCompanionMethod());
    }

    @Test
    public void testInferredReturnCompanionMethodMatchesViaStrippedSignature()
    {
        String src = """
            class KInferred {
                companion object {
                    fun sample(value: Int) = value * value
                }
            }
            """;
        Class<?> cl = compileAndLoad("KInferred.kt", src, "KInferred");
        MethodView sample = findMethod(cl, "sample");

        // The parser cannot infer the return type, so it emits a "void sample(int)"
        // target that differs from the reflected "int sample(int)" — the two only
        // agree once the return type is stripped, which is how the param name attaches.
        ClassInfo info = KotlinInfoParser.parse(new StringReader(src), null);
        String target = info.getCommentsAsList().stream()
                .map(c -> c.target)
                .filter(t -> t.endsWith("sample(int)"))
                .findFirst().orElse(null);
        assertNotNull("parser emits a comment for the companion method", target);
        assertNotEquals("inferred return makes the full signatures differ",
                sample.getSignature(), target);
        assertEquals("but they match once the return type is stripped",
                View.stripReturnType(sample.getSignature()), View.stripReturnType(target));
    }

    @Test
    public void testCompanionPropertyAccessorsSurfaced()
    {
        Class<?> cl = compileAndLoad("CompanionProp.kt", """
            class CompanionProp {
                companion object {
                    var counter: Int = 0
                }
            }
            """, "CompanionProp");

        MethodView getter = findMethod(cl, "getCounter");
        MethodView setter = findMethod(cl, "setCounter");
        assertNotNull("Companion property getter should be surfaced", getter);
        assertNotNull("Companion property setter should be surfaced", setter);
        assertTrue(getter.isKotlinCompanionMethod());
        assertTrue("Companion property accessor should be labelled auto-generated",
                getter.isAutoGenerated());
        assertEquals("Companion", getter.getCompanionReceiverName());
    }

    // --- Test infrastructure ---

    private MethodView findMethod(Class<?> cl, String name)
    {
        List<MethodView> matches = findMethods(cl, name);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private List<MethodView> findMethods(Class<?> cl, String name)
    {
        View view = View.getView(cl, null);
        List<MethodView> matches = new ArrayList<>();
        for (MethodView mv : view.getDeclaredMethods()) {
            if (mv.getName().equals(name)) {
                matches.add(mv);
            }
        }
        return matches;
    }

    private Class<?> compileAndLoad(String fileName, String source, String className)
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

            classLoader = new URLClassLoader(
                    new URL[]{outputDir.toUri().toURL()},
                    getClass().getClassLoader());
            return classLoader.loadClass(className);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to compile and load: " + fileName, e);
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
