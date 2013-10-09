package bluej.parser;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import bluej.parser.lexer.LocatableToken;

import junit.framework.TestCase;

public class NewParserTest extends TestCase
{
    /**
     * Test array as type parameter
     */
    public void test1()
    {
        StringReader sr = new StringReader(
                "LinkedList<String[]>"
        );
        InfoParser ip = new InfoParser(sr, null);
        List<LocatableToken> ll = new LinkedList<LocatableToken>();
        assertTrue(ip.parseTypeSpec(false, true, ll));
        // 6 tokens: LinkedList, '<', String, '[', ']', '>'
        assertEquals(6, ll.size());
    }

    /**
     * Test handling of '>>' sequence in type spec
     */
    public void test2()
    {
        StringReader sr = new StringReader(
                "LinkedList<List<String[]>>"
        );
        InfoParser ip = new InfoParser(sr, null);
        List<LocatableToken> ll = new LinkedList<LocatableToken>();
        assertTrue(ip.parseTypeSpec(false, true, ll));
        // 8 tokens: LinkedList, '<', List, '<', String, '[', ']', '>>'
        assertEquals(8, ll.size());
    }

    /**
     * Test multiple type parameters
     */
    public void test3()
    {
        StringReader sr = new StringReader(
                "Map<String,Integer> v1; "
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test generic inner class of generic outer class
     */
    public void test4()
    {
        StringReader sr = new StringReader(
                "Outer<String>.Inner<String> v8; "
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test wildcard type parameters
     */
    public void test5()
    {
        StringReader sr = new StringReader(
                "A<?> v8; " +
                "A<? extends String> v9; " +
                "A<? super String> v10;"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
        ip.parseStatement();
        ip.parseStatement();
    }

    /**
     * Test less-than operator.
     */
    public void test6()
    {
        StringReader sr = new StringReader(
                "b = (i < j);"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test a funky statement.
     */
    public void test7()
    {
        StringReader sr = new StringReader(
                "boolean.class.equals(T.class);"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test a class declaration with a single type parameter.
     */
    public void test8()
    {
        StringReader sr = new StringReader(
                "class A<T>{}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }

    /**
     * Test a class declaration containing a semi-colon
     */
    public void test9()
    {
        StringReader sr = new StringReader(
                "class A{;}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }

    /**
     * Test a simple enum
     */
    public void test10()
    {
        StringReader sr = new StringReader(
                "enum A {" +
                "    one, two, three;" +
                "    private int x;" +
                "}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }

    /**
     * Test array declarators after a variable name.
     */
    public void test11()
    {
        StringReader sr = new StringReader(
                "int a[] = {1, 2, 3};"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test array declarators after a method parameter name.
     */
    public void test12()
    {
        StringReader sr = new StringReader(
                "int a[], int[] b);"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseMethodParamsBody();
    }

    /** 
     * Test array declarators after a field name.
     */
    public void test13()
    {
        StringReader sr = new StringReader(
                "class A { int x[] = {1,2,3}, y = 5; }"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }
    
    /**
     * Test multiple field definition in one statement.
     */
    public void test13p2()
    {
        StringReader sr = new StringReader(
                "class A { private int x, y; }"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }

    /**
     * Test multiple variable declaration in a single statement.
     */
    public void test14()
    {
        StringReader sr = new StringReader(
                "int x[], y = 3, z, q;"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test annotation declaration
     */
    public void test15()
    {
        StringReader sr = new StringReader(
                "public @interface Copyright{  String value();}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }

    /**
     * Test use of marker annotation
     */
    public void test16()
    {
        StringReader sr = new StringReader(
                "@Preliminary public class TimeTravel { }"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }

    /**
     * Test the use of an annotation.
     */
    public void test17()
    {
        StringReader sr = new StringReader(
                "@Copyright(\"2002 Yoyodyne Propulsion Systems\")"+
                "public class NewParserTest { }"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }

    /**
     * Test the '?:' operator.
     */
    public void testQuestionOperator()
    {
        StringReader sr = new StringReader(
                "Object g = (x<y) ? null : null;"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test a static method call.
     */
    public void testStaticMethodCall()
    {
        StringReader sr = new StringReader(
                "AAA.bbb(1,2,3);"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test the declaration of an annotation.
     */
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
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }


    /**
     * Test the use of an annotation.
     */
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
        JavaParser ip = new JavaParser(sr);
        ip.parseClassBody();
    }

    /**
     * Test the use of an annotation for a method.
     */
    public void test20()
    {
        StringReader sr = new StringReader(
                "@Test public static void m1() { } }"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseClassBody();
    }

    /**
     * Test the use of a qualified annotation
     */
    public void test21()
    {
        StringReader sr = new StringReader(
                "@Test.RequestForEnhancement int req;"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }



    public void test22()
    {
        StringReader sr = new StringReader(
                "@Expression(\"execution(* com.mypackage.Target.*(..))\") "+
                "Pointcut pc1; "
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();

    }

    public void test23()
    {
        StringReader sr = new StringReader(
                "@Expression(\"execution(* com.mypackage.Target.*(..))\") "+
                "volatile Pointcut pc1; "
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
    public void test24()
    {
        StringReader sr = new StringReader(
                "(byte)++(bb)"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseExpression();
    }

    public void test25()
    {
        StringReader sr = new StringReader(
                "new String[]{\"hello\", \"goodbye\",}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseExpression();
    }
    
    /** Test generic method call */
    public void testGenericMethodCall()
    {
        // someMethod might be declared something like:
        //    public <T> void someMethod(T arg) { }
        StringReader sr = new StringReader(
                "this.<String>someMethod(\"hello\")"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseExpression();
    }
    
    public void testPrimitiveCast()
    {
        StringReader sr = new StringReader(
                "(byte)(a + 1)"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseExpression();
    }
    
    public void testSynchronizedModifier()
    {
        StringReader sr = new StringReader(
                "interface A {" +
                "synchronized int someMethod();" +
                "}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
        
        sr = new StringReader("synchronized { throw new Exception(); }");
        ip = new JavaParser(sr);
        ip.parseStatement();
        
        sr = new StringReader("synchronized(getSomeValue()) { throw new Exception(); }");
        ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
    public void testVarargsMethod()
    {
        StringReader sr = new StringReader(
                "interface A {" +
                "synchronized int someMethod(int ... a);" +
                "}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseTypeDef();
    }
    
    /**
     * Test for loop with double initializer
     */
    public void testForLoop()
    {
        StringReader sr = new StringReader(
                "for (int i = 8, j; ; ) {" +
                "}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
    /**
     * Test for loop where initializer has modifier(s)
     */
    public void testForLoop2()
    {
        StringReader sr = new StringReader(
                "for (final int i : intArray) {" +
                "}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
    /**
     * Test for loop where initializer variables are already declared
     */
    public void testForLoop3()
    {
        // if i and j are already declared, this should still parse:
        StringReader sr = new StringReader(
                "for (i = 0, j = 8; i++; i < 10) {" +
                "}"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
    public void testFunkyCast()
    {
        StringReader sr = new StringReader(
                "return (Insets)((ContainerPeer)peer).insets().clone();"
                );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
    public void testMethodParamModifier()
    {
        StringReader sr = new StringReader(
                "interface I {" +
                "void someMethod(final String argument);" +
                "}"
                );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
    public void testParenthesizedValue()
    {
        StringReader sr = new StringReader(
                "new int[] { 1, 2 + (someValue), 3 }"
                );
        JavaParser ip = new JavaParser(sr);
        ip.parseExpression();
    }
    
    public void testTopLevelExtraSemis()
    {
        StringReader sr = new StringReader(
                "import java.lang.*; ;" +
                "interface A {" +
                "};"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseCU();
    }
    
    public void testParenthesizedInTrinary()
    {
        StringReader sr = new StringReader(
                "sb.append((isFilled) ? \"yes\": \"no\");"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
        
    }
}
