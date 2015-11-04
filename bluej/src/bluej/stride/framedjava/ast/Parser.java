/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.ast;

import java.io.StringReader;
import java.util.UUID;

import bluej.parser.JavaParser;
import bluej.parser.ParseFailure;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;

public class Parser
{

    public static boolean parseableAsType(String s)
    {
        JavaParser p = new JavaParser(new StringReader(s), false);
        try
        {
            // TODO not sure this handles multidim arrays with a size
            p.parseTypeSpec(true);
            
            // Only valid if we have parsed all the way to end of the String:
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false;
            
            return true;
        }
        catch (ParseFailure pf)
        {
            return false;
        }
    }
    
    // Checks if it can be parsed as the part following "import " and before the semi-colon
    public static boolean parseableAsImportTarget(String s)
    {
        JavaParser p = new JavaParser(new StringReader("import " + s + ";"), false);
        try
        {
            p.parseImportStatement();
            
            // Only valid if we have parsed all the way to end of the String:
            
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false;
            
            return true;
        }
        catch (ParseFailure pf)
        {
            return false;
        }
    }

    // We generate new names via a UUID because we don't want duplicates between
    // different dummy names; this would cause confusion and potential nuisance
    // compile errors
    public static String generateNewDummyName()
    {
        return "code__dummy__gf3gen__" + UUID.randomUUID().toString().replace('-', '_');
    }
    
    public static boolean isDummyName(String name)
    {
        return name.startsWith("code__dummy__gf3gen__");
    }

    public static boolean parseableAsNameDef(String s)
    {
        // We don't need to parse, just lex and see if it comes out as an ident token:
        JavaLexer lexer = new JavaLexer(new StringReader(s));
        LocatableToken t = lexer.nextToken();
        LocatableToken t2 = lexer.nextToken();
        if (t.getType() == JavaTokenTypes.IDENT && t2.getType() == JavaTokenTypes.EOF)
            return true;
        else
            return false;
    }

    public static boolean parseableAsExpression(String e)
    {
        return Parser.parseAsExpression(new JavaParser(new StringReader(e), false));
    }
    
    /**
     *  Tries to run the given parser by calling parseExpression.
     * Any ParseFailure exceptions are caught and false is returned.
     * If there is no exception, but after parsing we are not at EOF
     * then false is also returned.
     * true is only returned if there is no ParseFailure, and we parse
     * all the way to EOF
     */
    public static boolean parseAsExpression(JavaParser p)
    {
        try
        {
            p.parseExpression();
            
            // Only valid if we have parsed all the way to end of the String:
            
            LocatableToken tok = p.getTokenStream().nextToken();
            if (tok.getType() != JavaTokenTypes.EOF)
                return false; 
        }
        catch (ParseFailure pf)
        {
            //Debug.message("Invalid expression: " + pf.getMessage());
            return false;
        }
        return true;
        
    }
}
