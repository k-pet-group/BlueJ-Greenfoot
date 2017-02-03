/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2016,2017  Michael Kolling and John Rosenberg
 
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
package bluej.parser.lexer;

import java.util.LinkedList;
import java.util.List;

import bluej.parser.JavaParser;
import bluej.parser.TokenStream;


/**
 * This is a token stream processor. It removes whitespace and comment tokens from the
 * stream, attaching comments as hidden tokens to the next suitable token.
 * 
 * @author Davin McCall
 */
public final class JavaTokenFilter implements TokenStream
{
    private TokenStream sourceStream;
    private LocatableToken lastComment;
    private LocatableToken cachedToken;
    private List<LocatableToken> buffer = new LinkedList<LocatableToken>();
    private LinkedList<LocatableToken> recent = new LinkedList<>();
    private JavaParser parser;
    
    public JavaTokenFilter(TokenStream source)
    {
        sourceStream = source;
        lastComment = null;
    }
    
    public JavaTokenFilter(TokenStream source, JavaParser parser)
    {
        this(source);
        this.parser = parser;
    }
        
    public LocatableToken nextToken()
    {
        LocatableToken rval;
        if (! buffer.isEmpty()) {
            // Make sure we have a cached token if buffer is only size 1;
            // necessary to ensure that token lengths are set correctly.
            // If buffer length is 2, we know token we are getting will have size
            // set correctly.
            if (cachedToken == null && buffer.size() == 1) {
                cachedToken = nextToken2();
            }
            rval = buffer.remove(buffer.size() - 1);
        }
        else
        {
            // We cache one lookahead token so that we can be sure the returned token
            // has its end column set correctly. (The end column for a token can only be set
            // when the following token is received).

            if (cachedToken == null)
            {
                rval = nextToken2();
            }
            else
            {
                rval = cachedToken;
            }

            cachedToken = nextToken2();
        }
        // We keep up to 30 in the recent list; if you push back more than that,
        // we don't keep track:
        recent.addLast(rval);
        if (recent.size() > 30)
            recent.removeFirst();
        return rval;
    }
    
    /**
     * Push a token on to the stream. The token will be returned by the next call
     * to nextToken().
     */
    public void pushBack(LocatableToken token)
    {
        buffer.add(token);
        if (!recent.isEmpty() && token == recent.getLast())
            recent.removeLast();
    }

    /**
     * Gets the most recent token returned by nextToken which has not been
     * pushed back using pushBack.
     */
    public LocatableToken getMostRecent()
    {
        return recent.isEmpty() ? null : recent.getLast();
    }
    
    /**
     * Look ahead a certain number of tokens (without actually consuming them).
     * @param distance  The distance to look ahead (1 or greater).
     */
    public LocatableToken LA(int distance)
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
    
    private LocatableToken nextToken2()
    {
        LocatableToken t = null;
        
        // Repeatedly read tokens until we find a non-comment, non-whitespace token.
        while (true) {
            t = (LocatableToken) sourceStream.nextToken();
            
            int ttype = t.getType();
            if (ttype == JavaTokenTypes.ML_COMMENT) {
                // If we come across a comment, save it.
                lastComment = t;
                if (parser != null) {
                    parser.gotComment(t);
                }
            }
            else if (ttype == JavaTokenTypes.SL_COMMENT) {
                if (parser != null) {
                    parser.gotComment(t);
                }
            }
            else {
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
