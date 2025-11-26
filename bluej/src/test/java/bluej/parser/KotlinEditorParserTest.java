/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2019,2022  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.MethodReflective;
import bluej.extensions2.SourceType;
import bluej.parser.entity.*;
import bluej.parser.nodes.*;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class KotlinEditorParserTest
{
    @BeforeClass
    public static void initConfig()
    {
        InitConfig.init();
    }
    
    private TestEntityResolver resolver;
    
    @Before
    public void setUp() throws Exception
    {
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode, String pkg, boolean throwOnException) throws ParseException {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        TestableDocument document = new TestableDocument(resolver, SourceType.Kotlin);
        document.enableParser(true);
        document.insertString(0, sourceCode);
        List<String> parseErrors = document.getParseErrors();
        if(!parseErrors.isEmpty() && throwOnException) {
            String msg = String.join("\n", parseErrors);
            throw new ParseException(msg, 0);
        }
        return document.getParser();
    }

    /**
     * Helper method to assert that a method exists in a class with the expected return type.
     */
    private void assertMethodExists(GenTypeClass aClass, String methodName, String expectedReturnType)
    {
        Map<String,Set<MethodReflective>> methods = aClass.getReflective().getDeclaredMethods();
        Set<MethodReflective> methodSet = methods.get(methodName);
        assertNotNull("Method " + methodName + " should exist", methodSet);
        assertEquals("Method " + methodName + " should have exactly one overload", 1, methodSet.size());
        MethodReflective method = methodSet.iterator().next();
        assertEquals("Method " + methodName + " should have correct return type",
                     expectedReturnType, method.getReturnType().toString(false));
    }

    /**
     * Helper method to parse source code and resolve a class for testing.
     */
    private GenTypeClass parseAndResolveClass(String sourceCode, String className, String packageName, boolean printResult, boolean throwOnException) throws ParseException {
        ParsedCUNode parsedNode = cuForSource(sourceCode, packageName, throwOnException);
        resolver.addCompilationUnit(packageName, parsedNode);
        if (printResult) {
            ParsedCUNode.printTree(parsedNode, 0, 0);
        }
        EntityResolver entityResolver = new PackageResolver(this.resolver, packageName);
        TypeEntity classEntity = entityResolver.resolvePackageOrClass(className, null).resolveAsType();
        return classEntity.getType().asClass();
    }

    public static void printLinesWithPositions(String input) {
        String[] lines = input.split("\n", -1); // keep empty lines
        int offset = 0;

        // Determine max width of line content for alignment, excluding empty lines
        int maxContentLength = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                maxContentLength = Math.max(maxContentLength, line.length());
            }
        }

        // Determine width for line numbers
        int maxLineNumberWidth = String.valueOf(lines.length).length();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineStart = offset;
            int from = -1;
            int to = -1;

            for (int j = 0; j < line.length(); j++) {
                if (!Character.isWhitespace(line.charAt(j))) {
                    if (from == -1) {
                        from = lineStart + j;
                    }
                    to = lineStart + j;
                }
            }

            String lineNumber = String.format("%" + maxLineNumberWidth + "d", i + 1);
            if (from != -1) {
                String paddedLine = String.format("%-" + maxContentLength + "s", line);
                String annotation = "//from=" + from + " to=" + to;
                System.out.println(lineNumber + ": " + paddedLine + " " + annotation);
            } else {
                // Just print line number and original line (empty or whitespace only)
                System.out.println(lineNumber + ": " + line);
            }

            offset += line.length() + 1; // account for '\n'
        }
    }


    @Test
    public void testNestedClassParsing() throws ParseException {
        String sourceCode = ""
            + "class A\n"       // position 0
            + "{\n"             // position 8 
            + "   class B\n"    // position 10 
            + "    {\n"         // position 21 
            + "    }\n"
            + "}\n";

        ParsedCUNode pcuNode = cuForSource(sourceCode, "", true);
        NodeAndPosition<ParsedNode> classNP = pcuNode.findNodeAtOrAfter(0, 0);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNP.getNode().getNodeType());
        assertEquals(0, classNP.getPosition());
        
        NodeAndPosition<ParsedNode> innerNP = classNP.getNode().findNodeAtOrAfter(9, 0);
        
        NodeAndPosition<ParsedNode> classBNP = innerNP.getNode().findNodeAtOrAfter(innerNP.getPosition(),
                innerNP.getPosition());
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classBNP.getNode().getNodeType());
        assertEquals(13, classBNP.getPosition());
    }

    /**
     * Test that a method defined inside a class is recognized properly.
     */
    @Test
    public void testMethodRecognition() throws ParseException {
        String aClassSrc = """
                class A {
                  fun hello() : String {
                    return "hello";
                  }
                  fun answer() : Int {
                    return 42;
                  }
                }
                """;

        GenTypeClass aClass = parseAndResolveClass(aClassSrc, "A", "", false, true);
        assertMethodExists(aClass, "hello", "java.lang.String");
        assertMethodExists(aClass, "answer", "int");
    }

    @Test
    public void testComplexKotlinClass() throws ParseException {
        String source = """
                /**
                 * Write a description of class YetAnotherKotlinClass here.
                 *
                 * @author (your name)
                 * @version (a version number or a date)
                 */
                class YetAnotherKotlinClass {
                    // instance variables - replace the example below with your own
                    private var x: Int = 0

                    /**
                     * Constructor for objects of class YetAnotherKotlinClass
                     */
                    init {
                        // initialise instance variables
                        x = 0
                    }

                    /**
                     * An example of a method - replace this comment with your own
                     *
                     * @param  y  a sample parameter for a method
                     * @return    the sum of x and y
                     */
                    fun sampleMethod(y: Int): Int {
                        // put your code here
                        return x + y
                    }
                }
                """;

        GenTypeClass aClass = parseAndResolveClass(source, "YetAnotherKotlinClass", "", false, true);
        assertMethodExists(aClass, "sampleMethod", "int");
    }

    @Test
    public void testKotlinClassWithPackage() throws ParseException {
        String source = """
                package my.kotlin
                /**
                 * Write a description of class HelloKotlin here.
                 *
                 * @author (your name)
                 * @version (a version number or a date)
                 */
                class HelloKotlin {
                    // instance variables - replace the example below with your own
                    private val jinit: JInitializer = JInitializer();
                    private var x: Int = 0

                    /**
                     * Constructor for objects of class HelloKotlin
                     */
                    init {
                        // initialise instance variables
                        x = jinit.getInitialValue();
                    }

                    /**
                     * An example of a method - replace this comment with your own
                     *
                     * @param  y  a sample parameter for a method
                     * @return    the sum of x and y
                     */
                    fun sampleMethod(y: Int): Int {
                        println("Running computation in Kotlin.")
                        return x + y
                    }
                    
                    fun sayHello(sender: String) {
                        println("Hello from Kotlin! Sender: {" + sender + "}")
                    }
                }
                """;

        GenTypeClass aClass = parseAndResolveClass(source, "HelloKotlin", "my.kotlin", false, true);
        assertMethodExists(aClass, "sampleMethod", "int");
        // Note: Kotlin Unit type maps to int in the parser, not void
        assertMethodExists(aClass, "sayHello", "kotlin.Unit");
    }

    @Test
    public void testKotlinClassWithReadPropertyAndMethod() throws ParseException {
        String source = """
                class Dog {
                    val name: String
                      get() {
                          return "sparky"
                      }
                
                    fun bark() {
                          var i = 0;
                          while (i < 5) {
                              print(name)
                          }
                          for (x in 1..5) {
                              print(name + "!");
                          }
                    }
                }
                """;

//        printLinesWithPositions(source);
        /*
         1: class Dog {                      //from=0 to=10
         2:     val name: String             //from=16 to=31
         3:       get() {                    //from=39 to=45
         4:           return "sparky"        //from=57 to=71
         5:       }                          //from=79 to=79
         6:
         7:     fun bark() {                 //from=86 to=97
         8:           var i = 0;             //from=109 to=118
         9:           while (i < 5) {        //from=130 to=144
        10:               print(name)        //from=160 to=170
        11:           }                      //from=182 to=182
        12:           for (x in 1..5) {      //from=194 to=210
        13:               print(name + "!"); //from=226 to=243
        14:           }                      //from=255 to=255
        15:     }                            //from=261 to=261
        16: }                                //from=263 to=263
         */

        GenTypeClass aClass = parseAndResolveClass(source, "Dog", "", false, true);
        assertMethodExists(aClass, "bark", "kotlin.Unit");

        ParsedCUNode parsedNode = cuForSource(source, "", true);
        resolver.addCompilationUnit("", parsedNode);
//        ParsedCUNode.printTree(parsedNode, 0, 0);
        NodeAndPosition<ParsedNode> nap = parsedNode.findNodeAt(194, 0);
        nap = nap.getNode().findNodeAt(194, nap.getPosition());
        nap = nap.getNode().findNodeAt(194, nap.getPosition());
        nap = nap.getNode().findNodeAt(194, nap.getPosition());
        nap = nap.getNode().findNodeAt(194, nap.getPosition());
        assertTrue("For loop node must be ContainerNode", nap.getNode() instanceof ContainerNode);
        assertEquals("For loop size is incorrect", 255-194+1, nap.getSize());
    }

    @Test
    @Ignore("Needs proper property parsing implementation")
    public void testKotlinClassWithReadPropertyAndMethod2() throws ParseException {
        String source = """
                class Dog {
                    val name
                      get() : String {
                          return "sparky"
                      }
                
                    fun bark() {
                        
                    }
                }
                """;

        printLinesWithPositions(source);
        /*
         1: class Dog {               //from=0 to=10
         2:     val name              //from=16 to=23
         3:       get() : String {    //from=31 to=46
         4:           return "sparky" //from=58 to=72
         5:       }                   //from=80 to=80
         6:
         7:     fun bark() {          //from=87 to=98
         8:
         9:     }                     //from=105 to=105
        10: }                         //from=107 to=107
         */

        GenTypeClass aClass = parseAndResolveClass(source, "Dog", "", false, true);
        assertMethodExists(aClass, "bark", "kotlin.Unit");

        ParsedCUNode parsedNode = cuForSource(source, "", true);
        resolver.addCompilationUnit("", parsedNode);
        ParsedCUNode.printTree(parsedNode, 0, 0);
        NodeAndPosition<ParsedNode> nap = parsedNode.findNodeAt(16, 0);
        nap = nap.getNode().findNodeAt(16, nap.getPosition());
        nap = nap.getNode().findNodeAt(16, nap.getPosition());
        assertTrue("Property declaration node must be FieldNode", nap.getNode() instanceof FieldNode);
        assertEquals("Propery declaration node must include both getterName and setterName", 80-16+1, nap.getSize());
    }

    @Test
    public void testNestedLoops() throws ParseException {
        String source = """
            class Dog {
                fun bark() {
                    while(true) {
                        break
                        while(true) {
                            println("")
                            break
                        }
                    }
                }
            }
            """;

        // printLinesWithPositions(source);
        /*
             1: class Dog {                 //from=0 to=10
             2:     fun bark() {            //from=16 to=27
             3:         while(true) {       //from=37 to=49
             4:             break           //from=63 to=67
             5:             while(true) {   //from=81 to=93
             6:                 println("") //from=111 to=121
             7:                 break       //from=139 to=143
             8:             }               //from=157 to=157
             9:         }                   //from=167 to=167
            10:     }                       //from=173 to=173
            11: }                           //from=175 to=175
            12:
         */
        GenTypeClass aClass = parseAndResolveClass(source, "Dog", "", false, true);
        assertMethodExists(aClass, "bark", "kotlin.Unit");

        ParsedCUNode parsedNode = cuForSource(source, "", false);
//        NodeAndPosition<ParsedNode> nap = parsedNode.findNodeAt(111, 0); // class
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // class body
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // method
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // method body
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // while loop node
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // while loop body node
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // while body contents node
//        assertTrue("Outer while node must be ContainerNode", nap.getNode() instanceof ContainerNode);
//        // End brace - start brace + 1
//        var expectedOuterLoopSize = 167 - 49 + 1;
//        assertEquals("Outer while size is incorrect",expectedOuterLoopSize, nap.getSize());
//
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // statement node (?)
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // while loop node
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // while body node
//        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // while body node
//        assertTrue("Inner while node must be ContainerNode", nap.getNode() instanceof ContainerNode);
//        // End brace - start brace + 1
//        var innerLoopSize = 157 - 93 + 1;
//        assertEquals("Inner while size is incorrect", innerLoopSize, nap.getSize());

        NodeTree.NodeAndPosition<ParsedNode> nap = parsedNode.findNodeAt(111, 0); // class
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // class body (it's internal span)
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // method
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // method body (it's internal span)
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // outer while loop node
        assertTrue("Outer while node must be ContainerNode", nap.getNode() instanceof ContainerNode);
        // End brace - start brace + 1
        var expectedOuterLoopSize = 167 - 37 + 1;
        assertEquals("Outer while size is incorrect",expectedOuterLoopSize, nap.getSize());

        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // outer while loop body node (it's internal span)
        nap = nap.getNode().findNodeAt(111, nap.getPosition()); // inner while loop node
        assertTrue("Inner while node must be ContainerNode", nap.getNode() instanceof ContainerNode);
        // End brace - start brace + 1
        var innerLoopSize = 157 - 81 + 1;
        assertEquals("Inner while size is incorrect", innerLoopSize, nap.getSize());

    }

    @Test
    public void testWhileWithoutBlock() throws ParseException {
        String source = """
                class Dog {
                    fun bark() {
                      while(true)
                          break
                    }
                }
                """;

//        printLinesWithPositions(source);
        GenTypeClass aClass = parseAndResolveClass(source, "Dog", "", false, true);
        assertMethodExists(aClass, "bark", "kotlin.Unit");
    }

    @Test
    @Ignore("Needs proper property parsing implementation")
    public void testKotlinClassWithVarProperty() throws ParseException {
        String source = """
                class Dog {
                     var name = "sparky"
                       get() : String {
                           return field+"!"
                       }
                       set(value) {
                         field = value
                       }

                     fun bark() {
                         println(name)
                     }
                 }
                """;

        printLinesWithPositions(source);
        /*
         1: class Dog {                 //from=0 to=10
         2:      var name = "sparky"    //from=17 to=35
         3:        get() : String {     //from=44 to=59
         4:            return field+"!" //from=72 to=87
         5:        }                    //from=96 to=96
         6:        set(value) {         //from=105 to=116
         7:          field = value      //from=127 to=139
         8:        }                    //from=148 to=148
         9:
        10:      fun bark() {           //from=156 to=167
        11:          println(name)      //from=178 to=190
        12:      }                      //from=197 to=197
        13:  }                          //from=200 to=200
         */

        GenTypeClass aClass = parseAndResolveClass(source, "Dog", "", false, false);
        assertMethodExists(aClass, "bark", "kotlin.Unit");

        ParsedCUNode parsedNode = cuForSource(source, "", false);
        resolver.addCompilationUnit("", parsedNode);
        ParsedCUNode.printTree(parsedNode, 0, 0);
        NodeAndPosition<ParsedNode> nap = parsedNode.findNodeAt(17, 0);
        nap = nap.getNode().findNodeAt(17, nap.getPosition());
        nap = nap.getNode().findNodeAt(17, nap.getPosition());
        assertTrue("Property declaration node must be FieldNode", nap.getNode() instanceof FieldNode);
        assertEquals("Propery declaration node must include both getterName and setterName", 148-17+1, nap.getSize());
    }

}
