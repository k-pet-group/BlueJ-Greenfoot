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
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        TestableDocument document = new TestableDocument(resolver, SourceType.Kotlin);
        document.enableParser(true);
        document.insertString(0, sourceCode);
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
    private GenTypeClass parseAndResolveClass(String sourceCode, String className, String packageName)
    {
        ParsedCUNode parsedNode = cuForSource(sourceCode, packageName);
        resolver.addCompilationUnit(packageName, parsedNode);

        EntityResolver entityResolver = new PackageResolver(this.resolver, packageName);
        TypeEntity classEntity = entityResolver.resolvePackageOrClass(className, null).resolveAsType();
        return classEntity.getType().asClass();
    }

    @Test
    public void testNestedClassParsing()
    {
        String sourceCode = ""
            + "class A\n"       // position 0
            + "{\n"             // position 8 
            + "   class B\n"    // position 10 
            + "    {\n"         // position 21 
            + "    }\n"
            + "}\n";
            
        ParsedCUNode pcuNode = cuForSource(sourceCode, "");
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
    public void testMethodRecognition()
    {
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

        GenTypeClass aClass = parseAndResolveClass(aClassSrc, "A", "");
        assertMethodExists(aClass, "hello", "java.lang.String");
        assertMethodExists(aClass, "answer", "int");
    }

    @Test
    public void testComplexKotlinClass()
    {
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

        GenTypeClass aClass = parseAndResolveClass(source, "YetAnotherKotlinClass", "");
        assertMethodExists(aClass, "sampleMethod", "int");
    }

    @Test
    public void testKotlinClassWithPackage()
    {
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

        GenTypeClass aClass = parseAndResolveClass(source, "HelloKotlin", "my.kotlin");
        assertMethodExists(aClass, "sampleMethod", "int");
        // Note: Kotlin Unit type maps to int in the parser, not void
        assertMethodExists(aClass, "sayHello", "kotlin.Unit");
    }
}
