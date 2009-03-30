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
package bluej.parser.ast;

import java.io.Reader;

import antlr.RecognitionException;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.collections.AST;
import bluej.parser.EscapedUnicodeReader;
import bluej.parser.JavaTokenFilter;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaRecognizer;

/**
 * Analyse Java source and create an AST tree of the source.
 *
 * @author Andrew
 */
public class JavaAnalyzer
{
    private boolean parsedOk;
    private AST rootAST;
    
    /**
     *
     */
    public JavaAnalyzer(Reader r) throws RecognitionException, TokenStreamException
    {
        parsedOk = false;
        
        // We use a lexer pipeline:
        // First, deal with escaped unicode characters:
        EscapedUnicodeReader eur = new EscapedUnicodeReader(r);

        // Next create the initial lexer stage
        JavaLexer lexer = new JavaLexer(eur);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        eur.setAttachedScanner(lexer);
        
        // Finally filter out comments and whitespace
        TokenStream filter = new JavaTokenFilter(lexer);

        // create a parser that reads from the scanner
        JavaRecognizer parser = new JavaRecognizer(filter);
        parser.setASTNodeClass("bluej.parser.ast.LocatableAST");

        // start parsing at the compilationUnit rule
        parser.compilationUnit();
      
        rootAST = parser.getAST();

        parsedOk = true;            
    }
    
    public AST getAST()
    {
        return rootAST;
    }
}
