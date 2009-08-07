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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.collections.AST;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.ClassScope;
import bluej.parser.symtab.PackageScope;
import bluej.parser.symtab.Scope;
import bluej.parser.symtab.Selection;
import bluej.utility.Debug;

/**
 * Parse class to get info.
 * 
 * To work "properly" this is a more complicated process which potentially requires parsing
 * multiple source files. However, at the moment we parse a single file at a time. We only
 * create dependencies to existing classes in the same package (as supplied).
 * 
 * @author Davin McCall
 * @version $Id: ClassParser.java 6497 2009-08-07 01:37:45Z davmac $
 */
public class ClassParser extends InfoParser
{
    // TODO: type parameters for methods and types should be inserted into the scope
    // for the method/type (as a type).
    
    public ClassParser(Reader r)
    {
    	super(r);
    }
}
