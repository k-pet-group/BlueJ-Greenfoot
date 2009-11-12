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

import java.io.Reader;
import java.util.*;

//import bluej.parser.ast.*;
//import bluej.parser.ast.gen.*;

/**
 * @author Andrew
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class UnitTestAnalyzer
{
    UnitTestParser utp;
    
    /**
     * Analyse unit test source code.
     */
    public UnitTestAnalyzer(Reader r)
    {
        utp = new UnitTestParser(r);
    }

    /**
     * Extract from the unit testing source the list of source spans
     * for the fields declared in the unit test class.
     * 
     * ie
     *
     * class FooBar {
     *    private int a = 10;
     *    java.util.HashMap h,i,j = null;
     *    public String aString;
     * }
     * gives us a list with SourceSpan objects encompassing
     *   p in private to ;
     *   j in java to ;
     *   p in public to ;
     *
     * The list will be ordered in the order that the fields appear in the src.
     */
    public List<SourceSpan> getFieldSpans()
    {
        return utp.getFieldSpans();
    }

    /**
     * Extract from the unit testing source
     * the opening and closing bracket locations for the method 'methodName'.
     * We select only methods that do not have any parameters (all unit test
     * methods take no arguments).
     * ie
     *
     * class FooBar {
     *    public void setUp() {
     *       // do something
     *       i++;
     *    }
     * }
     * gives us a SourceSpan object from the second "{" to the first "}"
     */
    public SourceSpan getMethodBlockSpan(String methodName)
    {
        return utp.getMethodBlockSpan(methodName);
    }

    /**
     * Extract from the unit test source a source location where
     * we should insert declarations of fields (that will become
     * the classes fixtures).
     * 
     * @return
     */
    public SourceLocation getFixtureInsertLocation()
    {
        return utp.getFixtureInsertLocation();
    }

    /**
     * Extract from the unit test source a source location where
     * we can insert new methods.
     * 
     * @return
     */
    public SourceLocation getNewMethodInsertLocation()
    {
        return utp.getNewMethodInsertLocation();
    }
  
}
