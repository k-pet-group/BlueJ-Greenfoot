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
package bluej.parser.nodes;

import bluej.parser.EditorParser;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * A node representing a method or constructor inner body (the part between, but not
 * including, the '{' and '}').
 * 
 * @author Davin McCall
 */
public class MethodBodyNode extends IncrementalParsingNode
{    
    public MethodBodyNode(JavaParentNode parent)
    {
        super(parent);
        complete = true;
        setInner(true);
    }
    
    @Override
    protected boolean isDelimitingNode(NodeAndPosition<ParsedNode> nap)
    {
        return nap.getNode().isContainer();
    }
    
    @Override
    protected int doPartialParse(ParseParams params, int state)
    {
        last = params.tokenStream.nextToken();

        if (last.getType() == JavaTokenTypes.RCURLY) {
            return PP_ENDS_NODE;
        }
        if (last.getType() == JavaTokenTypes.EOF) {
            params.abortPos = lineColToPos(params.document, last.getLine(), last.getColumn());
            return complete ? PP_ABORT : PP_INCOMPLETE;
        }
        
        if (checkBoundary(params, last)) {
            return PP_PULL_UP_CHILD;
        }
        
        last = params.parser.parseStatement(last);
        if (last == null) {
            last = params.tokenStream.LA(1);
            if (last.getType() == JavaTokenTypes.EOF) {
                return PP_INCOMPLETE;
            }
        }
        return PP_OK;
    }
    
    @Override
    protected boolean lastPartialCompleted(EditorParser parser, LocatableToken token, int state)
    {
        return token != null;
    }
    
    @Override
    protected boolean isNodeEndMarker(int tokenType)
    {
        //return tokenType == JavaTokenTypes.RCURLY;
        return false;
    }
    
    @Override
    protected boolean marksOwnEnd()
    {
        return false;
    }
    
    @Override
    public boolean growsForward()
    {
        return true;
    }
}
