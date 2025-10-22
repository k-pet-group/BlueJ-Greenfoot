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
import org.junit.After;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

/**
 * Test class for CompilationUnitContext to verify backwards compatibility
 * with existing .ctxt file format.
 */
public class CompilationUnitContextTest {
    
    private File testCtxtFile;
    
    @Before
    public void setUp() throws IOException {
        testCtxtFile = File.createTempFile("test", ".ctxt");
        testCtxtFile.deleteOnExit();
    }
    
    @After
    public void tearDown() {
        if (testCtxtFile != null && testCtxtFile.exists()) {
            testCtxtFile.delete();
        }
    }
    
    /**
     * Test backwards compatibility: reading old format .ctxt files
     * created with Properties.store()
     */
    @Test
    public void testBackwardsCompatibilityRead() throws IOException {
        // Create a .ctxt file in the old format using Properties
        Properties oldProps = new Properties();
        oldProps.setProperty("numComments", "2");
        oldProps.setProperty("comment0.target", "void test()");
        oldProps.setProperty("comment0.text", "Test method");
        oldProps.setProperty("comment1.target", "TestClass(int,String)");
        oldProps.setProperty("comment1.text", "Constructor");
        oldProps.setProperty("comment1.params", "value name");
        
        try (OutputStream out = new FileOutputStream(testCtxtFile)) {
            oldProps.store(out, "BlueJ class context");
        }
        
        // Read it using PropertyContextFormat
        JavaContext context = PropertyContextFormat.fromFile("TestClass", testCtxtFile);

        assertNotNull("Context should not be null", context);

        // Verify the data was loaded correctly (2 methods)
        assertEquals("Should have 2 methods", 2, context.methods().size());
        
        MethodMetadata method0 = context.methods().get(0);
        assertEquals("test", method0.name());
        assertEquals("void test()", method0.signature());
        assertEquals("Test method", method0.documentation().orElse(""));
        assertEquals(0, method0.parameters().size());
        
        MethodMetadata method1 = context.methods().get(1);
        assertEquals("TestClass", method1.name());
        assertEquals("TestClass(int,String)", method1.signature());
        assertEquals("Constructor", method1.documentation().orElse(""));
        assertEquals(2, method1.parameters().size());
        // Parameter format is "type name"
        assertTrue("First param should contain 'value'", method1.parameters().get(0).contains("value"));
        assertTrue("Second param should contain 'name'", method1.parameters().get(1).contains("name"));
    }
    
    /**
     * Test backwards compatibility: writing .ctxt files 
     * that can be read by old Properties.load()
     */
    @Test
    public void testBackwardsCompatibilityWrite() throws IOException {
        // Create context with data using new API
        List<MethodMetadata> methods = Arrays.asList(
            new MethodMetadata(
                "test",
                "void test()",
                "void",
                List.of(),
                Optional.of("Test method")
            ),
            new MethodMetadata(
                "TestClass",
                "TestClass(int,String)",
                "TestClass",
                Arrays.asList("int value", "String name"),
                Optional.of("Constructor")
            )
        );
        
        JavaContext context = new JavaContext("TestClass", methods, List.of());
        
        // Save using PropertyContextFormat
        PropertyContextFormat.writeToFile(context, testCtxtFile);
        
        // Read it back using old Properties method
        Properties readProps = new Properties();
        try (InputStream in = new FileInputStream(testCtxtFile)) {
            readProps.load(in);
        }
        
        // Verify the format is compatible
        assertEquals("2", readProps.getProperty("numComments"));
        assertEquals("void test()", readProps.getProperty("comment0.target"));
        assertEquals("Test method", readProps.getProperty("comment0.text"));
        assertEquals("TestClass(int,String)", readProps.getProperty("comment1.target"));
        assertEquals("Constructor", readProps.getProperty("comment1.text"));
        assertEquals("value name", readProps.getProperty("comment1.params"));
    }
    
    /**
     * Test that empty/missing files are handled gracefully
     */
    @Test
    public void testEmptyFile() throws IOException {
        File emptyFile = File.createTempFile("empty", ".ctxt");
        emptyFile.deleteOnExit();
        
        JavaContext context = PropertyContextFormat.fromFile("TestClass", emptyFile);

        assertNotNull("Context should not be null", context);
        assertEquals("Should have no methods", 0, context.methods().size());
        assertEquals("Should have no fields", 0, context.fields().size());
    }
    
    /**
     * Test that non-existent files are handled gracefully
     */
    @Test
    public void testNonExistentFile() throws IOException {
        File nonExistent = new File("non_existent_file.ctxt");
        
        JavaContext context = PropertyContextFormat.fromFile("TestClass", nonExistent);

        assertNull("Context should be null for non-existent file", context);
    }
}