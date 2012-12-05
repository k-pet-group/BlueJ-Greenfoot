/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012  Michael Kolling and John Rosenberg 
 
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

package bluej.collect;

import junit.framework.TestCase;
import bluej.utility.Utility;

public class TestAnonymisation extends TestCase
{
    private static String combineLines(String[] lines)
    {
        StringBuilder s = new StringBuilder();
        for (String line : lines)
        {
            s.append(line).append("\n");
        }
        return s.toString();
    }
    
    private static void assertAutoAnon(String[] input)
    {
        assertAnon(input, input);
    }
    
    private static void assertAnon(String[] input, String[] expectedOutput)
    {
        String[] actualOutput = Utility.splitLines(CodeAnonymiser.anonymise(combineLines(input)));
        
        assertEquals(expectedOutput.length, actualOutput.length);
        for (int i = 0; i < expectedOutput.length; i++)
        {
            assertEquals("Line " + i + " differs", expectedOutput[i], actualOutput[i]);
        }
    }
    
    public void test1()
    {
        assertAutoAnon(new String[] {
"class Foo",
"{",
"}"});
        
        assertAutoAnon(new String[] {
"    class Foo",
"{",
"}"});
    }
    
    public void test2()
    {
        assertAutoAnon(new String[] {
"  import bar;",
"",
"class Foo",
"{",
"}"});
    }
    
    public void test3()
    {
        assertAnon(new String[] {
"  import bar;",
"/** Some comment",
"*/",
"class Foo",
"{",
"}"},

new String[] {
"  import bar;",
"/** #### #######",
"*/",
"class Foo",
"{",
"}"});
    }
    
    public void test3B()
    {
        assertAnon(new String[] {
"import bar;",
"/** Some comment == something + blah - 6",
"*/",
"class Foo",
"{",
"}"},

new String[] {
"import bar;",
"/** #### ####### == ######### + #### - #",
"*/",
"class Foo",
"{",
"}"});
    }
    
    public void test4()
    {
        assertAnon(new String[] {
"/* Blah */ import bar;",
"/** Some comment",
"*/",
"class Foo",
"{",
"}"},

new String[] {
"/* #### */ import bar;",
"/** #### #######",
"*/",
"class Foo",
"{",
"}"});
    }
    
    public void test5()
    {
        assertAnon(new String[] {
"/* Blah */ import bar;",
"/** Some comment",
"*/",
"Foo"
},

new String[] {
"/* #### */ import bar;",
"/** #### #######",
"*/",
"Foo"});
    }
    
    public void test6()
    {
        assertAnon(new String[] {
"/* Blah */ /** Fiver */ import bar;",
"/** Some comment",
"*/",
"Foo"
},

new String[] {
"/* #### */ /** ##### */ import bar;",
"/** #### #######",
"*/",
"Foo"});
    }
}
