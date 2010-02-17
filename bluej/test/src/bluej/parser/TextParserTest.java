/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import junit.framework.TestCase;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.texteval.DeclaredVar;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;

/**
 * Test that void results are handled correctly by the textpad parser.
 * 
 * @author Davin McCall
 * @version $Id: TextParserTest.java 7136 2010-02-17 03:15:57Z davmac $
 */
public class TextParserTest extends TestCase
{
    private EntityResolver resolver;
    private ObjectBench objectBench;
    
    @Override
    protected void setUp() throws Exception
    {
        objectBench = new ObjectBench();
        resolver = new ClassLoaderResolver(getClass().getClassLoader());
    }
    
    public void testVoidResult()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("System.out.println(\"no comment\");");
        assertNull(r);
    }
    
    public void testNull()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("null");
        assertEquals("null", r);
    }
    
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
    
    public void testParenthesizedExpression()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(int)(2+5l) * 3");
        assertEquals("int", r);
    }
    
    public void testCasting()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(String)null");
        assertEquals("java.lang.String", r);
    }
    
    public void testCasting2()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(String[])null");
        assertEquals("java.lang.String[]", r);
    }
    
    public void testCasting3()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(java.util.LinkedList<? extends javax.swing.JComponent>[])null");
        assertEquals("java.util.LinkedList<? extends javax.swing.JComponent>[]", r);
    }
    
    public void testStaticMethodCall()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("javax.swing.BorderFactory.createEmptyBorder()");
        assertEquals("javax.swing.border.Border", r);
        
        r = tp.parseCommand("Class.forName(\"java.lang.Object\")");
        assertEquals("java.lang.Class<?>", r);
    }

    public void testStaticVariable()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("System.out");
        assertEquals("java.io.PrintStream", r);
        r = tp.parseCommand("java.lang.System.out");
        assertEquals("java.io.PrintStream", r);
    }

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
    
    public void testNewExpression2()
    {
        // New inner class
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new " + Inner.class.getCanonicalName() + "()");
        assertEquals(getClass().getName() + ".Inner", r);
    }
    
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
    
    public void testNewInnerClass()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new javax.swing.Box.Filler(null, null, null)");
        assertEquals("javax.swing.Box.Filler", r);
    } 
    
    public void testCastToWildcard()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("(java.util.LinkedList<?>) new java.util.LinkedList<Thread>()");
        assertEquals("java.util.LinkedList<?>", r);
    }
    
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
    }
    
    public void testAnonymousInnerClass()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new Runnable() {" +
        		"public void run() {}" +
        		"}");
        assertEquals("java.lang.Runnable", r);
    }
    
    public void testClassLiteral()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("Object.class");
        assertEquals("java.lang.Class<java.lang.Object>", r);
        
        r = tp.parseCommand("int.class");
        assertEquals("java.lang.Class<java.lang.Integer>", r);
    }
    
    public void testClassLiteral2()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("Object[].class");
        assertEquals("java.lang.Class<java.lang.Object[]>", r);
        
        r = tp.parseCommand("int[][].class");
        assertEquals("java.lang.Class<int[][]>", r);
    }
    
    public void testImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import java.util.LinkedList;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }
    
    public void testWcImport()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("import java.util.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }

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
    }
    
    public void testUnboxing()
    {
        TextAnalyzer tp = new TextAnalyzer(resolver, "", objectBench);
        String r = tp.parseCommand("new Integer(4) * 5");
        assertEquals("int", r);
        
        // + is especially confusing because it's overloaded for Strings:
        r = tp.parseCommand("new Integer(4) + 5");
        assertEquals("int", r);
    }
    
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
    }
    
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
        assertNull(r);
        
        r = tp.parseCommand("3 < null");
        assertNull(r);
        
        r = tp.parseCommand("3.0f < (Integer)null");
        assertEquals("boolean", r);
    }
    
    //TODO test instanceof
    
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
}
