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
 * @version $Id: TextParserTest.java 6664 2009-09-11 08:13:43Z davmac $
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
    
    public void testCasting()
    {
        ObjectBench ob = new ObjectBench();
        TextAnalyzer tp = new TextAnalyzer(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(String)s");
        assertEquals("java.lang.String", r);
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
    }
    
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
        String r = tp.parseCommand("new " + getClass().getName() + ".Inner()");
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
}
