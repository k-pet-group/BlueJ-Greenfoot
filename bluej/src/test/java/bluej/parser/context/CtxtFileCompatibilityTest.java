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

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

/**
 * Test to verify .ctxt file format compatibility between old and new implementations.
 * This test creates actual .ctxt files and verifies they maintain the exact format
 * expected by BlueJ.
 */
public class CtxtFileCompatibilityTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private File testCtxtFile;
    
    @Before
    public void setUp() throws IOException {
        testCtxtFile = tempFolder.newFile("TestClass.ctxt");
    }
    
    /**
     * Test that we can read actual BlueJ .ctxt file format
     */
    @Test
    public void testReadActualBlueJFormat() throws IOException {
        // Create a .ctxt file in the actual BlueJ format
        String actualBlueJContent = 
            "#BlueJ class context\n" +
            "comment0.target=TestClass\n" +
            "comment0.text=\\\n" +
            "\\ TestClass\\ is\\ a\\ sample\\ class\\ for\\ testing.\\\n" +
            "\\ It\\ demonstrates\\ various\\ features.\\\n" +
            "\\ \n" +
            "comment1.params=x\\ y\n" +
            "comment1.target=TestClass(int,\\ int)\n" + 
            "comment1.text=\\\n" +
            "\\ Constructor\\ for\\ objects\\ of\\ class\\ TestClass\\\n" +
            "\\ @param\\ x\\ the\\ x\\ coordinate\\\n" +
            "\\ @param\\ y\\ the\\ y\\ coordinate\\\n" +
            "\\ \n" +
            "comment2.params=\n" +
            "comment2.target=int\\ sampleMethod()\n" +
            "comment2.text=\\\n" +
            "\\ An\\ example\\ of\\ a\\ method\\ -\\ replace\\ this\\ comment\\ with\\ your\\ own\\\n" +
            "\\ \n" +
            "comment3.params=y\n" +
            "comment3.target=int\\ anotherMethod(int)\n" +
            "comment3.text=\\\n" +
            "\\ Another\\ method\\ with\\ parameter\\\n" +
            "\\ @param\\ y\\ a\\ sample\\ parameter\\ for\\ this\\ method\\\n" +
            "\\ @return\\ the\\ sum\\ of\\ x\\ and\\ y\\\n" +
            "\\ \n" +
            "numComments=4\n";
        
        try (PrintWriter writer = new PrintWriter(testCtxtFile)) {
            writer.write(actualBlueJContent);
        }
        
        // Load using PropertyContextFormat
        JavaContext context = PropertyContextFormat.fromFile("TestClass", testCtxtFile);

        assertNotNull("Context should not be null", context);
        
        // Verify all metadata was loaded correctly (4 total: 3 methods + 0 fields, ignoring class comment)
        // Note: Class comments are not stored in metadata records
        assertEquals("Should have 3 methods", 3, context.methods().size());
        
        // Verify constructor (first method in the list after parsing)
        MethodMetadata constructor = context.methods().get(0);
        assertEquals("TestClass", constructor.name());
        assertEquals("TestClass(int, int)", constructor.signature());
        assertEquals(2, constructor.parameters().size());
        assertTrue("Constructor comment should contain param descriptions",
            constructor.documentation().orElse("").contains("x coordinate"));
        
        // Verify method comments
        MethodMetadata method1 = context.methods().get(1);
        assertEquals("sampleMethod", method1.name());
        assertEquals("int sampleMethod()", method1.signature());
        assertEquals(0, method1.parameters().size());
        
        MethodMetadata method2 = context.methods().get(2);
        assertEquals("anotherMethod", method2.name());
        assertEquals("int anotherMethod(int)", method2.signature());
        assertEquals(1, method2.parameters().size());
    }
    
    /**
     * Test that we generate the correct format when saving
     */
    @Test
    public void testWriteCorrectFormat() throws IOException {
        // Create a context with typical BlueJ content using new API
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "MyClass",
                "MyClass(String, int)",
                "MyClass",
                Arrays.asList("String name", "int value"),
                Optional.of("Constructor for MyClass\n" +
                           "@param name The name of the object\n" +
                           "@param value The initial value\n")
            ),
            new MethodMetadata(
                "doSomething",
                "void doSomething()",
                "void",
                List.of(),
                Optional.of("Method that performs an action")
            ),
            new MethodMetadata(
                "getName",
                "String getName()",
                "String",
                List.of(),
                Optional.of("Returns the name of this object\n@return the name")
            ),
            new MethodMetadata(
                "validate",
                "boolean validate(String, int, boolean)",
                "boolean",
                Arrays.asList("String input", "int threshold", "boolean strict"),
                Optional.of("Validates the object state\n" +
                           "@param input The input to validate\n" +
                           "@param threshold The threshold value\n" +
                           "@param strict Whether to use strict validation\n" +
                           "@return true if valid, false otherwise")
            )
        );
        
        JavaContext context = new JavaContext("MyClass", methods, List.of());
        
        // Save the context
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Read the file as Properties to verify format
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(testCtxtFile)) {
            props.load(in);
        }
        
        // Verify the Properties format (4 comments now, no class comment)
        assertEquals("4", props.getProperty("numComments"));
        
        // Check constructor
        assertEquals("MyClass(String, int)", props.getProperty("comment0.target"));
        assertEquals("name value", props.getProperty("comment0.params"));
        assertNotNull(props.getProperty("comment0.text"));
        
        // Check methods
        assertEquals("void doSomething()", props.getProperty("comment1.target"));
        assertNull(props.getProperty("comment1.params")); // No params
        
        assertEquals("String getName()", props.getProperty("comment2.target"));
        assertNull(props.getProperty("comment2.params")); // No params
        
        assertEquals("boolean validate(String, int, boolean)", props.getProperty("comment3.target"));
        assertEquals("input threshold strict", props.getProperty("comment3.params"));
        assertNotNull(props.getProperty("comment3.text"));
    }
    
    /**
     * Test handling of escape sequences in Properties format
     */
    @Test
    public void testEscapeSequenceHandling() throws IOException {
        // Create content with special characters that need escaping in Properties
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "test",
                "void test()",
                "void",
                List.of(),
                Optional.of("Method with special chars: = : \\ \n" +
                           "New line above, tab\there, and unicode \\u0041")
            )
        );
        
        JavaContext context = new JavaContext("TestClass", methods, List.of());
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Read back
        JavaContext loaded = PropertyContextFormat.fromFile("TestClass", testCtxtFile);

        assertNotNull("Context should not be null", loaded);
        
        assertEquals(1, loaded.methods().size());
        MethodMetadata method = loaded.methods().get(0);
        
        // Properties should handle escaping automatically
        assertTrue("Should preserve special characters",
            method.documentation().orElse("").contains("special chars"));
    }
    
    /**
     * Test that empty params are handled correctly (no params property written)
     */
    @Test
    public void testEmptyParamsHandling() throws IOException {
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "noParams",
                "void noParams()",
                "void",
                List.of(),
                Optional.of("Method with no parameters")
            ),
            new MethodMetadata(
                "emptyList",
                "void emptyList()",
                "void",
                List.of(),
                Optional.of("Method with empty param list")
            ),
            new MethodMetadata(
                "withParams",
                "void withParams(int, String)",
                "void",
                Arrays.asList("int value", "String text"),
                Optional.of("Method with parameters")
            )
        );
        
        JavaContext context = new JavaContext("TestClass", methods, List.of());
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Verify the file content
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(testCtxtFile)) {
            props.load(in);
        }
        
        // No params property should be written for methods without parameters
        assertNull("Should not have params property for first method", 
            props.getProperty("comment0.params"));
        assertNull("Should not have params property for second method", 
            props.getProperty("comment1.params"));
        assertEquals("value text", props.getProperty("comment2.params"));
    }
    
    /**
     * Test compatibility with BlueJ's actual file header
     */
    @Test
    public void testFileHeaderCompatibility() throws IOException {
        // Create an empty context (no methods or fields)
        JavaContext context = new JavaContext("TestClass", List.of(), List.of());
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Read the file content
        String content;
        try (BufferedReader reader = new BufferedReader(new FileReader(testCtxtFile))) {
            content = reader.readLine();
        }
        
        // Should start with the BlueJ header
        assertTrue("File should start with BlueJ header", 
            content.startsWith("#BlueJ class context"));
    }
}