/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg 
 
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

import org.junit.Rule;
import org.junit.Test;

import bluej.JavaFXThreadingRule;

/**
 * Basic Java 11 parse tests.
 */
public class Java11BasicParseTest
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    /**
     * Test that a variable can be declared using the "var" keyword(ish).
     */
    @Test
    public void testVarDecl1()
    {
        StringReader sr = new StringReader(
                "var v = \"hello\";"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test that an explicitly typed variable can be called "var".
     */
    @Test   
    public void testVarDecl2()
    {
        StringReader sr = new StringReader(
                "String var = \"hello\";"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }

    /**
     * Test all the vars!
     */
    @Test
    public void testVarDecl3()
    {
        StringReader sr = new StringReader(
                "var var = \"var\" + var();"
        );
        JavaParser ip = new JavaParser(sr);
        ip.parseStatement();
    }
    
}
