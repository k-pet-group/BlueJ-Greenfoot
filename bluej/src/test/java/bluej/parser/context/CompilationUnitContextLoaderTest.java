/*
 This file is part of the BlueJ program.
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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
package bluej.parser.context;

import bluej.classmgr.BPClassLoader;
import bluej.pkgmgr.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CompilationUnitContextLoader} focusing on file versus
 * classpath resolution behaviour.
 */
public class CompilationUnitContextLoaderTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void contextForClassLoadsFromPackageRoot() throws Exception {
        File root = temp.newFolder("projectRoot");
        File packageDir = new File(root, "example/app");
        assertTrue(packageDir.mkdirs());

        File ctxtFile = new File(packageDir, "Sample.ctxt");
        Properties props = new Properties();
        props.setProperty("numComments", "1");
        props.setProperty("comment0.target", "void greet()");
        props.setProperty("comment0.text", "Greets the user");
        try (OutputStream out = new FileOutputStream(ctxtFile)) {
            props.store(out, "test");
        }

        // Use TestClassLoaderProvider instead of trying to create Project instance
        TestClassLoaderProvider provider = new TestClassLoaderProvider.Builder()
                .projectDir(root)
                .build();

        CompilationUnitContextLoader loader = new CompilationUnitContextLoader(provider);

        CompilationUnitContext context = loader.contextForClass("example.app.Sample");

        assertNotNull("Context should not be null", context);
        
        // Verify it's a JavaContext with correct data
        assertTrue("Should be JavaContext", context instanceof JavaContext);
        JavaContext javaContext = (JavaContext) context;
        assertEquals("Expected one method", 1, javaContext.methods().size());
        assertEquals("greet", javaContext.methods().get(0).name());
        assertEquals("void greet()", javaContext.methods().get(0).signature());
        assertEquals("Greets the user", javaContext.methods().get(0).documentation().orElse(""));

        // Cached access should return the same instance when caching is enabled
        assertSame("Context should be cached", context, loader.contextForClass("example.app.Sample"));
    }

    @Test
    public void contextForClassFallsBackToResource() throws Exception {
        final File resourceFile;
        final File root = temp.newFolder("resourceRoot");

        try {
            Properties props = new Properties();
            props.setProperty("numComments", "1");
            props.setProperty("comment0.target", "int size()");
            props.setProperty("comment0.text", "Returns size");

            File packageDir = new File(root, "cache/demo");
            assertTrue(packageDir.mkdirs());

            resourceFile = new File(packageDir, "List.ctxt");

            props.store(new FileOutputStream(resourceFile), "test");
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        // Use TestClassLoaderProvider with resource path
        TestClassLoaderProvider provider = new TestClassLoaderProvider.Builder()
                .projectDir(root)
                .withResource(root)
                .build();

        CompilationUnitContextLoader loader = new CompilationUnitContextLoader(provider);

        CompilationUnitContext context = loader.contextForClass("cache.demo.List");
        assertNotNull("Context should be loaded from resource", context);
        
        assertTrue("Should be JavaContext", context instanceof JavaContext);
        JavaContext javaContext = (JavaContext) context;
        assertEquals(1, javaContext.methods().size());
        assertEquals("size", javaContext.methods().get(0).name());
        assertEquals("int size()", javaContext.methods().get(0).signature());
        assertEquals("Returns size", javaContext.methods().get(0).documentation().orElse(""));

        // Ensure caching uses resource key (second call should reuse instance)
        assertSame(context, loader.contextForClass("cache.demo.List"));
    }

    @Test
    public void contextForClassWithClassObjectUsesPackageRoot() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            // Skip test when compiler is not available (e.g. running on JRE)
            return;
        }

        File root = temp.newFolder("compiledProject");
        File sourceDir = new File(root, "sample");
        assertTrue(sourceDir.mkdirs());

        File sourceFile = new File(sourceDir, "LoaderSubject.java");
        String source = "package sample; public class LoaderSubject { public void ping() {} }";
        try (OutputStream out = new FileOutputStream(sourceFile)) {
            out.write(source.getBytes(StandardCharsets.UTF_8));
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends javax.tools.JavaFileObject> units =
                fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
            boolean success = compiler.getTask(null, fileManager, null,
                Arrays.asList("-d", root.getAbsolutePath()), null, units).call();
            assertTrue("Compilation should succeed", success);
        }

        // Create corresponding ctxt file next to compiled class
        File ctxtFile = new File(sourceDir, "LoaderSubject.ctxt");
        Properties props = new Properties();
        props.setProperty("numComments", "1");
        props.setProperty("comment0.target", "void ping()");
        props.setProperty("comment0.text", "Ping method");
        try (OutputStream out = new FileOutputStream(ctxtFile)) {
            props.store(out, "compiled");
        }

        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { root.toURI().toURL() }, null)) {
            Class<?> clazz = Class.forName("sample.LoaderSubject", true, urlClassLoader);

            // Use TestClassLoaderProvider with resource path
            TestClassLoaderProvider provider = new TestClassLoaderProvider.Builder()
                    .projectDir(root)
                    .withResource(root)
                    .build();

            CompilationUnitContextLoader loader = new CompilationUnitContextLoader(provider);

            CompilationUnitContext context = loader.contextForClass(clazz);
            assertNotNull(context);
            
            assertTrue("Should be JavaContext", context instanceof JavaContext);
            JavaContext javaContext = (JavaContext) context;
            assertEquals(1, javaContext.methods().size());
            assertEquals("ping", javaContext.methods().get(0).name());
            assertEquals("void ping()", javaContext.methods().get(0).signature());
            assertEquals("Ping method", javaContext.methods().get(0).documentation().orElse(""));
        }
    }
}


