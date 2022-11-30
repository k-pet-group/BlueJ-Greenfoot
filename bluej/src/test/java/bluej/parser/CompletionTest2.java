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
import bluej.utility.Debug;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.OutputStreamWriter;
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
        Debug.setDebugStream(new OutputStreamWriter(System.out));
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

    private void assertTypeAtAandB(String expectedTypeNameA, String expectedTypeNameB, String javaSrc)
    {
        Parsed p = parse(javaSrc, resolver);
        resolver.addCompilationUnit("", p.node());

        Map.of("A", expectedTypeNameA, "B", expectedTypeNameB).forEach(( key, exp) -> {
            ExpressionTypeInfo suggests = p.node().getExpressionType(p.positionStart(key), p.doc());

            if (exp == null)
            {
                assertNull(key, suggests);
            }
            else
            {
                assertNotNull(key, suggests);
                assertEquals(key, exp, suggests.getSuggestionType().toString());
            }
        });
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

    private String withRecordDef(String middle)
    {
        return """
               public class Outer
               {
                   public record Coordinates(Integer x, Double y) {
                       public Float magnitude() { return Math.sqrt(x*x + y*y);}
                   }
                   public record PrefixedString(String prefix, String content) {}
                   public record PrefixedT<T>(String prefix, T content) {}
                   public record Arrays(int[] singleIntArray, Double[][] doubleDoubleArray) {}
                   public record VarargsPrim(int single, int... multiple) {}
                   public record VarargsObj(Integer single, Integer... multiple) {}
                   public record VarargsT<T>(int single, T... multiple) {}
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

    @Test
    public void testSwitch()
    {
        // The scope of a variable in a classic switch is all the way to the end, oddly:
        assertTypeAtA("java.lang.String", withLambdaDefs(
            """
            switch (3)
            {
                case 1:
                    String s = null;
                    break;
                case 2:
                    s./*A*/length();
                    break;
            }    
            """
        ));

        // But in a new simple switch, it's only the block it's in:
        assertTypeAtA(null, withLambdaDefs(
            """
            switch (3)
            {
                case 1 -> {
                    String s = null;
                }
                case 2 -> {
                    s./*A*/length();
                }
            }    
            """
        ));
    }

    @Test
    public void testStaticInner()
    {
        // Check that static items of non-static inner classes work:
        assertTypeAtA("java.lang.String", 
            """
            class Outer
            {
                class Inner
                {
                    static String s;
                }
                
                public void foo()
                {
                    Outer.Inner.s./*A*/length();
                }
            }    
            """
        );

        assertTypeAtA("java.lang.String",
            """
            class Grandparent
            {
                class Parent
                {
                    class Child
                    {
                        static String s;
                    }
                }
                
                public void foo()
                {
                    Grandparent.Parent.Child.s./*A*/length();
                }
            }    
            """
        );

        assertTypeAtA("java.lang.String",
            """
            class Grandparent
            {
                class Parent
                {
                    class Child
                    {
                        static String getString() {return ""};
                    }
                }
                
                public void foo()
                {
                    Grandparent.Parent.Child.getString()./*A*/length();
                }
            }    
            """
        );

    }
    
    @Test
    public void testRecord()
    {
        // public record Coordinates(Integer x, Double y) {
        //     public Float magnitude() { return Math.sqrt(x*x + y*y);}
        // }
        assertTypeAtA("Outer.Coordinates", withRecordDef("""
            public void foo(Coordinates c) {
                c./*A*/toString();
            }
            """));
        assertTypeAtA("java.lang.Float", withRecordDef("""
            public void foo(Coordinates c) {
                c.magnitude()./*A*/toString();
            }
            """));
        assertTypeAtA("java.lang.Integer", withRecordDef("""
            public void foo(Coordinates c) {
                c.x()./*A*/toString();
            }
            """));
        
        // public record PrefixedString(String prefix, String content) {}
        assertTypeAtA("java.lang.String", withRecordDef("""
            public void foo(PrefixedString p) {
                p.prefix()./*A*/toString();
            }
            """));
        
        // public record PrefixedT<T>(String prefix, T content) {}
        assertTypeAtA("java.lang.String", withRecordDef("""
            public void foo(PrefixedT<Integer> p) {
                p.prefix()./*A*/toString();
            }
            """));
        assertTypeAtA("java.lang.Integer", withRecordDef("""
            public void foo(PrefixedT<Integer> p) {
                p.content()./*A*/toString();
            }
            """));
        assertTypeAtA("java.lang.Integer", withRecordDef("""
            public void foo(PrefixedT<Integer> p) {
                p.content()./*A*/toString();
            }
            """));
        // public record Arrays(int[] singleIntArray, Double[][] doubleDoubleArray) {}
        assertTypeAtA("int[]", withRecordDef("""
            public void foo(Arrays x) {
                x.singleIntArray()./*A*/toString();
            }
            """));
        assertTypeAtA("java.lang.Double[][]", withRecordDef("""
            public void foo(Arrays x) {
                x.doubleDoubleArray()./*A*/toString();
            }
            """));
        assertTypeAtA("java.lang.Double[]", withRecordDef("""
            public void foo(Arrays x) {
                x.doubleDoubleArray()[0]./*A*/toString();
            }
            """));

        // public record VarargsObj(Integer single, Integer... multiple) {}
        assertTypeAtA("java.lang.Integer[]", withRecordDef("""
            public void foo(VarargsObj x) {
                x.multiple()./*A*/toString();
            }
            """));
        
        // public record VarargsPrim(int single, int... multiple) {}
        assertTypeAtA("int[]", withRecordDef("""
            public void foo(VarargsPrim x) {
                x.multiple()./*A*/toString();
            }
            """));

        // public record VarargsT<T>(int single, T... multiple) {}
        assertTypeAtA("java.lang.Double[]", withRecordDef("""
            public void foo(VarargsT<Double> x) {
                x.multiple()./*A*/toString();
            }
            """));
        assertTypeAtA("java.lang.Double[][][]", withRecordDef("""
            public void foo(VarargsT<Double[][]> x) {
                x.multiple()./*A*/toString();
            }
            """));
    }

    @Test
    public void testKeywords()
    {
        // This is to check that the parsing of the non-sealed keyword hasn't
        // interfered:
        assertTypeAtA("java.lang.String", withRecordDef("""
            public void foo() {
                String seald;
                Integer.toString(non-seald./*A*/length());
                // Note above is an expression, because the keyword is non-sealed, and above is missing an e
            }
            """));

        assertTypeAtA("java.lang.String", withRecordDef("""
            public void foo() {
                String sealed2;
                Integer.toString(non-sealed2./*A*/length());
                // Note above is an expression, because the keyword is non-sealed, and above has an extra 2
            }
            """));

        assertTypeAtA("Outer.Local", withRecordDef("""
            public void foo() {
                class Local { Integer non; }
                Local foo;
                Integer.toString(foo./*A*/non-sealed2);
                // This should also be an expression because the 2 should prevent the keyword being parsed
            }
            """));
    }
    
    /**
     * We have decided not to support the exact scoping of instanceof pattern matching,
     * as it's quite complex to get right.  Instead we will support a generous interpretation,
     * and offer completion on the variable anywhere where the analysis *might* have
     * shown it is in scope.  This method contains all the tests for where the exact
     * analysis would also offer completion.  The testIfInstanceofApproximate method
     * has all the tests which are technically incorrect; if we ever decide to redo it
     * properly those are the tests we would want to remove.
     */
    @Test
    public void testIfInstanceofExact()
    {
        // Should be in scope inside the if:
        assertTypeAtA("java.lang.Integer", withLambdaDefs("""
            public void ifInt(Object orig)
            {
                if (orig instanceof Integer x)
                    x./*A*/toString();
                }
            }
        """));
        
        // Should be in scope if then using an &&:
        assertTypeAtA( "java.lang.Integer", withLambdaDefs("""
            public void ifInt(Object orig)
            {
                if (orig instanceof Integer x && x./*A*/intValue() > 5) {
                    x.toString();
                }
            }
        """));
        
        // More complex example where the declaration is inside a sub-expression, but the scope should extend onwards:
        assertTypeAtA( "java.lang.Integer", withLambdaDefs("""
            public void ifInt(Object orig)
            {
                if ((orig instanceof Integer x && x.intValue() > 5) && x./*A*/toString()) {
                    x.toString();
                }
            }
        """));

        // Should work outside if, within an expression:
        assertTypeAtA("java.lang.String", withLambdaDefs("""
            public final boolean equals(Object o) {
                return (o instanceof String s) &&
                    s./*A*/equalsIgnoreCase(this);
            }
        """));
        
        // Negated patterns can then be in scope!
        assertTypeAtA("java.lang.String", withLambdaDefs("""
            public void onlyForStrings(Object o) throws MyException {
                if (!(o instanceof String s))
                    throw new Exception();
                // s is in scope
                System.out.println(s./*A*/length());
            }
        """));

        // De Morgan!
        assertTypeAtA("java.lang.String", withLambdaDefs("""
            public void onlyForStrings(Object o) throws MyException {
                if (!(o instanceof String s) || o.hashCode() == 0)
                    throw new Exception();
                // s is in scope
                System.out.println(s./*A*/length());
            }
        """));
        
    }
    
    @Test
    public void testIfInstanceofApproximate()
    {
        // It could be in scope outside the if, should it have been negated: 
        assertTypeAtA( "java.lang.Integer", withLambdaDefs("""
            public void ifInt(Object orig)
            {
                if (orig instanceof Integer x) {
                    x.toString();
                }
                x./*A*/toString();
            }
        """));

        // Ditto for an else:
        assertTypeAtA( "java.lang.Integer", withLambdaDefs("""
            public void ifInt(Object orig)
            {
                if (orig instanceof Integer x) {
                    x.toString();
                } else {
                    x./*A*/toString();
                }
            }
        """));

        // Ideally we'd notice this is || but actually we support anywhere later in the expression:
        assertTypeAtA( "java.lang.Integer", withLambdaDefs("""
            public void ifInt(Object orig)
            {
                if (orig instanceof Integer x || x./*A*/intValue() > 5) {
                    x.toString();
                }
            }
        """));

        // Ditto:
        assertTypeAtA( "java.lang.Integer", withLambdaDefs("""
            public void ifInt(Object orig)
            {
                if ((orig instanceof Integer x || x.intValue() > 5) && x./*A*/toString()) {
                    x.toString();
                }
            }
        """));


        // Name shadowing:
        assertTypeAtAandB("java.lang.String", "java.lang.String", withLambdaDefs("""
            Double s;
    
            void test1(Object o) {
                if (o instanceof String s) {
                    System.out.println(s./*A*/length());      // Field s is shadowed
                    s = s + "\n";               // Assignment to pattern variable
                }
                System.out.println(s./*B*/toString());          // Refers to pattern s
            }
        """));

        // Name shadowing part 2:
        assertTypeAtAandB("java.lang.String", "java.lang.Integer", withLambdaDefs("""
            Double s;
    
            void test1(Object o) {
                if (o instanceof String s) {
                    System.out.println(s./*A*/length());      // Field s is shadowed
                    s = s + "\n";               // Assignment to pattern variable
                }
                if (o instanceof Integer s) {
                    System.out.println(s./*B*/toString());          // Refers to integer s
                }
            }
        """));

        // Name shadowing part 3:
        assertTypeAtAandB("java.lang.String", "java.lang.Integer", withLambdaDefs("""
            Double s;
    
            void test1(Object o) {
                if (o instanceof String s) {
                    System.out.println(s.length());      // Field s is shadowed
                    s = s + "\n";               // Assignment to pattern variable                
                }
                s./*A*/toString();
                if (o instanceof Integer s) {
                    System.out.println(s.toString());          // Refers to integer s
                }
                s./*B*/toString();
            }
        """));

        // Name shadowing part 4:
        assertTypeAtAandB("java.lang.Double", "java.lang.String", withLambdaDefs("""
            Double s;
    
            void test1(Object o) {
                s./*A*/toString();
                if (o instanceof String s) {
                    System.out.println(s.length());      // Field s is shadowed
                    s = s + "\n";               // Assignment to pattern variable                
                }
                s./*B*/toString();
            }
        """));
    }

    @Test
    public void testIfInstanceofArrays()
    {
        // Test arrays:
        assertTypeAtA("java.lang.Integer[][]", withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        if (orig instanceof Integer[][] x)
                            x./*A*/toString();
                        }
                    }
                """));
        // Primitive arrays
        assertTypeAtA("int[][][]", withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        if (orig instanceof int[][][] x)
                            x./*A*/toString();
                        }
                    }
                """));
    }
    
    @Test
    public void testIfInstanceofNestedScopes()
    {
        // Nested scopes #1:
        assertTypeAtA("java.lang.Integer[][]", withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        {
                            if (orig instanceof Integer[][] x)
                                x./*A*/toString();
                            }
                        }
                    }
                """));
        // Nested scopes #2  (our incorrect approximation):
        assertTypeAtA("java.lang.Integer", withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        {
                            if (orig instanceof Integer x)
                                
                            }
                        }
                        x./*A*/toString();
                    }
                """));
        // Nested scopes #3 (our incorrect approximation):
        assertTypeAtA("java.lang.Integer", withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        while (true) {
                            if (orig instanceof Integer x)
                                
                            }
                        }
                        x./*A*/toString();
                    }
                """));
        // Nested scopes #4 (our incorrect approximation):
        assertTypeAtA("java.lang.Integer", withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        try {
                            if (orig instanceof Integer x)
                                
                            }
                        } finally {}
                        x./*A*/toString();
                    }
                """));
    }
    
    @Test
    public void testIfInstanceofLambdas()
    {
        // instanceof variables should not escape block lambdas:
        assertTypeAtA(null, withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        withInteger(s -> {
                            if (s instanceof Integer x)
                                
                            }
                        });
                        x./*A*/toString();
                    }
                """));
        // instanceof variables should not escape expression lambdas:
        assertTypeAtA(null, withLambdaDefs("""
                    public void ifInt(Object orig)
                    {
                        withInteger(s -> (s instanceof Integer x));
                        x./*A*/toString();
                    }
                """));
    }
}
