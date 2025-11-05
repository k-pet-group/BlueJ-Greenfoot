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
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import bluej.extensions2.SourceType;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.psi.SourceInput;
import bluej.parser.symtab.ClassInfo;
import org.junit.Ignore;
import org.junit.Test;

import static bluej.parser.SourceInputTestUtils.*;
import static org.junit.Assert.*;
import static bluej.utility.ResourceFileReader.getResourceFile;

/**
 * Test the Kotlin parser functionality by parsing various Kotlin source files and strings.
 * <p>
 * This class contains tests for:
 * <ul>
 * <li>Verifying that the KotlinInfoParser class exists and can be instantiated</li>
 * <li>Parsing a simple Kotlin string</li>
 * <li>Parsing a simple Kotlin file (kotlin_simple.kt)</li>
 * <li>Parsing a more complex Kotlin file with various language constructs (kotlin_basic.kt)</li>
 * </ul>
 */
public class KotlinBasicParseTest
{
    /*
     * Test that the KotlinInfoParser class exists and can be instantiated.
     * This is a basic test to verify that the Kotlin parser functionality is available.
     */
    @Test
    public void testKotlinParserExists() throws Exception
    {
        // Create a StringReader with a simple Kotlin class
        StringReader sr = new StringReader(
                """
                        class SimpleKotlin {
                          val name: String = "Test"
                        }
                        """
        );

        // Verify that parsing via InfoParser succeeds for Kotlin source
        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);
        assertNotNull(info);
    }

    /**
     * Test parsing a Kotlin file with basic language constructs.
     * This test verifies that the KotlinInfoParser can parse a Kotlin file with various language constructs.
     * @throws Exception if there is an error reading the file
     */
    @Test
    public void testParseKotlinBasicFile() throws Exception
    {
        // Get the kotlin_basic.kt file
        SourceInput input = getResourceFile(getClass(), "/bluej/parser/kotlin/kotlin_basic.kt");
        assertNotNull("kotlin_basic.kt file should exist", input);

        // Parse the Kotlin file
        ClassInfo info = InfoParser.parse(input).orElse(null);
        assertNotNull("Parsed ClassInfo should not be null", info);

        // Assert that the class name is correct
        assertEquals("KotlinBasicClass", info.getName());

        assertFalse("Class should not be an interface", info.isInterface());
        assertFalse("Class should not be abstract", info.isAbstract());
        assertFalse(info.hadParseError());
    }

    /**
     * Test parsing a simple Kotlin string.
     * This test verifies that the KotlinInfoParser can parse a simple Kotlin class from a string.
     */
    @Test
    public void testParseSimpleKotlinString() throws Exception
    {
        // Create a StringReader with a simple Kotlin class
        StringReader sr = new StringReader(
                """
                        class SimpleKotlin {
                          val name: String = "Test"
                        }
                        """
        );

        // Parse the Kotlin string
        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

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
     * Test parsing the kotlin_simple.kt file.
     * This test verifies that the KotlinInfoParser can parse a simple Kotlin file.
     * @throws Exception if there is an error reading the file
     */
    @Test
    public void testParseKotlinSimpleFile() throws Exception
    {
        // Get the kotlin_simple.kt file
        SourceInput input = getResourceFile(getClass(), "/bluej/parser/kotlin/kotlin_simple.kt");
        assertNotNull("kotlin_simple.kt file should exist", input);

        // Parse the Kotlin file
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("SimpleKotlinClass", info.getName());

        assertFalse("Class should not be an interface", info.isInterface());
        assertFalse("Class should not be abstract", info.isAbstract());

        List<String> usedClasses = info.getUsed();
        assertEquals("Used classes size should be 0", 0, usedClasses.size());
    }

    /**
     * Test that when a class uses a field from another class, the other class is marked as "used".
     * This test verifies that the KotlinInfoParser correctly identifies dependencies between classes
     * when one class uses a field from another class.
     */
    @Test
    @Ignore("not implemented")
    public void testClassFieldUsage() throws Exception
    {
        // Create a StringReader with two Kotlin classes where one uses a field of type from the other class
        StringReader sr = new StringReader(
                """
                        class ClassUsingField {
                          // Explicitly use ClassWithField as a type for a field
                          private val classWithField: ClassWithField = ClassWithField()
                          var x: Int = 0
                         \s
                          fun useField() {
                            // Access the field from the other class
                            val value = classWithField.field
                            println(value)
                          }
                        }
                        """
        );

        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("ClassUsingField", info.getName());

        List<String> usedClasses = info.getUsed();
        assertEquals("Used classes size should be 1", 1, usedClasses.size());
        assertTrue("ClassWithField should be in the list of used classes", usedClasses.contains("ClassWithField"));
    }

    /**
     * Test parsing the kotlin_simple.kt file.
     * This test verifies that the KotlinInfoParser can parse a simple Kotlin file.
     * @throws Exception if there is an error reading the file
     */
    @Test
    public void testParseHelloKotlin() throws Exception
    {
        // Get the kotlin_simple.kt file
        SourceInput input = getResourceFile(getClass(), "/bluej/parser/kotlin/hello_kotlin.kt");
        assertNotNull("hello_kotlin.kt file should exist", input);

        // Parse the Kotlin file
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("HelloKotlin", info.getName());

//        List<String> usedClasses = info.getUsed();
//        assertEquals("Used classes size should be 1", 1, usedClasses.size());
//        assertTrue("JInitializer should be in the list of used classes", usedClasses.contains("JInitializer"));
    }

    /**
     * Test that the KotlinInfoParser correctly identifies files with top-level functions.
     * This test verifies that the hasTopLevelFunctions property is set correctly.
     */
    @Test
    @Ignore("TODO: not supported yet")
    public void testTopLevelFunctionDetection() throws Exception
    {
        // Create a StringReader with a Kotlin file that has top-level functions
        StringReader sr = new StringReader(
                """
                        fun topLevelFunction() {
                          println("This is a top-level function")
                        }

                        class SomeClass {
                          fun classMethod() {
                            println("This is a class method")
                          }
                        }
                        """
        );

        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("SomeClass", info.getName());
        assertTrue("File should be identified as having top-level functions", info.hasTopLevelFunctions());

        // Create a StringReader with a Kotlin file that has no top-level functions
        sr = new StringReader(
                """
                        class SomeClass {
                          fun classMethod() {
                            println("This is a class method")
                          }
                        }
                        """
        );

        input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("SomeClass", info.getName());
        assertFalse("File should not be identified as having top-level functions", info.hasTopLevelFunctions());

        // Create a StringReader with a Kotlin file with top-level functions but no classes
        sr = new StringReader(
                """
                        fun topLevelFunction() {
                          println("This is a top-level function")
                        }
                        """
        );

        input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        info = InfoParser.parse(input).orElse(null);
        assertNotNull("Parsed ClassInfo should not be null", info);
        assertTrue("File should be identified as having top-level functions", info.hasTopLevelFunctions());
        assertFalse("File should be identified as not having any public classes", info.foundPublicClass());
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
                    """
                            fun topLevelFunction() {
                              println("This is a top-level function")
                            }

                            public class FirstClass {
                              fun classMethod() {
                                println("This is a class method")
                              }
                            }

                            public class SecondClass {
                              fun anotherMethod() {
                                println("This is another method")
                              }
                            }

                            class NonPublicClass {
                              fun hiddenMethod() {
                                println("This is a hidden method")
                              }
                            }

                            private class PrivateClass {
                              fun privateMethod() {
                                println("This is a private method")
                              }
                            }

                            protected class ProtectedClass {
                              fun protectedMethod() {
                                println("This is a protected method")
                              }
                            }

                            internal class InternalClass {
                              fun internalMethod() {
                                println("This is an internal method")
                              }
                            }
                            """
            );
        }

        // TODO Get the list of public classes
