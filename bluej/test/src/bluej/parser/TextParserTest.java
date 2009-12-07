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
import bluej.parser.TextAnalyzer.DeclaredVar;

/**
 * Test that void results are handled correctly by the textpad parser.
 * 
 * @author Davin McCall
 * @version $Id: TextParserTest.java 6911 2009-12-07 07:12:25Z davmac $
 */
public class TextParserTest extends TestCase
{
    public void testVoidResult()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("System.out.println(\"no comment\");");
        assertNull(r);
    }
    
    public void testArithmeticPromotion()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
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
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(int)(2+5l) * 3");
        assertEquals("int", r);
    }
    
    public void testCasting()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(String)null");
        assertEquals("java.lang.String", r);
    }
    
    public void testCasting2()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(String[])null");
        assertEquals("java.lang.String[]", r);
    }
    
    public void testCasting3()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(java.util.LinkedList<? extends javax.swing.JComponent>[])null");
        assertEquals("java.util.LinkedList<? extends javax.swing.JComponent>[]", r);
    }
    
    public void testStaticMethodCall()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("javax.swing.BorderFactory.createEmptyBorder()");
        assertEquals("javax.swing.border.Border", r);
    }

    public void testStaticVariable()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("System.out");
        assertEquals("java.io.PrintStream", r);
        r = tp.parseCommand("java.lang.System.out");
        assertEquals("java.io.PrintStream", r);
    }

    public void testNewExpression()
    {
        // Classes in java.lang can be unqualified
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
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
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("new " + Inner.class.getCanonicalName() + "()");
        assertEquals(getClass().getName() + ".Inner", r);
    }
    
    public void testNewExpression3()
    {
        // Type arguments
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
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
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("javax.swing.Box.new Filler()");
        assertEquals("javax.swing.Box.Filler", r);
    } 
    
    public void testCastToWildcard()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(java.util.LinkedList<?>) new java.util.LinkedList<Thread>()");
        assertEquals("java.util.LinkedList<?>", r);
    }
    
    public void testArrayDeclaration()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        tp.parseCommand("int [] ia = new int [] {1,2,3};");
        List<DeclaredVar> declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        DeclaredVar var = declaredVars.get(0);
        assertEquals("ia", var.getName());
        assertEquals("int[]", var.getDeclaredVarType().toString());
        
        // Test two-dimensional array
        tp.parseCommand("int [][] iaa = new int [5][6];");
        declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        var = (TextAnalyzer.DeclaredVar) declaredVars.get(0);
        assertEquals("iaa", var.getName());
        assertEquals("int[][]", var.getDeclaredVarType().toString());
    }
    
    public void testAnonymousInnerClass()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("new Runnable() {" +
        		"public void run() {}" +
        		"}");
        assertEquals("java.lang.Runnable", r);
    }
    
    public void testClassLiteral()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("Object.class");
        assertEquals("java.lang.Class", r);
    }
    
    public void testImport()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("import java.util.LinkedList;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }
    
    public void testWcImport()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("import java.util.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("new LinkedList()");
        assertEquals("java.util.LinkedList", r);
    }

    public void testStaticImport()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("import static java.awt.Color.BLACK;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("BLACK");
        assertEquals("java.awt.Color", r);
    }
    
    public void testStaticWildcardImport()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("import static java.awt.Color.*;");
        assertNull(r);
        tp.confirmCommand();
        r = tp.parseCommand("BLACK");
        assertEquals("java.awt.Color", r);
    }
    
    public void testStringConcat()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("\"string\"");
        assertEquals("java.lang.String", r);
        
        r = tp.parseCommand("\"string\" + 4");
        assertEquals("java.lang.String", r);

        r = tp.parseCommand("\"string\" + 4.7 + 4");
        assertEquals("java.lang.String", r);

        r = tp.parseCommand("\"string\" + new Object()");
        assertEquals("java.lang.String", r);
    }
}
