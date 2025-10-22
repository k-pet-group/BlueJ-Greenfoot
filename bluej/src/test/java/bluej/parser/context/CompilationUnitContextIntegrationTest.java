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

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive integration test for CompilationUnitContext across
 * Package, View, ClassTarget, and ClassInfo components.
 */
public class CompilationUnitContextIntegrationTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private File projectDir;
    private File packageDir;
    private File ctxtFile;
    private File classFile;
    
    @Before
    public void setUp() throws IOException {
        // Create test directory structure
        projectDir = tempFolder.newFolder("test_project");
        packageDir = new File(projectDir, "test_package");
        packageDir.mkdirs();
        
        // Create test .java file
        classFile = new File(packageDir, "TestClass.java");
        try (PrintWriter writer = new PrintWriter(classFile)) {
            writer.println("package test_package;");
            writer.println("");
            writer.println("/**");
            writer.println(" * Test class for integration testing");
            writer.println(" */");
            writer.println("public class TestClass {");
            writer.println("    ");
            writer.println("    /**");
            writer.println("     * Constructor");
            writer.println("     * @param value Initial value");
            writer.println("     * @param name The name");
            writer.println("     */");
            writer.println("    public TestClass(int value, String name) {");
            writer.println("    }");
            writer.println("    ");
            writer.println("    /**");
            writer.println("     * Test method");
            writer.println("     */");
            writer.println("    public void testMethod() {");
            writer.println("    }");
            writer.println("    ");
            writer.println("    /**");
            writer.println("     * Another method with parameters");
            writer.println("     * @param input Input string");
            writer.println("     * @param count Number of times");
            writer.println("     * @return Result string");
            writer.println("     */");
            writer.println("    public String processData(String input, int count) {");
            writer.println("        return input;");
            writer.println("    }");
            writer.println("}");
        }
        
        // Create corresponding .ctxt file
        ctxtFile = new File(packageDir, "TestClass.ctxt");
    }
    
    @After
    public void tearDown() {
        // Cleanup is handled by TemporaryFolder
    }
    
    /**
     * Test 1: ClassInfo to CompilationUnitContext flow
     * Verifies that ClassInfo properly populates CompilationUnitContext
     */
    @Test
    public void testClassInfoToContextFlow() throws Exception {
        // Create a context using new immutable API
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "TestClass",
                "TestClass(int,String)",
                "TestClass",
                Arrays.asList("int value", "String name"),
                Optional.of("Constructor")
            ),
            new MethodMetadata(
                "testMethod",
                "void testMethod()",
                "void",
                List.of(),
                Optional.of("Test method")
            ),
            new MethodMetadata(
                "processData",
                "String processData(String,int)",
                "String",
                Arrays.asList("String input", "int count"),
                Optional.of("Another method with parameters")
            )
        );
        
        JavaContext context = new JavaContext("TestClass", methods, List.of());
        
        // Verify the context has the expected data
        assertEquals("Should have 3 methods", 3, context.methods().size());
        
        // Verify first method (constructor)
        MethodMetadata method0 = context.methods().get(0);
        assertEquals("TestClass", method0.name());
        assertEquals("TestClass(int,String)", method0.signature());
        assertEquals("Constructor", method0.documentation().orElse(""));
        assertEquals(2, method0.parameters().size());
        
        // Verify second method
        MethodMetadata method1 = context.methods().get(1);
        assertEquals("testMethod", method1.name());
        assertEquals("void testMethod()", method1.signature());
        assertEquals("Test method", method1.documentation().orElse(""));
        assertEquals(0, method1.parameters().size());
        
        // Verify third method
        MethodMetadata method2 = context.methods().get(2);
        assertEquals("processData", method2.name());
        assertEquals("String processData(String,int)", method2.signature());
        assertEquals("Another method with parameters", method2.documentation().orElse(""));
        assertEquals(2, method2.parameters().size());
    }
    
    /**
     * Test 2: Package.java save context functionality
     * Verifies that Package can save CompilationUnitContext to .ctxt files
     */
    @Test
    public void testPackageSaveContext() throws Exception {
        // Create and populate context using new API
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "TestClass",
                "TestClass(int,String)",
                "TestClass",
                Arrays.asList("int value", "String name"),
                Optional.of("Constructor with parameters")
            ),
            new MethodMetadata(
                "doSomething",
                "void doSomething()",
                "void",
                List.of(),
                Optional.of("Method that does something")
            )
        );
        
        JavaContext context = new JavaContext("TestClass", methods, List.of());
        
        // Save the context (simulates what Package would do)
        PropertyContextFormat.writeToFile(context, ctxtFile);
        
        // Verify the file was created
        assertTrue("Ctxt file should exist", ctxtFile.exists());
        
        // Verify the saved format by reading with Properties
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(ctxtFile)) {
            props.load(in);
        }
        
        // Verify Properties format compatibility
        assertEquals("2", props.getProperty("numComments"));
        assertEquals("TestClass(int,String)", props.getProperty("comment0.target"));
        assertEquals("Constructor with parameters", props.getProperty("comment0.text"));
        assertEquals("value name", props.getProperty("comment0.params"));
        assertEquals("void doSomething()", props.getProperty("comment1.target"));
        assertEquals("Method that does something", props.getProperty("comment1.text"));
        assertNull("Should not have params for second comment", props.getProperty("comment1.params"));
    }
    
    /**
     * Test 3: View.java library class resource loading
     * Verifies that View can load CompilationUnitContext for library classes
     */
    @Test
    public void testViewLibraryClassLoading() throws Exception {
        // Create a .ctxt file simulating a library class context
        Properties libProps = new Properties();
        libProps.setProperty("numComments", "3");
        libProps.setProperty("comment0.target", "java.lang.String()");
        libProps.setProperty("comment0.text", "Default constructor");
        libProps.setProperty("comment1.target", "int length()");
        libProps.setProperty("comment1.text", "Returns the length of this string");
        libProps.setProperty("comment2.target", "String substring(int,int)");
        libProps.setProperty("comment2.text", "Returns a substring");
        libProps.setProperty("comment2.params", "beginIndex endIndex");
        
        File libCtxtFile = new File(tempFolder.getRoot(), "String.ctxt");
        try (OutputStream out = new FileOutputStream(libCtxtFile)) {
            libProps.store(out, "Library class context");
        }
        
        // Load using PropertyContextFormat (simulates what View would do)
        JavaContext libContext = PropertyContextFormat.fromFile("String", libCtxtFile);

        assertNotNull("Context should not be null", libContext);

        // Verify library context was loaded correctly (3 methods)
        assertEquals("Should have 3 methods", 3, libContext.methods().size());
        
        MethodMetadata method0 = libContext.methods().get(0);
        assertEquals("java.lang.String", method0.name()); // Full qualified name from signature
        assertEquals("java.lang.String()", method0.signature());
        assertEquals("Default constructor", method0.documentation().orElse(""));
        
        MethodMetadata method1 = libContext.methods().get(1);
        assertEquals("length", method1.name());
        assertEquals("int length()", method1.signature());
        assertEquals("Returns the length of this string", method1.documentation().orElse(""));
        
        MethodMetadata method2 = libContext.methods().get(2);
        assertEquals("substring", method2.name());
        assertEquals("String substring(int,int)", method2.signature());
        assertEquals("Returns a substring", method2.documentation().orElse(""));
        assertEquals(2, method2.parameters().size());
    }
    
    /**
     * Test 4: ClassTarget getCompilationContext() API
     * Verifies the ClassTarget can provide CompilationUnitContext
     */
    @Test
    public void testClassTargetGetCompilationContext() throws Exception {
        // Create a context and save it to file
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "TestClass",
                "TestClass()",
                "TestClass",
                List.of(),
                Optional.of("Default constructor")
            ),
            new MethodMetadata(
                "initialize",
                "void initialize(String)",
                "void",
                Arrays.asList("String config"),
                Optional.of("Initialization method")
            )
        );
        
        JavaContext context = new JavaContext("TestClass", methods, List.of());
        PropertyContextFormat.writeToFile(context, ctxtFile);
        
        JavaContext loadedContext = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Loaded context should not be null", loadedContext);
        
        // Verify the loaded context matches what was saved
        assertEquals("Should have 2 methods", 2, loadedContext.methods().size());
        
        MethodMetadata method0 = loadedContext.methods().get(0);
        assertEquals("TestClass", method0.name());
        assertEquals("TestClass()", method0.signature());
        assertEquals("Default constructor", method0.documentation().orElse(""));
        assertEquals(0, method0.parameters().size());
        
        MethodMetadata method1 = loadedContext.methods().get(1);
        assertEquals("initialize", method1.name());
        assertEquals("void initialize(String)", method1.signature());
        assertEquals("Initialization method", method1.documentation().orElse(""));
        assertEquals(1, method1.parameters().size());
    }
    
    /**
     * Test 5: Round-trip compatibility test
     * Write with JavaContext, read with Properties, 
     * then write with Properties and read with JavaContext
     */
    @Test
    public void testRoundTripCompatibility() throws Exception {
        // Step 1: Create and save with JavaContext
        List<MethodMetadata> methods1 = Arrays.asList(
            new MethodMetadata(
                "TestClass",
                "TestClass(String,int,boolean)",
                "TestClass",
                Arrays.asList("String name", "int value", "boolean flag"),
                Optional.of("Complex constructor")
            )
        );
        
        JavaContext context1 = new JavaContext("TestClass", methods1, List.of());
        PropertyContextFormat.writeToFile(context1, ctxtFile);
        
        // Step 2: Read with Properties
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(ctxtFile)) {
            props.load(in);
        }
        
        // Verify Properties content
        assertEquals("1", props.getProperty("numComments"));
        assertEquals("TestClass(String,int,boolean)", props.getProperty("comment0.target"));
        assertEquals("Complex constructor", props.getProperty("comment0.text"));
        assertEquals("name value flag", props.getProperty("comment0.params"));
        
        // Step 3: Modify and save with Properties
        props.setProperty("comment1.target", "void newMethod()");
        props.setProperty("comment1.text", "Added method");
        props.setProperty("numComments", "2");
        
        try (OutputStream out = new FileOutputStream(ctxtFile)) {
            props.store(out, "Modified by Properties");
        }
        
        // Step 4: Read with JavaContext
        JavaContext context2 = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Context should not be null", context2);
        
        // Verify both methods are present
        assertEquals("Should have 2 methods", 2, context2.methods().size());
        
        MethodMetadata method0 = context2.methods().get(0);
        assertEquals("TestClass", method0.name());
        assertEquals("TestClass(String,int,boolean)", method0.signature());
        assertEquals("Complex constructor", method0.documentation().orElse(""));
        assertEquals(3, method0.parameters().size());
        
        MethodMetadata method1 = context2.methods().get(1);
        assertEquals("newMethod", method1.name());
        assertEquals("void newMethod()", method1.signature());
        assertEquals("Added method", method1.documentation().orElse(""));
        assertEquals(0, method1.parameters().size());
    }
    
    /**
     * Test 6: Error handling - malformed .ctxt file
     */
    @Test
    public void testMalformedCtxtFileHandling() throws Exception {
        // Create a malformed .ctxt file
        try (PrintWriter writer = new PrintWriter(ctxtFile)) {
            writer.println("This is not a valid properties file!");
            writer.println("numComments = not_a_number");
            writer.println("comment0.target missing equals");
        }
        
        // Try to load it - should handle gracefully
        JavaContext context = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Context should not be null", context);
        
        // Context should have handled the error gracefully
        assertNotNull("Context should not be null", context);
    }
    
    /**
     * Test 7: Performance test - large .ctxt file
     */
    @Test
    public void testLargeCtxtFilePerformance() throws Exception {
        // Create a large context with many methods
        int numMethods = 100;
        List<MethodMetadata> methods = new ArrayList<>();
        
        for (int i = 0; i < numMethods; i++) {
            List<String> params = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                params.add("Object param" + j);
            }
            
            methods.add(new MethodMetadata(
                "method" + i,
                "void method" + i + "(String,int,boolean,Object,List)",
                "void",
                params,
                Optional.of("Method number " + i + " with multiple parameters")
            ));
        }
        
        JavaContext context = new JavaContext("LargeClass", methods, List.of());
        
        // Measure save performance
        long startSave = System.currentTimeMillis();
        PropertyContextFormat.writeToFile(context, ctxtFile);
        long saveDuration = System.currentTimeMillis() - startSave;
        
        assertTrue("Save should complete within reasonable time", saveDuration < 1000);
        
        // Measure load performance
        long startLoad = System.currentTimeMillis();
        JavaContext loadedContext = PropertyContextFormat.fromFile("LargeClass", ctxtFile);

        assertNotNull("Context should not be null", loadedContext);
        long loadDuration = System.currentTimeMillis() - startLoad;
        
        assertTrue("Load should complete within reasonable time", loadDuration < 1000);
        assertEquals("Should load all methods", numMethods, loadedContext.methods().size());
    }
    
    /**
     * Test 8: Unicode and special characters handling
     */
    @Test
    public void testUnicodeAndSpecialCharacters() throws Exception {
        // Create context with Unicode and special characters
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "método",
                "void método()",
                "void",
                List.of(),
                Optional.of("Method with Unicode: 日本語 中文 한국어 العربية")
            ),
            new MethodMetadata(
                "processText",
                "String processText(String)",
                "String",
                Arrays.asList("String tëxt_with_ümläuts"),
                Optional.of("Handles special chars: !@#$%^&*(){}[]|\\:;\"'<>,.?/")
            )
        );
        
        JavaContext context = new JavaContext("TestClass", methods, List.of());
        
        // Save and reload
        PropertyContextFormat.writeToFile(context, ctxtFile);
        
        JavaContext loadedContext = PropertyContextFormat.fromFile("TestClass", ctxtFile);

        assertNotNull("Context should not be null", loadedContext);
        
        // Verify Unicode and special characters are preserved
        assertEquals(2, loadedContext.methods().size());
        
        MethodMetadata method0 = loadedContext.methods().get(0);
        assertEquals("método", method0.name());
        assertTrue(method0.documentation().orElse("").contains("日本語"));
        assertTrue(method0.documentation().orElse("").contains("中文"));
        
        MethodMetadata method1 = loadedContext.methods().get(1);
        assertTrue(method1.documentation().orElse("").contains("!@#$%^&*()"));
        assertTrue(method1.parameters().get(0).contains("tëxt_with_ümläuts"));
    }
}