/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2013,2014,2015,2016,2019 Michael Kolling and John Rosenberg
 
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

import java.util.List;

import bluej.JavaFXThreadingRule;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugmgr.codepad.DeclaredVar;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.ValueEntity;
import bluej.parser.nodes.ParsedCUNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test that void results are handled correctly by the textpad parser.
 * 
 * @author Davin McCall
 */
public class TextParserTest
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    @BeforeClass
    public static void initConfig()
    {
        InitConfig.init();
    }

    private TestEntityResolver resolver;
    private TestObjectBench objectBench;
    
    @Before
    public void setUp() throws Exception
    {
        objectBench = new TestObjectBench();
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private ParsedCUNode cuForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        TestableDocument document = new TestableDocument(resolver);
        document.enableParser(true);
        document.insertString(0, sourceCode);
        return document.getParser();
    }
    
    @Test
    public void testVoidResult()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("System.out.println(\"no comment\");");
        assertNull(r);
    }
    
    @Test
    public void testNull()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("null");
        assertEquals("null", r);
    }
    
    @Test
    public void testArithmeticPromotion()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("2+3");
        assertEquals("int", r);
        r = tp.parseCommand("2.0+3");
        assertEquals("double", r);
        r = tp.parseCommand("2.2+3.0f");
        assertEquals("double", r);
        r = tp.parseCommand("'a'+'b'");
        assertEquals("int", r);
        r = tp.parseCommand("4+5l");
        assertEquals("long", r);
    }
    
    @Test
    public void testParenthesizedExpression()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(int)(2+5l) * 3");
        assertEquals("int", r);
    }
    
    @Test
    public void testCasting()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(String)null");
        assertEquals("java.lang.String", r);
    }
    
    @Test
    public void testCasting2()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(String[])null");
        assertEquals("java.lang.String[]", r);
    }
    
    @Test
    public void testCasting3()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(java.util.LinkedList<? extends javax.swing.JComponent>[])null");
        assertEquals("java.util.LinkedList<? extends javax.swing.JComponent>[]", r);
    }
    
    /**
     * Test casting a numeric value to a numeric primitive type.
     */
    @Test
    public void testCasting4()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(char) 4");
        assertEquals("char", r);
        r = tp.parseCommand("(byte) 4");
        assertEquals("byte", r);        
        r = tp.parseCommand("(short) 4");
        assertEquals("short", r);
        r = tp.parseCommand("(int) 4");
        assertEquals("int", r);
        r = tp.parseCommand("(long) 4");
        assertEquals("long", r);
        r = tp.parseCommand("(float) 4");
        assertEquals("float", r);
        r = tp.parseCommand("(double) 4");
        assertEquals("double", r);
    }
    
    /**
     * Test casting of negative numeric values.
     */
    @Test
    public void testCasting5()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(char) -4");
        assertEquals("char", r);
        r = tp.parseCommand("(byte) -4");
        assertEquals("byte", r);        
        r = tp.parseCommand("(short) -4");
        assertEquals("short", r);
        r = tp.parseCommand("(int) -4");
        assertEquals("int", r);
        r = tp.parseCommand("(long) -4");
        assertEquals("long", r);
        r = tp.parseCommand("(float) -4");
        assertEquals("float", r);
        r = tp.parseCommand("(double) -4");
        assertEquals("double", r);
    }
    
    @Test
    public void testStaticMethodCall()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("javax.swing.BorderFactory.createEmptyBorder()");
        assertEquals("javax.swing.border.Border", r);
        
        r = tp.parseCommand("Class.forName(\"java.lang.Object\")");
        assertEquals("java.lang.Class<?>", r);
        
        // Now try dynamically
        String aSrc = "class A {\n" +
            "  static int nn() { return 0; }\n" +
            "}\n";
        ParsedCUNode aNode = cuForSource(aSrc, "");
        resolver.addCompilationUnit("", aNode);
        r = tp.parseCommand("A.nn()");
        assertEquals("int", r);
    }

    @Test
    public void testStaticVariable()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("System.out");
        assertEquals("java.io.PrintStream", r);
        r = tp.parseCommand("java.lang.System.out");
        assertEquals("java.io.PrintStream", r);
        
        String aSrc = "class A {\n" +
            "  static int nn = 1;\n" +
            "}\n";
        ParsedCUNode aNode = cuForSource(aSrc, "");
        resolver.addCompilationUnit("", aNode);
        r = tp.parseCommand("A.nn");
        assertEquals("int", r);
    }

    @Test
    public void testNewExpression()
    {
        // Classes in java.lang can be unqualified
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new String()");
        assertEquals("java.lang.String", r);
        
        // Fully qualified classes
        r = tp.parseCommand("new java.util.LinkedList()");
        assertEquals("java.util.LinkedList", r);

        r = tp.parseCommand("new java.util.ArrayList(5)");
        assertEquals("java.util.ArrayList", r);
    }
    
    /** Used by the next test */
    public static class Inner<T>
    {
        public class Further<U>
        {
            U u;
        }
    }
    
    @Test
    public void testNewExpression2()
    {
        // New inner class
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new " + Inner.class.getCanonicalName() + "()");
        assertEquals(getClass().getName() + ".Inner", r);
    }
    
    @Test
    public void testNewExpression3()
    {
        // Type arguments
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new " + getClass().getName() + ".Inner<String>()");
        assertEquals(getClass().getName() + ".Inner<java.lang.String>", r);
        
        // Array
        r = tp.parseCommand("new int[10]");
        assertEquals("int[]", r);
        
        r = tp.parseCommand("new java.util.HashMap<String, String>[10]");
        assertEquals("java.util.HashMap<java.lang.String,java.lang.String>[]", r);
    }
    
    @Test
    public void testNewInnerClass()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new javax.swing.Box.Filler(null, null, null)");
        assertEquals("javax.swing.Box.Filler", r);
    } 
    
    @Test
    public void testCastToWildcard()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(java.util.LinkedList<?>) new java.util.LinkedList<Thread>()");
        assertEquals("java.util.LinkedList<?>", r);
    }
    
    @Test
    public void testArrayDeclaration()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        tp.parseCommand("int [] ia = new int [] {1,2,3};");
        List<DeclaredVar> declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        DeclaredVar var = declaredVars.get(0);
        assertEquals("ia", var.getName());
        assertEquals("int[]", var.getDeclaredType().toString());
        
        // Test two-dimensional array
        tp.parseCommand("int [][] iaa = new int [5][6];");
        declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        var = declaredVars.get(0);
        assertEquals("iaa", var.getName());
        assertEquals("int[][]", var.getDeclaredType().toString());
        
        // Simple reference-type array with no initializer
        tp.parseCommand("String [] a;");
        List<DeclaredVar> vars = tp.getDeclaredVars();
        assertEquals(1, vars.size());
        assertEquals("java.lang.String[]", vars.get(0).getDeclaredType().toString());
        
        // Array with array declarators after the name instead of before it
        tp.parseCommand("String a[];");
        vars = tp.getDeclaredVars();
        assertEquals(1, vars.size());
        assertEquals("java.lang.String[]", vars.get(0).getDeclaredType().toString());
        
        // Multiple declaration
        tp.parseCommand("int a, b[], c, d[][], e[];");
        vars = tp.getDeclaredVars();
        assertEquals(5, vars.size());
        assertEquals("int", vars.get(0).getDeclaredType().toString());
        assertEquals("int[]", vars.get(1).getDeclaredType().toString());
        assertEquals("int", vars.get(2).getDeclaredType().toString());
        assertEquals("int[][]", vars.get(3).getDeclaredType().toString());
        assertEquals("int[]", vars.get(4).getDeclaredType().toString());
        
        // Multiple declarations 2
        tp.parseCommand("int [] a, b[], c, d[][];");
        vars = tp.getDeclaredVars();
        assertEquals(4, vars.size());
        assertEquals("int[]", vars.get(0).getDeclaredType().toString());
        assertEquals("int[][]", vars.get(1).getDeclaredType().toString());
        assertEquals("int[]", vars.get(2).getDeclaredType().toString());
        assertEquals("int[][][]", vars.get(3).getDeclaredType().toString());
    }
    
    @Test
    public void testAnonymousInnerClass()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new Runnable() {" +
                "public void run() {}" +
                "}");
        assertEquals("java.lang.Runnable", r);
    }
    
    @Test
    public void testClassLiteral()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("Object.class");
        assertEquals("java.lang.Class<java.lang.Object>", r);
        
        r = tp.parseCommand("int.class");
        assertEquals("java.lang.Class<java.lang.Integer>", r);
    }
    
    @Test
    public void testClassLiteral2()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("Object[].class");
        assertEquals("java.lang.Class<java.lang.Object[]>", r);
        
        r = tp.parseCommand("int[][].class");
        assertEquals("java.lang.Class<int[][]>", r);
    }
    
    @Test
    public void testImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import java.util.LinkedList;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }
    
    @Test
    public void testWcImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import java.util.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }

    @Test
    public void testStaticImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import static java.awt.Color.BLACK;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("BLACK");
        assertEquals("java.awt.Color", r);
        
        // Try one that's not the same type as its enclosing class:
        r = tp.parseCommand("import static Math.PI;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("PI");
        assertEquals("double", r);
    }
    
    @Test
    public void testStaticWildcardImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import static java.awt.Color.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("BLACK");
        assertEquals("java.awt.Color", r);
        
        // Try one that's not the same type as its enclosing class:
        r = tp.parseCommand("import static Math.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("PI");
        assertEquals("double", r);
    }
    
    @Test
    public void testStringConcat()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("\"string\"");
        assertEquals("java.lang.String", r);
        
        r = tp.parseCommand("\"string\" + 4");
        assertEquals("java.lang.String", r);

        r = tp.parseCommand("\"string\" + 4.7 + 4");
        assertEquals("java.lang.String", r);

        r = tp.parseCommand("\"string\" + new Object()");
        assertEquals("java.lang.String", r);
        
        r = tp.parseCommand("4 + \"a string\"");
        assertEquals("java.lang.String", r);
        
        r = tp.parseCommand("new int[3] + \" a string!\"");
        assertEquals("java.lang.String", r);
    }
    
    @Test
    public void testUnboxing()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new Integer(4) * 5");
        assertEquals("int", r);
        
        // + is especially confusing because it's overloaded for Strings:
        r = tp.parseCommand("new Integer(4) + 5");
        assertEquals("int", r);
    }
    
    @Test
    public void testOperators()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("3 << 2");
        assertEquals("int", r);
        r = tp.parseCommand("3 << 2l");
        assertEquals("int", r);
        
        r = tp.parseCommand("3 >> 2l");
        assertEquals("int", r);
        
        r = tp.parseCommand("3 >>> 2");
        assertEquals("int", r);
        
        r = tp.parseCommand("3 == 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 != 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 < 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 <= 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 > 4");
        assertEquals("boolean", r);
        r = tp.parseCommand("3 >= 4");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("! true");
        assertEquals("boolean", r);
        r = tp.parseCommand("true || false");
        assertEquals("boolean", r);
        r = tp.parseCommand("true && false");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("~4");
        assertEquals("int", r);
        r = tp.parseCommand("~4l");
        assertEquals("long", r);
        r = tp.parseCommand("4l & 5");
        assertEquals("long", r);
        r = tp.parseCommand("4l | 5");
        assertEquals("long", r);
        r = tp.parseCommand("4l ^ 5"); // xor
        assertEquals("long", r);
    }
    
    @Test
    public void testOperators2()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        
        String r = tp.parseCommand("true ? 3 : 4");
        assertEquals("int", r);
        r = tp.parseCommand("true ? \"a string\" : \"b string\"");
        assertEquals("java.lang.String", r);
        r = tp.parseCommand("true ? \"a string\" : 4");
        // The result in this case is really:
        //   java.lang.Object & java.io.Serializable & java.lang.Comparable<? extends [recursive]>
        boolean correct = r.equals("java.lang.Object");
        correct |= r.equals("java.io.Serializable");
        correct |= r.equals("java.lang.Comparable<? extends java.lang.Comparable<?>>");
        assertTrue(correct);
        
        // If one side is a byte and the other is a constant which could be narrowed to
        // a byte, then the result type should be byte:
        r = tp.parseCommand("true ? (byte) 3 : 4");
        assertEquals("byte", r);
    }
    
    @Test
    public void testOperators3()
    {
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public static long a = 4;\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("Lala.a++");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a--");        
        assertEquals("long", r);
        r = tp.parseCommand("++Lala.a");
        assertEquals("long", r);
        r = tp.parseCommand("--Lala.a");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a += 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a -= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a *= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a /= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a %= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a &= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a |= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a ^= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a <<= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a >>= 1");
        assertEquals("long", r);
        r = tp.parseCommand("Lala.a >>>= 1");
        assertEquals("long", r);
    }
    
    @Test
    public void testOperators4()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        
        String r = tp.parseCommand("true | false");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("true & false");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("true ^ false");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("!true");
        assertEquals("boolean", r);
    }
    
    /**
     * Check that an entity represents a constant with a particular integer (byte,short,char,int,long) value
     */
    private void checkConstInt(JavaEntity ent, long val)
    {
        assertNotNull(ent);
        ValueEntity vent = ent.resolveAsValue();
        assertNotNull(vent);
        assertTrue(vent.hasConstantIntValue());
        assertEquals(val, vent.getConstantIntValue());
    }
    
    /**
     * Check that an entity represents a constant with a particular boolean value
     */
    private void checkConstBool(JavaEntity ent, boolean val)
    {
        assertNotNull(ent);
        ValueEntity vent = ent.resolveAsValue();
        assertNotNull(vent);
        assertTrue(vent.hasConstantBooleanValue());
        assertEquals(val, vent.getConstantBooleanValue());
    }
    
    @Test
    public void testConstantExpressions()
    {
        // From JLS 15.28
        
        // literals
        TextParser parser = new TextParser(resolver, "3", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        JavaEntity exprType = parser.getExpressionType();
        checkConstInt(exprType, 3);
        
        // casts to primitive types
        parser = new TextParser(resolver, "(byte)3", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 3);
        
        // Various operators
        parser = new TextParser(resolver, "3+5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 8);
        
        parser = new TextParser(resolver, "3-5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, -2);
        
        parser = new TextParser(resolver, "3*5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 15);

        parser = new TextParser(resolver, "3/5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 0);

        parser = new TextParser(resolver, "3%5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 3);
        
        parser = new TextParser(resolver, "3<<2", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 12);
        
        parser = new TextParser(resolver, "3>>1", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 1);

        parser = new TextParser(resolver, "3>>>1", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 1);
        
        parser = new TextParser(resolver, "3<5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, true);

        parser = new TextParser(resolver, "3>5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, false);

        parser = new TextParser(resolver, "3>=5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, false);

        parser = new TextParser(resolver, "3<=5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, true);
        
        parser = new TextParser(resolver, "-3", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, -3);

        parser = new TextParser(resolver, "~3", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, -4);
        
        parser = new TextParser(resolver, "+3", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 3);
        
        parser = new TextParser(resolver, "!true", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, false);
                
        parser = new TextParser(resolver, "3 != 5", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, true);
    }
    
    @Test
    public void testConstantExpressions2()
    {
        TextParser parser = new TextParser(resolver, "3l", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        JavaEntity exprType = parser.getExpressionType();
        checkConstInt(exprType, 3);
        
        parser = new TextParser(resolver, "(int)4f", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 4);
        
        parser = new TextParser(resolver, "(int)5.0", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 5);
        
        parser = new TextParser(resolver, "\'a\'", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, 'a');
    }
    
    @Test
    public void testConstantExpressions3()
    {
        // Division by 0 yields a 'non-constant' int
        TextParser parser = new TextParser(resolver, "4 / 0", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        JavaEntity exprType = parser.getExpressionType();
        assertNotNull(exprType);
        assertEquals("int", exprType.getType().toString());
        
        // Division by floating-point 0 yields a constant infinity
        parser = new TextParser(resolver, "4.0 / 0.0", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        assertNotNull(exprType);
        assertEquals("double", exprType.getType().toString());
        
        parser = new TextParser(resolver, "(int)(4.0 / 0.0)", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, Integer.MAX_VALUE);

        parser = new TextParser(resolver, "4.0f / 0.0f", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        assertNotNull(exprType);
        assertEquals("float", exprType.getType().toString());
        
        parser = new TextParser(resolver, "(int)(4.0f / 0.0f)", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, Integer.MAX_VALUE);

        parser = new TextParser(resolver, "(short)(4.0f / 0.0f)", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, -1);

        parser = new TextParser(resolver, "(char)(4.0f / 0.0f)", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, Character.MAX_VALUE);
        
        parser = new TextParser(resolver, "(byte)(4.0f / 0.0f)", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstInt(exprType, -1);
                
        // Modulo implies division
        parser = new TextParser(resolver, "4 % 0", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        assertNotNull(exprType);
        assertEquals("int", exprType.getType().toString());
    }
    
    @Test
    public void testConstantStrings()
    {
        // From JLS 15.28
        TextParser parser = new TextParser(resolver, "\"hello\" == \"hello\"", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        JavaEntity exprType = parser.getExpressionType();
        checkConstBool(exprType, true);

        // casts to String do not remove constness
        parser = new TextParser(resolver, "\"hello\" == (java.lang.String)\"hello\"", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, true);

        parser = new TextParser(resolver, "\"hello\" == \"goodbye\"", null, true);
        parser.parseExpression();
        assertTrue(parser.atEnd());
        exprType = parser.getExpressionType();
        checkConstBool(exprType, false);
    }
    
    @Test
    public void testUnboxingNumericComparison()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("3 == new Integer(6)");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(6) != 3");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) != 0.0f");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) < 0.0f");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) < new Double(8)");
        assertEquals("boolean", r);
        
        r = tp.parseCommand("new Integer(7) < null");
        assertEquals("", r); // expression form, but invalid
        
        r = tp.parseCommand("3 < null");
        assertEquals("", r); // expression form, but invalid
        
        r = tp.parseCommand("3.0f < (Integer)null");
        assertEquals("boolean", r);
    }
        
    @Test
    public void testEqualityReferenceOperators()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("null == null");
        assertEquals("boolean", r);
        r = tp.parseCommand("new Integer(4) == null");
        assertEquals("boolean", r);
        // Perform an object reference check:
        r = tp.parseCommand("new Integer(4) != new Integer(5)");
        assertEquals("boolean", r);
        // This shouldn't convert to numeric types, at least one must be numeric,
        // and they don't inherit from each other so it's invalid:
        //r = tp.parseCommand("new Integer(4) != new Double(6)");
        //assertNull(r);

        // These should work because the numeric types inherit from Object:
        r = tp.parseCommand("new Object() != new Double(6)");
        assertEquals("boolean", r);
        r = tp.parseCommand("new Integer(5) == new Object()");
        assertEquals("boolean", r);
    }
    
    @Test
    public void testLiterals()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("0xFFFF");
        assertEquals("int", r);
        
        r = tp.parseCommand("0xFFFFFFFFFl");
        assertEquals("long", r);
        
        r = tp.parseCommand("1234");
        assertEquals("int", r);
        
        r = tp.parseCommand("12345L");
        assertEquals("long", r);
        
        // Obscure hexadecimal floating point - value after P is exponent (in decimal)
        r = tp.parseCommand("0xabcP2");
        assertEquals("double", r);
        
        r = tp.parseCommand("0xabcP2f");
        assertEquals("float", r);
    }
    
    //test behaviour of parsing of statements and expressions
    //please refer to #Bug 213
    @Test
    public void testObjectBench()
    {
        String lalaSrc = "package xyz; public class Lala { " +
                "public String toString() { return \"haha\"; }" +
                "public String foo() { return \"mama\"; } }";
        
        String nanaSrc="package xyz;\n"
            + "public class Nana extends Lala\n"
            + "{\n"
            + "  public int bar() {\n"
            + "    return 99;\n"
            + "  }\n"
            + "}\n";

        ParsedCUNode lalaNode = cuForSource(lalaSrc, "xyz");
        resolver.addCompilationUnit("xyz", lalaNode);
       
        ParsedCUNode nanaNode = cuForSource(nanaSrc, "xyz");
        resolver.addCompilationUnit("xyz", nanaNode);

        EntityResolver res = new PackageResolver(this.resolver, "xyz");
                
        TextAnalyzer tp = new TextAnalyzer(res, "xyz", objectBench);
        
        String r = tp.parseCommand("46");        
        assertEquals("int", r);
        tp.confirmCommand();
        
        r = tp.parseCommand("new Lala()");        
        assertEquals("xyz.Lala", r);
        tp.confirmCommand();
            
        r = tp.parseCommand("(new Lala()).toString()");
        assertEquals("java.lang.String", r);
        tp.confirmCommand();
       
        r = tp.parseCommand("(new Lala()).foo()");
        assertEquals("java.lang.String", r);
        tp.confirmCommand();
        
        r = tp.parseCommand("(new Nana()).foo()");
        assertEquals("java.lang.String", r);
        tp.confirmCommand();

        r = tp.parseCommand("(new Nana()).bar()");
        assertEquals("int", r);
        tp.confirmCommand();     
    }
    
    @Test
    public void testMethodResolution()
    {
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public int method(Runnable r) { return 0; }\n"
            + "  public float method(Thread r) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((Runnable) null)");
        assertEquals("int", r);
        r = tp.parseCommand("new Lala().method(new Thread())");        
        assertEquals("float", r);
    }
    
    @Test
    public void testMethodResolution2()
    {
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public int method(Runnable r) { return 0; }\n"
            + "  private float method(Thread r) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((Runnable) null)");
        assertEquals("int", r);
        r = tp.parseCommand("new Lala().method(new Thread())");        
        assertEquals("int", r);
    }
    
    @Test
    public void testMethodResolution3()
    {
        // widening primitive conversion
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public float method(int b) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((byte)3)");
        assertEquals("float", r);
    }

    @Test
    public void testMethodResolution4()
    {
        // boxing conversion followed by widening reference conversion
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public String method(Integer i) { return null; }\n"
            + "  public float method(Object o) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method((byte)3)");
        assertEquals("float", r);
    }
    
    @Test
    public void testMethodResolution5()
    {
        // method with type parameters, return type is dependent on parameter type
        String lalaSrc = ""
            + "public class Lala\n"
            + "{\n"
            + "  public <T> T method(T i) { return null; }\n"
            // + "  public float method(Object o) { return 0f; }\n"
            + "}\n";
        
        ParsedCUNode lalaNode = cuForSource(lalaSrc, "");
        resolver.addCompilationUnit("", lalaNode);

        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        String r = tp.parseCommand("new Lala().method(\"a string\")");
        assertEquals("java.lang.String", r);
        
        r = tp.parseCommand("new Lala().<Thread>method(null)");
        assertEquals("java.lang.Thread", r);
    }
    
    @Test
    public void testMethodResolution6()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        
        String r = tp.parseCommand("java.util.Arrays.asList(\"one\", \"two\", \"three\")");
        assertEquals("java.util.List<java.lang.String>", r);
    }
    
    @Test
    public void testInstanceof()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        
        String r = tp.parseCommand("(new Object()) instanceof String");
        assertEquals("boolean", r);
    }
    
    @Test
    public void testArrayLength()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        
        String r = tp.parseCommand("new String[5].length");
        assertEquals("int", r);
    }
    
    @Test
    public void testParenthesizedVar()
    {
        // Test for ticket #413
        TestValueCollection coll = new TestValueCollection();
        coll.addVariable("boolVal", JavaPrimitiveType.getBoolean(), true, false);
        TextAnalyzer tp = new TextAnalyzer(resolver, "", coll);
        
        String r = tp.parseCommand("(boolVal) && true");
        assertEquals("boolean", r);
        r = tp.parseCommand("(boolVal) || true");
        assertEquals("boolean", r);
    }
    
    @Test
    public void testEagerReturnTypeResolutionA1() throws Exception
    {
        String aClassSrc = "class Test1<T> {\n" +
          "  <S> S foo(S x, S y) { return x; }\n" +
          "  <S extends Number & Comparable<? extends Number>> S baz(Test1<S> a) { return null; }\n" +
          "  void bar(Test1<Long> x, Test1<Integer> y) {\n" +
          "    baz(foo(x, y));\n" +
          "  }\n" +
          "}\n";
        
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
            
        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        tp.parseCommand("Test1<String> t = new Test1<String>();");
        List<DeclaredVar> declVars = tp.getDeclaredVars();
        assertEquals(1, declVars.size());
        tp.confirmCommand();
        
        objectBench.addDeclaredVars(declVars);

        String rr = tp.parseCommand("t");
        assertEquals("Test1<java.lang.String>", rr);
        
        String r = tp.parseCommand("t.foo(new Test1<Long>(), new Test1<Integer>())");
        assertEquals("Test1<? extends java.lang.Number>", r);
    }

    @Test
    public void testEagerReturnTypeResolutionA4() throws Exception
    {
        String aClassSrc = "abstract class Test4 {\n" +
                "        abstract class A0<T> {}\n" +
                "        class A1<T> extends A0<T> {}\n" +
                "        class A2<T> extends A0<T> {}\n" +
                "        abstract <S> S foo(S x, S y);\n" +
                "        abstract <S1> S1 baz(A0<S1> a);\n" +
                "        void bar(A2<Integer> y, A1<Long> x){\n" +
                "             baz(foo(x, y));\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
         
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
            
        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        tp.parseCommand("Test4 t = new Test4();");
        List<DeclaredVar> declVars = tp.getDeclaredVars();
        assertEquals(1, declVars.size());
        tp.confirmCommand();
        
        objectBench.addDeclaredVars(declVars);
        
        tp.parseCommand("Test4.A2<Integer> a2 = t.new A2<Integer>();");
        declVars = tp.getDeclaredVars();
        assertEquals(1, declVars.size());
        tp.confirmCommand();
        objectBench.addDeclaredVars(declVars);

        tp.parseCommand("Test4.A1<Long> a1 = t.new A1<Long>();");
        declVars = tp.getDeclaredVars();
        assertEquals(1, declVars.size());
        tp.confirmCommand();
        objectBench.addDeclaredVars(declVars);
        
        String rr = tp.parseCommand("t");
        assertEquals("Test4", rr);
        
        String r = tp.parseCommand("t.baz(t.foo(a1, a2))");
        assertEquals("java.lang.Number", r);
    }
    
    private void addClass(String src)
    {
        resolver.addCompilationUnit("", cuForSource(src, ""));
    }
    
    @Test
    @Ignore("currently fails")
    public void testEagerReturnTypeResolutionBsimple() throws Exception
    {
        addClass("interface List<S> {}");
        addClass("interface I<S> {}");
        addClass("interface J<S> extends I<S> {}");
        addClass("interface K extends I<String> {}");
        addClass("interface L<S> extends I {}");
        
        String aClassSrc = "abstract class Test1 {\n" +
                "    <T> T lower(List<? extends T> l) { return null; }\n" +
                "    <T> T lower2(List<? extends T> l1, List<? extends T> l2) { return null; }\n" +
                "    <T> T upper(List<? super T> l) { return null; }\n" +
                "    <T> T upper2(List<? super T> l1, List<? super T> l2) { return null; }\n" +
                "    <T> T eq(List<T> l) { return null; }\n" +
                "    <T> T eq2(List<T> l1, List<T> l2) { return null; }\n" +
                "    <X> X takeI(I<X> i) {}\n" +
                "    K takeIString(I<String> i) {}\n" +
                "}\n";
         
        ParsedCUNode aNode = cuForSource(aClassSrc, "");
        resolver.addCompilationUnit("", aNode);
            
        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);
        
        tp.parseCommand("List<I<?>> i1 = null;");
        List<DeclaredVar> declVars = tp.getDeclaredVars();
        assertEquals(1, declVars.size());
        tp.confirmCommand();
        objectBench.addDeclaredVars(declVars);
        
        tp.parseCommand("Test1 t = new Test1();");
        declVars = tp.getDeclaredVars();
        assertEquals(1, declVars.size());
        tp.confirmCommand();
        objectBench.addDeclaredVars(declVars);

        String rr = tp.parseCommand("t.takeI(t.lower(i1))");
        assertEquals("java.lang.Object", rr);
        
        rr = tp.parseCommand("t.takeI(t.eq(i1))");
        assertEquals("java.lang.Object", rr);
        
        rr = tp.parseCommand("t.takeI(t.upper(i1))");
        assertEquals("java.lang.Object", rr);

        // Java 8. Not allowed in Java 7:
        rr = tp.parseCommand("t.takeIString(t.upper(i1))");
        assertEquals("K", rr);
    }
    
    @Test
    public void test560()
    {
        // Requires Java 8.
        EntityResolver res = new PackageResolver(this.resolver, "");
        TextAnalyzer tp = new TextAnalyzer(res, "", objectBench);

        String rr = tp.parseCommand("java.util.Optional.ofNullable(null).isPresent()");
        assertEquals("boolean", rr);
    }
    
}
