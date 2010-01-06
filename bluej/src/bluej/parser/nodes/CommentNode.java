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

import javax.swing.text.Document;

import bluej.editor.moe.Token;

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
    
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(length, colour);
        tok.next = new Token(0, Token.END);
        return tok;
    }

    public void textInserted(Document document, int nodePos, int insPos, int length)
    {
        getParentNode().reparseNode(document, nodePos, 0);
    }

    public void textRemoved(Document document, int nodePos, int delPos, int length)
    {
        getParentNode().reparseNode(document, nodePos, 0);
    }

}
