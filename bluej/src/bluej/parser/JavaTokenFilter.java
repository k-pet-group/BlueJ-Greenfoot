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

import java.util.LinkedList;
import java.util.List;

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
    private List<LocatableToken> buffer = new LinkedList<LocatableToken>();
    
    public JavaTokenFilter(TokenStream source)
    {
        sourceStream = source;
        lastComment = null;
    }
    
    public LocatableToken nextToken() throws TokenStreamException
    {
        if (! buffer.isEmpty()) {
        	// Make sure we have a cached token; necessary to ensure that token lengths
        	// are set correctly.
        	if (cachedToken == null) {
        		cachedToken = nextToken2();
        	}
        	return buffer.remove(buffer.size() - 1);
        }
    	
    	// We cache one lookahead token so that we can be sure the returned token
        // has its end column set correctly. (The end column for a token can only be set
        // when the following token is received).
        Token rval;
        if (cachedToken == null)
            rval = nextToken2();
        else
            rval = cachedToken;
        
        cachedToken = nextToken2();
        return (LocatableToken) rval;
    }
    
    /**
     * Push a token on to the stream. The token will be returned by the next call
     * to nextToken().
     */
    public void pushBack(LocatableToken token)
    {
    	buffer.add(token);
    }
    
    /**
     * Look ahead a certain number of tokens (without actually consuming them).
     * @param distance  The distance to look ahead (1 or greater).
     */
    public LocatableToken LA(int distance) throws TokenStreamException
    {
    	if (cachedToken != null) {
    		buffer.add(0, (LocatableToken) cachedToken);
    		cachedToken = null;
    	}
    	
    	int numToAdd = distance - buffer.size();
    	while (numToAdd > 0) {
    		buffer.add(0, nextToken2());
    		numToAdd--;
    	}
    
    	return buffer.get(buffer.size() - distance);
    }
    
    private LocatableToken nextToken2() throws TokenStreamException
    {    	
    	LocatableToken t = null;
        
        // Repeatedly read tokens until we find a non-comment, non-whitespace token.
        while (true) {
            t = (LocatableToken) sourceStream.nextToken();
            
            // The previous token ends at the beginning of this token.
            if (previousToken != null) {
                previousToken.setEndLineAndCol(t.getLine(), t.getColumn());
            }
            previousToken = t;
                        
            int ttype = t.getType();
            if (ttype == JavaTokenTypes.ML_COMMENT) {
                // If we come across a comment, save it.
                lastComment = t;
            }
            else if (ttype == JavaTokenTypes.SL_COMMENT) {
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
