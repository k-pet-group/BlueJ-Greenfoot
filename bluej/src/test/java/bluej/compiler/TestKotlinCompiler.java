/*
 This file is part of the BlueJ program. 
 Copyright (C) 2023  Michael Kolling and John Rosenberg

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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for the KotlinCompiler class.
 */
public class TestKotlinCompiler
{
    private KotlinCompiler compiler;
    private Path tempDir;
    private File kotlinFile;

    @Before
    public void setUp() throws IOException
    {
        // Create a temporary directory for compilation output
        tempDir = Files.createTempDirectory("kotlin-compiler-test");

        // Create a simple Kotlin class file
        String simpleKotlinClass = 
            "class SimpleKotlinClass {\n" +
            "    val name: String = \"Test\"\n" +
            "    val value: Int = 42\n" +
            "}";

        kotlinFile = tempDir.resolve("SimpleKotlinClass.kt").toFile();
        Files.write(kotlinFile.toPath(), simpleKotlinClass.getBytes(StandardCharsets.UTF_8));

        // Initialize the compiler
        compiler = new KotlinCompiler();
        compiler.setDestDir(tempDir.toFile());

        // Set up classpath
        List<File> classPath = new ArrayList<>();
        classPath.add(tempDir.toFile());
        classPath.add(new File(getKotlinStdLibJarPath()));
        compiler.setClasspath(classPath);
    }

    @After
    public void tearDown() throws IOException
    {
        // Clean up temporary files
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    System.err.println("Failed to delete: " + path);
                }
            });
    }

    @Test
    public void testCompileSimpleKotlinClass()
    {
        // Create a simple CompileObserver to track compilation progress
        TestCompileObserver observer = new TestCompileObserver();

        // Compile the Kotlin file
        boolean success = compiler.compile(
            new File[] { kotlinFile }, 
            observer, 
            false, // not internal
            null,  // no user options
            StandardCharsets.UTF_8, CompileType.EXPLICIT_USER_COMPILE, null
        );

        // Verify compilation was successful
        assertTrue("Compilation should succeed", success);
        assertFalse("No errors should be reported", observer.hasErrors());

        // Verify the class file was generated
        File classFile = tempDir.resolve("SimpleKotlinClass.class").toFile();
        assertTrue("Class file should be generated", classFile.exists());
    }

    /**
     * A simple CompileObserver implementation for testing.
     */
    private static class TestCompileObserver implements CompileObserver
    {
        private boolean hasErrors = false;

        @Override
        public void compilerMessage(Diagnostic diagnostic, CompileType type)
        {
            if (diagnostic.getType() == Diagnostic.ERROR) {
                hasErrors = true;
                System.out.println("Compilation error: " + diagnostic.getMessage());
            }
        }

        @Override
        public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
        {
            System.out.println("Starting compilation of " + sources.length + " files");
        }

        @Override
        public void endCompile(CompileInputFile[] sources, boolean successful, CompileType type, int compilationSequence)
        {
            System.out.println("Compilation " + (successful ? "succeeded" : "failed"));
        }

        public boolean hasErrors()
        {
            return hasErrors;
        }
    }

    private static String getKotlinStdLibJarPath() {
        Class<?> stdlibClass = kotlin.collections.CollectionsKt.class;
        String resourcePath = stdlibClass.getName().replace('.', '/') + ".class";
        var url = stdlibClass.getClassLoader().getResource(resourcePath);
        if (url != null && "jar".equals(url.getProtocol())) {
            String path = url.getPath();
            return path.substring("file:".length(), path.indexOf("!"));
        }
        return null;
    }
}
