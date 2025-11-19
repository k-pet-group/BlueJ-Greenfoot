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

import bluej.extensions2.SourceType;
import bluej.parser.psi.SourceInput;
import org.junit.Rule;
import org.junit.Test;



/**
 * Basic Java 11 parse tests.
 */
public class Java11BasicParseTest
{


    /**
     * Test that a variable can be declared using the "var" keyword(ish).
     */
    @Test
    public void testVarDecl1()
    {
        SourceInput input = SourceInput.fromString(
                "var v = \"hello\";",
                SourceType.Java
        );
        SourceParser parser = new SourceParser(input);
        parser.parseStatement();
    }

    /**
     * Test that an explicitly typed variable can be called "var".
     */
    @Test
    public void testVarDecl2()
    {
        SourceInput input = SourceInput.fromString(
                "String var = \"hello\";",
                SourceType.Java
        );
        SourceParser parser = new SourceParser(input);
        parser.parseStatement();
    }

    /**
     * Test all the vars!
     */
    @Test
    public void testVarDecl3()
    {
        SourceInput input = SourceInput.fromString(
                "var var = \"var\" + var();",
                SourceType.Java
        );
        SourceParser parser = new SourceParser(input);
        parser.parseStatement();
    }

}
