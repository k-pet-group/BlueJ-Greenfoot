/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009,2010,2011,2012,2014,2016,2022,2024  Michael Kolling and John Rosenberg

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
package bluej.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import bluej.JavaFXThreadingRule;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.symtab.ClassInfo;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static bluej.utility.ResourceFileReader.getResourceFile;

/**
 * Test the Kotlin parser functionality by parsing various Kotlin source files and strings.
 * <p>
 * This class contains tests for:
 * <ul>
 * <li>Verifying that the KotlinInfoParser class exists and can be instantiated</li>
 * <li>Parsing a simple Kotlin string</li>
 * <li>Parsing a simple Kotlin file (kotlin_simple.dat)</li>
 * <li>Parsing a more complex Kotlin file with various language constructs (kotlin_basic.dat)</li>
 * </ul>
 */
public class KotlinBasicParseTest
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();


    /**
     * Test that the KotlinInfoParser class exists and can be instantiated.
     * This is a basic test to verify that the Kotlin parser functionality is available.
     */
    @Test
    public void testKotlinParserExists()
    {
        // Create a StringReader with a simple Kotlin class
        StringReader sr = new StringReader(
                "class SimpleKotlin {\n" +
                "  val name: String = \"Test\"\n" +
                "}\n"
        );

        // Verify that KotlinInfoParser can be instantiated
        KotlinInfoParser parser = new KotlinInfoParser(sr, new ClassLoaderResolver(this.getClass().getClassLoader()));
        assertNotNull(parser);
    }

    /**
     * Test parsing a Kotlin file with basic language constructs.
     * This test verifies that the KotlinInfoParser can parse a Kotlin file with various language constructs.
     * @throws Exception if there is an error reading the file
     */
    @Test
    public void testParseKotlinBasicFile() throws Exception
    {
        // Get the kotlin_basic.dat file
        File file = getResourceFile(getClass(), "/bluej/parser/kotlin/kotlin_basic.dat");
        assertNotNull("kotlin_basic.dat file should exist", file);

        // Create a reader for the file
        FileInputStream fis = new FileInputStream(file);

        try (fis) {
            InputStreamReader reader = new InputStreamReader(fis);
            // Parse the Kotlin file
            ClassInfo info = KotlinInfoParser.parse(reader, new ClassLoaderResolver(this.getClass().getClassLoader()), "bluej.parser.kotlin.data");
            assertNotNull("Parsed ClassInfo should not be null", info);

            // Assert that the class name is correct
            assertEquals("KotlinBasicClass", info.getName());

            assertFalse("Class should not be an interface", info.isInterface());
            assertFalse("Class should not be abstract", info.isAbstract());
        }
        // Close the file input stream
    }

    /**
     * Test parsing a simple Kotlin string.
     * This test verifies that the KotlinInfoParser can parse a simple Kotlin class from a string.
     */
    @Test
    public void testParseSimpleKotlinString()
    {
        // Create a StringReader with a simple Kotlin class
        StringReader sr = new StringReader(
                "class SimpleKotlin {\n" +
                "  val name: String = \"Test\"\n" +
                "}\n"
        );

        // Parse the Kotlin string
        ClassInfo info = KotlinInfoParser.parse(sr, new ClassLoaderResolver(this.getClass().getClassLoader()), "testpkg");

        // Assert that the parsed info is not null
        assertNotNull("Parsed ClassInfo should not be null", info);

        // Assert that the class name is correct
        assertEquals("SimpleKotlin", info.getName());

        // Assert that the class is not an interface
        assertFalse("Class should not be an interface", info.isInterface());

        // Assert that the class is not abstract
        assertFalse("Class should not be abstract", info.isAbstract());
    }

    /**
     * Test parsing the kotlin_simple.dat file.
     * This test verifies that the KotlinInfoParser can parse a simple Kotlin file.
     * @throws Exception if there is an error reading the file
     */
    @Test
    public void testParseKotlinSimpleFile() throws Exception
    {
        // Get the kotlin_simple.dat file
        File file = getResourceFile(getClass(), "/bluej/parser/kotlin/kotlin_simple.dat");
        assertNotNull("kotlin_simple.dat file should exist", file);

        // Create a reader for the file
        FileInputStream fis = new FileInputStream(file);

        try (fis) {
            InputStreamReader reader = new InputStreamReader(fis);
            ClassInfo info = KotlinInfoParser.parse(reader, new ClassLoaderResolver(this.getClass().getClassLoader()), "bluej.parser.kotlin.data");

            assertNotNull("Parsed ClassInfo should not be null", info);
            assertEquals("SimpleKotlinClass", info.getName());

            assertFalse("Class should not be an interface", info.isInterface());
            assertFalse("Class should not be abstract", info.isAbstract());

            List<String> usedClasses = info.getUsed();
            assertEquals("Used classes size should be 0", 0, usedClasses.size());
        }
    }

    /**
     * Test that when a class uses a field from another class, the other class is marked as "used".
     * This test verifies that the KotlinInfoParser correctly identifies dependencies between classes
     * when one class uses a field from another class.
     */
    @Test
    public void testClassFieldUsage()
    {
        // Create a StringReader with two Kotlin classes where one uses a field of type from the other class
        StringReader sr = new StringReader(
                "class ClassUsingField {\n" +
                "  // Explicitly use ClassWithField as a type for a field\n" +
                "  private val classWithField: ClassWithField = ClassWithField()\n" +
                "  var x: Int = 0\n" +
                "  \n" +
                "  fun useField() {\n" +
                "    // Access the field from the other class\n" +
                "    val value = classWithField.field\n" +
                "    println(value)\n" +
                "  }\n" +
                "}\n"
        );

        ClassInfo info = KotlinInfoParser.parse(sr, new ClassLoaderResolver(this.getClass().getClassLoader()), "testpkg");

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("ClassUsingField", info.getName());

        List<String> usedClasses = info.getUsed();
        assertEquals("Used classes size should be 1", 1, usedClasses.size());
        assertTrue("ClassWithField should be in the list of used classes", usedClasses.contains("ClassWithField"));
    }

    /**
     * Test parsing the kotlin_simple.dat file.
     * This test verifies that the KotlinInfoParser can parse a simple Kotlin file.
     * @throws Exception if there is an error reading the file
     */
    @Test
    public void testParseHelloKotlin() throws Exception
    {
        // Get the kotlin_simple.dat file
        File file = getResourceFile(getClass(), "/bluej/parser/kotlin/hello_kotlin.dat");
        assertNotNull("hello_kotlin.dat file should exist", file);

        // Create a reader for the file
        FileInputStream fis = new FileInputStream(file);

        try (fis) {
            InputStreamReader reader = new InputStreamReader(fis);
            ClassInfo info = KotlinInfoParser.parse(reader, new ClassLoaderResolver(this.getClass().getClassLoader()), "bluej.parser.kotlin.data");

            assertNotNull("Parsed ClassInfo should not be null", info);
            assertEquals("HelloKotlin", info.getName());

            List<String> usedClasses = info.getUsed();
            assertEquals("Used classes size should be 1", 1, usedClasses.size());
            assertTrue("JInitializer should be in the list of used classes", usedClasses.contains("JInitializer"));
        }
    }

    /**
     * Test that the KotlinInfoParser correctly identifies files with top-level functions.
     * This test verifies that the hasTopLevelFunctions property is set correctly.
     */
    @Test
    public void testTopLevelFunctionDetection()
    {
        // Create a StringReader with a Kotlin file that has top-level functions
        StringReader sr = new StringReader(
                "fun topLevelFunction() {\n" +
                "  println(\"This is a top-level function\")\n" +
                "}\n" +
                "\n" +
                "class SomeClass {\n" +
                "  fun classMethod() {\n" +
                "    println(\"This is a class method\")\n" +
                "  }\n" +
                "}\n"
        );

        ClassInfo info = KotlinInfoParser.parse(sr, new ClassLoaderResolver(this.getClass().getClassLoader()), "testpkg");

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("SomeClass", info.getName());
        assertTrue("File should be identified as having top-level functions", info.hasTopLevelFunctions());

        // Create a StringReader with a Kotlin file that has no top-level functions
        sr = new StringReader(
                "class SomeClass {\n" +
                "  fun classMethod() {\n" +
                "    println(\"This is a class method\")\n" +
                "  }\n" +
                "}\n"
        );

        info = KotlinInfoParser.parse(sr, new ClassLoaderResolver(this.getClass().getClassLoader()), "testpkg");

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("SomeClass", info.getName());
        assertFalse("File should not be identified as having top-level functions", info.hasTopLevelFunctions());
    }

    /**
     * Test that the KotlinInfoParser correctly identifies all public classes in a Kotlin file.
     * This test verifies that the getPublicClassNames method returns the correct list of class names.
     */
    @Test
    public void testPublicClassDetection() throws IOException
    {
        // Create a temporary file with multiple public classes
        File tempFile = File.createTempFile("KotlinTest", ".kt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(
                "fun topLevelFunction() {\n" +
                "  println(\"This is a top-level function\")\n" +
                "}\n" +
                "\n" +
                "public class FirstClass {\n" +
                "  fun classMethod() {\n" +
                "    println(\"This is a class method\")\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "public class SecondClass {\n" +
                "  fun anotherMethod() {\n" +
                "    println(\"This is another method\")\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "class NonPublicClass {\n" +
                "  fun hiddenMethod() {\n" +
                "    println(\"This is a hidden method\")\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "private class PrivateClass {\n" +
                "  fun privateMethod() {\n" +
                "    println(\"This is a private method\")\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "protected class ProtectedClass {\n" +
                "  fun protectedMethod() {\n" +
                "    println(\"This is a protected method\")\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "internal class InternalClass {\n" +
                "  fun internalMethod() {\n" +
                "    println(\"This is an internal method\")\n" +
                "  }\n" +
                "}\n"
            );
        }

        // Get the list of public classes
        List<String> publicClasses = KotlinInfoParser.getPublicClassNames(tempFile, 
            new ClassLoaderResolver(this.getClass().getClassLoader()));

        // Verify that the list contains the expected classes
        assertEquals("Should find 3 public classes", 3, publicClasses.size());
        assertTrue("Should contain FirstClass", publicClasses.contains("FirstClass"));
        assertTrue("Should contain SecondClass", publicClasses.contains("SecondClass"));
        assertTrue("Should contain NonPublicClass", publicClasses.contains("NonPublicClass"));
        assertFalse("Should not contain PrivateClass", publicClasses.contains("PrivateClass"));
        assertFalse("Should not contain ProtectedClass", publicClasses.contains("ProtectedClass"));
        assertFalse("Should not contain InternalClass", publicClasses.contains("InternalClass"));
    }

}
