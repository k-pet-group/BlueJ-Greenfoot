/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

/**
 * Test that void results are handled correctly by the textpad parser.
 * 
 * @author Davin McCall
 * @version $Id: TextParserTest.java 6164 2009-02-19 18:11:32Z polle $
 */
public class TextParserTest extends TestCase
{
    public void testVoidResult()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("System.out.println(\"no comment\");");
        assertNull(r);
    }
    
    public void testArithmeticPromotion()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("2+3");
        assertEquals("int", r);
        r = tp.parseCommand("2.0+3");
        assertEquals("double", r);
        r = tp.parseCommand("2.2+3.0f");
        assertEquals("double", r);
    }
    
    public void testCasting()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(String)s");
        assertEquals("java.lang.String", r);
    }

    public void testStaticMethodCall()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("javax.swing.BorderFactory.createEmptyBorder()");
        assertEquals("javax.swing.border.Border", r);
    }

    public void testStaticVariable()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("System.out");
        assertEquals("java.io.PrintStream", r);
        r = tp.parseCommand("java.lang.System.out");
        assertEquals("java.io.PrintStream", r);
    }
    
    public void testNewInnerClass()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("javax.swing.Box.new Filler()");
        assertEquals("javax.swing.Box.Filler", r);
    } 
    
    public void testCastToWildcard()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(java.util.LinkedList<?>) new java.util.LinkedList<Thread>()");
        assertEquals("java.util.LinkedList<?>", r);
    }
    
    public void testArrayDeclaration()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        tp.parseCommand("int [] ia = new int [] {1,2,3};");
        List declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        TextParser.DeclaredVar var = (TextParser.DeclaredVar) declaredVars.get(0);
        assertEquals("ia", var.getName());
        assertEquals("int[]", var.getDeclaredVarType().toString());
        
        // Test two-dimensional array
        tp.parseCommand("int [][] iaa = new int [5][6];");
        declaredVars = tp.getDeclaredVars();
        assertEquals(1, declaredVars.size());
        var = (TextParser.DeclaredVar) declaredVars.get(0);
        assertEquals("iaa", var.getName());
        assertEquals("int[][]", var.getDeclaredVarType().toString());
    }
}
