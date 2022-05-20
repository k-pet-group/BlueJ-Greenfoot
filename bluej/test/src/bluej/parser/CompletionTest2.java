/*
 This file is part of the BlueJ program. 
 Copyright (C) 2022  Michael Kolling and John Rosenberg
 
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

import bluej.JavaFXThreadingRule;
import bluej.parser.ParseUtility.StartEnd;
import bluej.parser.entity.ClassLoaderResolver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static bluej.parser.ParseUtility.Parsed;
import static bluej.parser.ParseUtility.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test for code completion, especially around lambdas and
 * features from Java 11-17 inclusive.
 */
public class CompletionTest2
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

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
    
    @Test
    public void testLocations()
    {
        // Test that our location mechanism is working correctly:
        Parsed p = parse("""
                /*A*/int /**B*/x
                /* C */ = 99;/*D*/
                """, resolver);
        // Hand-calculated positions:
        assertEquals(Map.of(
            "A",new StartEnd(0, 5),
                "*B", new StartEnd(9, 15),
                "C", new StartEnd(17, 24),
                "D", new StartEnd(30, 35)
        ), p.positions());
    }
    
    /**
     * Asserts that the type at position "A" in the source, i.e. where
     * / * A * / (without spaces) occurs in the source is the given one.
     * @param expectedTypeName The expected fully-qualified type name, or null if you expect the type to be unavailable
     * @param javaSrc The Java source code                        
     */
    private void assertTypeAtA(String expectedTypeName, String javaSrc)
    {
        Parsed p = parse(javaSrc, resolver);
        resolver.addCompilationUnit("", p.node());

        ExpressionTypeInfo suggests = p.node().getExpressionType(p.positionStart("A"), p.doc());
        if (expectedTypeName == null)
        {
            assertNull(suggests);
        }
        else
        {
            assertNotNull(suggests);
            assertEquals(expectedTypeName, suggests.getSuggestionType().toString());
        }
    }
    
    /**
     * Wraps the given Java in a class with some lambda utility functions
     */
    private String withLambdaDefs(String middle)
    {
        return """
               import java.util.function.*;
               public class Foo
               {
                   private static void withInteger(Consumer<Integer> c) {}
                   private static void withStringAndInteger(BiConsumer<String, Integer> c) {}
               """ + middle + """
               }
               """;
    }

    @Test
    public void testCastLiteral()
    {
        assertTypeAtA("java.lang.Integer", withLambdaDefs( 
    "{((Integer)1)./*A*/toString();}"
        ));
    }

    @Test
    public void testInner()
    {
        // Check that inner classes work right when nested multiple-levels inside an expression:
        assertTypeAtA("java.lang.Integer", withLambdaDefs( 
        "{return 1 + (2 + new Object(){ Integer x; {x./*A*/.toString();} }.hashCode());}"
        ));
        // And similar for lambdas:
        assertTypeAtA("java.lang.Integer", withLambdaDefs(
            "{return 1 + (2 + (() -> { Integer x; x./*A*/.toString();}).hashCode());}"
        ));
    }
    
    @Test
    public void testLambda()
    {
        // We only support auto complete when type is specified explicitly:
        
        // Inferred type:
        assertTypeAtA(null, withLambdaDefs(
                "{withInteger(x -> x./*A*/toString());}"
        ));
        // Inferred type with var:
        assertTypeAtA(null, withLambdaDefs(
                "{withInteger((var x) -> x./*A*/toString());}"
        ));

        // Explicit type:
        assertTypeAtA("java.lang.Integer", withLambdaDefs(
                "{withInteger((Integer x) -> x./*A*/toString());}"
        ));
        
        
        // Check scope works inside lambda with a block:
        assertTypeAtA("java.lang.Integer", withLambdaDefs("""
                {
                    withInteger((Integer x) -> {
                        if (true)
                        {
                            x./*A*/toString();
                        }
                    });
                }
                """
        ));
        assertTypeAtA("java.lang.Double", withLambdaDefs("""
                {
                    withInteger((Integer x) -> {
                        if (true)
                        {
                            Double y = 0.0;
                            y./*A*/toString();
                        }
                    });
                }
                """
        ));
        // Check scope works inside nested lambda:
        assertTypeAtA("java.lang.Integer", withLambdaDefs("""
                {
                    withInteger((Integer x) -> {
                        withInteger((Integer y) -> {
                            x./*A*/toString();
                            y.toString();
                        });
                    });
                }
                """
        ));
        assertTypeAtA("java.lang.Integer", withLambdaDefs("""
                {
                    withInteger((Integer x) -> {
                        withInteger((Integer y) -> {
                            x.toString();
                            y./*A*/toString();
                        });
                    });
                }
                """
        ));
        
        // Check scope doesn't extend outside lambda:
        assertTypeAtA(null, withLambdaDefs(
            """
            {
                withInteger((Integer x) -> {});
                x./*A*/toString();
            }
            """
        ));
        assertTypeAtA(null, withLambdaDefs(
            """
            {
                int r = withInteger((Integer x) -> {}) + x./*A*/toString();
            }
            """
        ));
        
        // Check multiple parameters:
        assertTypeAtA("java.lang.Integer", withLambdaDefs(
            "{withStringAndInteger((String s, Integer x) -> x./*A*/toString());}"
        ));
        assertTypeAtA("java.lang.String", withLambdaDefs(
            "{withStringAndInteger((String s, Integer x) -> s./*A*/length());}"
        ));
    }
}
