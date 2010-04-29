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

import java.io.Reader;

import javax.swing.text.Document;

import bluej.editor.moe.MoeSyntaxDocument;
import bluej.editor.moe.Token;
import bluej.parser.DocumentReader;
import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;

/**
 * A node type for representing comments in the code.
 * 
 * @author Davin McCall
 */
public class CommentNode extends ParsedNode
{
    byte colour;
    
    public CommentNode(ParsedNode parentNode, byte colour)
    {
        super(parentNode);
        this.colour = colour;
    }
    
    @Override
    public int getNodeType()
    {
        return NODETYPE_COMMENT;
    }
    
    /* (non-Javadoc)
     * @see bluej.parser.nodes.ParsedNode#getMarkTokensFor(int, int, int, javax.swing.text.Document)
     */
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(length, colour);
        tok.next = new Token(0, Token.END);
        return tok;
    }

    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    @Override
    public int textInserted(Document document, int nodePos, int insPos, int length, NodeStructureListener listener)
    {
        // grow ourself:
        int newSize = getSize() + length;
        resize(newSize);
        ((MoeSyntaxDocument) document).scheduleReparse(insPos, length);
        return ALL_OK;
    }

    @Override
    public int textRemoved(Document document, int nodePos, int delPos, int length, NodeStructureListener listener)
    {
        // shrink ourself:
        int newSize = getSize() - length;
        resize(newSize);
        ((MoeSyntaxDocument) document).scheduleReparse(delPos, 0);
        return ALL_OK;
    }

    @Override
    protected int reparseNode(Document document, int nodePos, int offset,
            NodeStructureListener listener)
    {
        // Make a reader and parser
        int pline = document.getDefaultRootElement().getElementIndex(offset) + 1;
        int pcol = offset - document.getDefaultRootElement().getElement(pline - 1).getStartOffset() + 1;
        Reader r = new DocumentReader(document, offset, nodePos + getSize());
        JavaLexer lexer = new JavaLexer(r, pline, pcol);

        LocatableToken commentToken = lexer.nextToken();
        if (commentToken.getType() != JavaTokenTypes.SL_COMMENT &&
                commentToken.getType() != JavaTokenTypes.ML_COMMENT) {
            return REMOVE_NODE;
        }
        
        // DAV TODO
        // If the node start has changed, slide this node - and any containing nodes which
        // this node begins - to the correct place.
        
        // This node must end at the token end.
        
        // If we changed type (from multi- to single-line) we should
        // re-parse parent, probably
        
        return REMOVE_NODE;
    }
    
}
