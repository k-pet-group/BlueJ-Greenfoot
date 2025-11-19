/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2016,2017,2022  Michael Kolling and John Rosenberg

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

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import bluej.extensions2.SourceType;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.psi.SourceInput;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class NewParserTest
{
    /**
     * Test array as type parameter
     */
    @Test
    public void test1()
    {
        StringReader sr = new StringReader(
                "LinkedList<String[]>"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        List<LocatableToken> ll = new LinkedList<LocatableToken>();
        assertTrue(ip.parseTypeSpec(false, true, ll));
        // 6 tokens: LinkedList, '<', String, '[', ']', '>'
        assertEquals(6, ll.size());
    }

    /**
     * Test handling of '>>' sequence in type spec
     */
    @Test
    public void test2()
    {
        StringReader sr = new StringReader(
                "LinkedList<List<String[]>>"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        List<LocatableToken> ll = new LinkedList<LocatableToken>();
        assertTrue(ip.parseTypeSpec(false, true, ll));
        // 8 tokens: LinkedList, '<', List, '<', String, '[', ']', '>>'
        assertEquals(8, ll.size());
    }

    /**
     * Test multiple type parameters
     */
    @Test
    public void test3()
    {
        StringReader sr = new StringReader(
                "Map<String,Integer> v1; "
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test generic inner class of generic outer class
     */
    @Test
    public void test4()
    {
        StringReader sr = new StringReader(
                "Outer<String>.Inner<String> v8; "
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test wildcard type parameters
     */
    @Test
    public void test5()
    {
        StringReader sr = new StringReader(
                "A<?> v8; " +
                "A<? extends String> v9; " +
                "A<? super String> v10;"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
        ip.parseStatement();
        ip.parseStatement();
    }

    /**
     * Test less-than operator.
     */
    @Test
    public void test6()
    {
        StringReader sr = new StringReader(
                "b = (i < j);"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test a funky statement.
     */
    @Test
    public void test7()
    {
        StringReader sr = new StringReader(
                "boolean.class.equals(T.class);"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test a class declaration with a single type parameter.
     */
    @Test
    public void test8()
    {
        StringReader sr = new StringReader(
                "class A<T>{}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test a class declaration containing a semi-colon
     */
    @Test
    public void test9()
    {
        StringReader sr = new StringReader(
                "class A{;}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test a simple enum
     */
    @Test
    public void test10()
    {
        StringReader sr = new StringReader(
                "enum A {" +
                "    one, two, three;" +
                "    private int x;" +
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test array declarators after a variable name.
     */
    @Test
    public void test11()
    {
        StringReader sr = new StringReader(
                "int a[] = {1, 2, 3};"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test array declarators after a method parameter name.
     */
    @Test
    public void test12()
    {
        StringReader sr = new StringReader(
                "int a[], int[] b);"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseMethodParamsBody();
    }

    /**
     * Test array declarators after a field name.
     */
    @Test
    public void test13()
    {
        StringReader sr = new StringReader(
                "class A { int x[] = {1,2,3}, y = 5; }"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test multiple field definition in one statement.
     */
    @Test
    public void test13p2()
    {
        StringReader sr = new StringReader(
                "class A { private int x, y; }"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test multiple variable declaration in a single statement.
     */
    @Test
    public void test14()
    {
        StringReader sr = new StringReader(
                "int x[], y = 3, z, q;"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test annotation declaration
     */
    @Test
    public void test15()
    {
        StringReader sr = new StringReader(
                "public @interface Copyright{  String value();}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test use of marker annotation
     */
    @Test
    public void test16()
    {
        StringReader sr = new StringReader(
                "@Preliminary public class TimeTravel { }"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test the use of an annotation.
     */
    @Test
    public void test17()
    {
        StringReader sr = new StringReader(
                "@Copyright(\"2002 Yoyodyne Propulsion Systems\")"+
                "public class NewParserTest { }"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test the '?:' operator.
     */
    @Test
    public void testQuestionOperator()
    {
        StringReader sr = new StringReader(
                "Object g = (x<y) ? null : null;"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test a static method call.
     */
    @Test
    public void testStaticMethodCall()
    {
        StringReader sr = new StringReader(
                "AAA.bbb(1,2,3);"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test the declaration of an annotation.
     */
    @Test
    public void test18()
    {
        StringReader sr = new StringReader(
                "public @interface RequestForEnhancement { " +
                "int id();" +
                "String synopsis();"+
                "String engineer()  default \"[unassigned]\"; "+
                "String date()      default \"[unimplemented]\"; "+
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }


    /**
     * Test the use of an annotation.
     */
    @Test
    public void test19()
    {
        StringReader sr = new StringReader(
                "public @RequestForEnhancement(" +
                "id       = 2868724," +
                "synopsis = \"Enable time-travel\","+
                "engineer = \"Mr. Peabody\", "+
                "date     = \"4/1/3007\""+
                ")"+
                "static void travelThroughTime(Date destination) { } }"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseClassBody();
    }

    /**
     * Test the use of an annotation for a method.
     */
    @Test
    public void test20()
    {
        StringReader sr = new StringReader(
                "@Test public static void m1() { } }"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseClassBody();
    }

    /**
     * Test the use of a qualified annotation
     */
    @Test
    public void test21()
    {
        StringReader sr = new StringReader(
                "@Test.RequestForEnhancement int req;"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }



    @Test
    public void test22()
    {
        StringReader sr = new StringReader(
                "@Expression(\"execution(* com.mypackage.Target.*(..))\") "+
                "Pointcut pc1; "
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();

    }

    @Test
    public void test23()
    {
        StringReader sr = new StringReader(
                "@Expression(\"execution(* com.mypackage.Target.*(..))\") "+
                "volatile Pointcut pc1; "
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    @Test
    public void test24()
    {
        StringReader sr = new StringReader(
                "(byte)++(bb)"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseExpression();
    }

    @Test
    public void test25()
    {
        StringReader sr = new StringReader(
                "new String[]{\"hello\", \"goodbye\",}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseExpression();
    }

    // Lambda syntax tests
    private void checkLambdaExpression(String s)
    {
        // test when parenthesized:
        StringReader sr = new StringReader("(" + s + ")");
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseExpression();

        // test when used in assigment:
        sr = new StringReader("Runnable r = " + s + ";");
        input = SourceInput.fromReader(sr, SourceType.Java);
        ip = new SourceParser(input);
        ip.parseStatement();

        // test when used as method parameter:
        sr = new StringReader("doSomething(" + s + ");");
        input = SourceInput.fromReader(sr, SourceType.Java);
        ip = new SourceParser(input);
        ip.parseStatement();
    }


    @Test
    public void testLambdaNoParameters1()
    {
        checkLambdaExpression("() -> {}");
    }

    @Test
    public void testLambdaNoParameters2()
    {
        checkLambdaExpression("() -> 42");   // No parameters; expression body
    }

    @Test
    public void testLambdaNoParameters3()
    {
        checkLambdaExpression("() -> null"); // No parameters; expression body
    }

    @Test
    public void testLambdaNoParameters4()
    {
        checkLambdaExpression("() -> {return 42;}"); // No parameters; block body with return
    }

    @Test
    public void testLambdaNoParameters5()
    {
        checkLambdaExpression("() -> System.gc()"); // No parameters; void block body
    }

    @Test
    public void testLambdaNoParameters6()
    {
        String s = "() -> {\n "
                + "    if (true) return 12;\n"
                + "    else {\n"
                + "        int result = 15;\n"
                + "        for (int i = 1; i < 10; i++)\n"
                + "            result *= i;\n"
                + "        return result;\n"
                + "    }\n"
                + "}\n"; // Complex block body with returns

        checkLambdaExpression(s);
    }

    @Test
    public void testLambdaSingleParameter1()
    {
        checkLambdaExpression("(int x) -> x+1"); // Single declared-type parameter
    }

    @Test
    public void testLambdaSingleParameter2()
    {
        checkLambdaExpression("(x) -> x+1"); // Single inferred-type parameter
    }

    @Test
    public void testLambdaSingleParameter3()
    {
        checkLambdaExpression("x -> x+1"); // Parens optional for single inferred-type case
    }

    @Test
    public void testLambdaSingleParameter4()
    {
        checkLambdaExpression("t -> { t.start(); } "); // Single inferred-type parameter
    }

    @Test
    public void testLambdaSingleParameter5()
    {
        checkLambdaExpression("(final int x) -> x+1"); // Modified declared-type parameter
    }

    @Test
    public void testLambdaSingleParameter6()
    {
        checkLambdaExpression("(CustomClass x) -> x+1"); // Modified declared-type parameter
    }

    @Test
    public void testLambdaSingleParameter7()
    {
        checkLambdaExpression("(int... x) -> x+1"); // Modified declared-type parameter
    }

    @Test
    public void testLambdaVarParameter1()
    {
        checkLambdaExpression("(var x) -> x+1");
    }

    @Test
    public void testLambdaVarParameter2()
    {
        checkLambdaExpression("(var x, y) -> x+1"); // Modified declared-type parameter
    }

    @Test
    public void testLambdaVarParameter3()
    {
        checkLambdaExpression("(var x, var y) -> x+1"); // Modified declared-type parameter
    }

    @Test
    public void testLambdaVarParameter4()
    {
        checkLambdaExpression("(x, var y) -> x+1"); // Modified declared-type parameter
    }

    @Test
    public void testLambdaVarParameter5()
    {
        checkLambdaExpression("(x, var y, int... z) -> x+1"); // Modified declared-type parameter
    }

    @Test
    public void testLambdaMultipleParameters1()
    {
        checkLambdaExpression("(int x, float y) -> x+y"); // Multiple declared-type parameters
    }

    @Test
    public void testLambdaMultipleParameters2()
    {
        checkLambdaExpression("(x,y) -> x+y"); // Multiple inferred-type parameters
    }    

    @Test
    public void testMethodRef2()
    {
        checkLambdaExpression("SomeClass::someMethod");
    }

    @Test
    public void testMethodRef3()
    {
        checkLambdaExpression("somepkg.someotherpkg.SomeClass::someMethod");
    }

    @Test
    public void testMethodRef4()
    {
        checkLambdaExpression("SomeClass::new");
    }

    /** Test generic method call */
    @Test
    public void testGenericMethodCall()
    {
        // someMethod might be declared something like:
        //    public <T> void someMethod(T arg) { }
        StringReader sr = new StringReader(
                "this.<String>someMethod(\"hello\")"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseExpression();
    }

    @Test
    public void testPrimitiveCast()
    {
        StringReader sr = new StringReader(
                "(byte)(a + 1)"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseExpression();
    }

    @Test
    public void testSynchronizedModifier()
    {
        StringReader sr = new StringReader(
                "interface A {" +
                "synchronized int someMethod();" +
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();

        sr = new StringReader("synchronized { throw new Exception(); }");
        input = SourceInput.fromReader(sr, SourceType.Java);
        ip = new SourceParser(input);
        ip.parseStatement();

        sr = new StringReader("synchronized(getSomeValue()) { throw new Exception(); }");
        input = SourceInput.fromReader(sr, SourceType.Java);
        ip = new SourceParser(input);
        ip.parseStatement();
    }

    public void testVarargsMethod()
    {
        StringReader sr = new StringReader(
                "interface A {" +
                "synchronized int someMethod(int ... a);" +
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseTypeDef();
    }

    /**
     * Test for loop with double initializer
     */
    @Test
    public void testForLoop()
    {
        StringReader sr = new StringReader(
                "for (int i = 8, j; ; ) {" +
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test for loop where initializer has modifier(s)
     */
    @Test
    public void testForLoop2()
    {
        StringReader sr = new StringReader(
                "for (final int i : intArray) {" +
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test for loop where initializer variables are already declared
     */
    @Test
    public void testForLoop3()
    {
        // if i and j are already declared, this should still parse:
        StringReader sr = new StringReader(
                "for (i = 0, j = 8; i++; i < 10) {" +
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test for loop where loop var is an array (and brackets on LHS)
     */
    @Test
    public void testForLoop4()
    {
        StringReader sr = new StringReader(
                "for (int[][] lesser : multidimArray) {}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    /**
     * Test for loop where loop var is an array (and brackets on RHS)
     */
    @Test
    public void testForLoop5()
    {
        StringReader sr = new StringReader(
                "for (int lesser[][] : multidimArray) {}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }


    @Test
    public void testFunkyCast()
    {
        StringReader sr = new StringReader(
                "return (Insets)((ContainerPeer)peer).insets().clone();"
                );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    @Test
    public void testMethodParamModifier()
    {
        StringReader sr = new StringReader(
                "interface I {" +
                "void someMethod(final String argument);" +
                "}"
                );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    @Test
    public void testParenthesizedValue()
    {
        StringReader sr = new StringReader(
                "new int[] { 1, 2 + (someValue), 3 }"
                );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseExpression();
    }

    @Test
    public void testTopLevelExtraSemis()
    {
        StringReader sr = new StringReader(
                "import java.lang.*; ;" +
                "interface A {" +
                "};"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseCU();
    }

    @Test
    public void testParenthesizedInTrinary()
    {
        StringReader sr = new StringReader(
                "sb.append((isFilled) ? \"yes\": \"no\");"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseStatement();
    }

    @Test
    public void testDefaultMethodModifier()
    {
        StringReader sr = new StringReader(
                "interface A {\n" +
                "  default int someMethod() { return 3; }\n" +
                "}"
        );
        SourceInput input = SourceInput.fromReader(sr, SourceType.Java);
        SourceParser ip = new SourceParser(input);
        ip.parseCU();
    }

    @Test
    public void testConstructor1()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("Foo() { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testConstructor2()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("public Foo() { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testConstructor3()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("<T> Foo(T t) { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testConstructor4()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("public <T, U> Foo() { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testMethod1()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("void foo() { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testMethod2()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("<T, U> void foo() { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testMethod3()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("public <T, U> void foo() { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testMethod4()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("public <T, U> java.lang.String[] foo() { return; } }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testField1()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("int foo; }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testField2()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("int foo[]; }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testField3()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("int foo = 0; }"), SourceType.Java)).parseClassBody();
    }

    @Test
    public void testTopLevelRecord1()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            record Foo(int x) {}
            """
        ), SourceType.Java)).parseCU();
    }
    @Test
    public void testTopLevelRecord2()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            public record R(int x, String s, double t)
            {
                public void foo() {return 6;}
            }
            """
        ), SourceType.Java)).parseCU();
    }

    @Test
    public void testTopLevelRecord3()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            public record GenericR<T>(T a, T b)
            {
                public GenericR(T both)
                {
                    this(both, both);
                }
                public T foo() {return a;}
            }
            """
        ), SourceType.Java)).parseCU();
    }

    @Test
    public void testTopLevelRecord4()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            public record GenericR<T, U>(T a, U b)
            {
                public T foo() {return a;}
            }
            """
        ), SourceType.Java)).parseCU();
    }

    @Test
    public void testTopLevelRecord5()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            public record GenericR<T, U>(T a, U b) implements Cloneable
            {
                public T foo() {return a;}
            }
            """
        ), SourceType.Java)).parseCU();
    }

    @Test
    public void testTopLevelRecord6()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            public record GenericR<T, U>(T a, U... b) implements Cloneable
            {
                public T foo() {return a;}
                private record Point(int x, Double y) {}
            }
            """
        ), SourceType.Java)).parseCU();
    }

    @Test
    public void testTopLevelRecord7()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            record Foo() {}
            """
        ), SourceType.Java)).parseCU();
    }
    @Test
    public void testTopLevelRecord8()
    {
        new SourceParser(SourceInput.fromReader(new StringReader("""
            record Foo(int... is) {}
            """
        ), SourceType.Java)).parseCU();
    }

}