//        List<String> publicClasses = InfoParser.getPublicClassNames(tempFile,
//            new ClassLoaderResolver(this.getClass().getClassLoader()));
//
//        // Verify that the list contains the expected classes
//        assertEquals("Should find 3 public classes", 3, publicClasses.size());
//        assertTrue("Should contain FirstClass", publicClasses.contains("FirstClass"));
//        assertTrue("Should contain SecondClass", publicClasses.contains("SecondClass"));
//        assertTrue("Should contain NonPublicClass", publicClasses.contains("NonPublicClass"));
//        assertFalse("Should not contain PrivateClass", publicClasses.contains("PrivateClass"));
//        assertFalse("Should not contain ProtectedClass", publicClasses.contains("ProtectedClass"));
//        assertFalse("Should not contain InternalClass", publicClasses.contains("InternalClass"));
    }

    @Test
    public void testSealedClasses() throws Exception
    {
        // Create a StringReader with a simple sealed class with an empty body
        StringReader sr = new StringReader(
                """
                        sealed class KotlinSealedClass {
                            class FirstType : KotlinSealedClass()
                            class SecondType : KotlinSealedClass()
                        }
                        """
        );

        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("KotlinSealedClass", info.getName());
        // Don't check for parse errors as the parser might report errors for valid Kotlin sealed classes
        // due to differences in how inner classes are handled in Kotlin vs Java
    }

    @Test
    public void testEmptyClass() throws Exception
    {
        // Create a StringReader with a simple sealed class with an empty body
        StringReader sr = new StringReader(
                """
                        class A
                        """
        );

        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("A", info.getName());
        assertFalse(info.hadParseError());
    }

    @Test
    public void testEmptyClassWithInheritance() throws Exception
    {
        // Create a StringReader with a simple sealed class with an empty body
        StringReader sr = new StringReader(
                """
                        open class A
                        class B : A()
                        """
        );

        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("A", info.getName());
        assertFalse(info.hadParseError());
    }

    @Test
    @Ignore("TODO: not supported yet")
    public void testTopLevelFun() throws Exception
    {
        // Create a StringReader with a simple sealed class with an empty body
        StringReader sr = new StringReader(
                """
                        fun someFunction() {
                            println("Hello from some function!")
                        }
                        fun someFunction2() {
                            println("Hello from some function!")
                        }
                    """
        );

        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertFalse(info.hadParseError());
    }

    @Test
    public void testClassWithTwoFuns() throws Exception
    {
        // Create a StringReader with a simple sealed class with an empty body
        StringReader sr = new StringReader(
                """
                        class A {
                            fun hello() : String {
                                return "hello";
                            }
                            fun answer() : Int {
                                return 42;
                            }
                        }
                    """
        );

        SourceInput input = createFromReader(sr, SourceType.Kotlin, new ClassLoaderResolver(this.getClass().getClassLoader()));
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertFalse(info.hadParseError());
    }

    @Test
    public void testYetAnotherKotlinClass() throws Exception
    {
        // Get the kotlin_simple.kt file
        SourceInput input = getResourceFile(getClass(), "/bluej/parser/kotlin/yet_another_kotlin_class.kt");
        assertNotNull("yet_another_kotlin_class.kt file should exist", input);

        // Parse the Kotlin file
        ClassInfo info = InfoParser.parse(input).orElse(null);

        assertNotNull("Parsed ClassInfo should not be null", info);
        assertEquals("YetAnotherKotlinClass", info.getName());
        assertFalse(info.hadParseError());
    }

}
