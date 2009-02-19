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

import antlr.Token;
import antlr.TokenStream;
import antlr.TokenStreamException;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;

/**
 * This is a token stream processor. It removes whitespace and comment tokens from the
 * stream, attaching comments as hidden tokens to the next suitable token.
 * 
 * @author Davin McCall
 */
public class JavaTokenFilter implements TokenStream
{
    private TokenStream sourceStream;
    private Token lastComment;
    private LocatableToken previousToken;
    private Token cachedToken;
    
    public JavaTokenFilter(TokenStream source)
    {
        sourceStream = source;
        lastComment = null;
    }
    
    public Token nextToken() throws TokenStreamException
    {
        // We cache one lookahead token so that we can be sure the returned token
        // has its end column set correctly. (The end column for a token can only be set
        // when the following token is received).
        Token rval;
        if (cachedToken == null)
            rval = nextToken2();
        else
            rval = cachedToken;
        
        cachedToken = nextToken2();
        return rval;
    }
    
    public Token nextToken2() throws TokenStreamException
    {
        LocatableToken t = null;
        
        // Repeatedly read tokens until we find a non-comment, non-whitespace token.
        while (true) {
            t = (LocatableToken) sourceStream.nextToken();
            
            // The previous token ends at the beginning of this token.
            if (previousToken != null) {
                previousToken.setEndColumn(t.getColumn());
            }
            previousToken = t;
            
            int ttype = t.getType();
            if (ttype == JavaTokenTypes.ML_COMMENT) {
                // If we come across a comment, save it.
                lastComment = t;
            }
            else if (ttype != JavaTokenTypes.WS) {
                // When we have an interesting token, attach the previous comment.
                if (lastComment != null) {
                    t.setHiddenBefore(lastComment);
                    lastComment = null;
                }
                break;
            }
        }
        
        return t;
    }
}
