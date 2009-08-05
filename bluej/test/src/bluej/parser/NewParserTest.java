package bluej.parser;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

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
		InfoParser ip = new InfoParser(sr);
		List ll = new LinkedList();
		assertTrue(ip.parseTypeSpec(false, ll));
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
		InfoParser ip = new InfoParser(sr);
		List ll = new LinkedList();
		assertTrue(ip.parseTypeSpec(false, ll));
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
					"String engineer();  default \"[unassigned]\"; "+
					"String date();      default \"[unimplemented]\"; "+
				"}"
		);
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
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
		InfoParser ip = new InfoParser(sr);
		ip.parseClassBody();
	}
	
	/**
	 * Test the use of a package annotation
	 */
	public void test21()
	{
		StringReader sr = new StringReader(
				"@Test.RequestForEnhancement req;"
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseStatement();
	}
	
	

	public void test22(){
	    StringReader sr = new StringReader(
	            "@Expression(\"execution(* com.mypackage.Target.*(..))\") "+
	            "Pointcut pc1; "
	    );
	    InfoParser ip = new InfoParser(sr);
        ip.parseStatement();

	}
}
