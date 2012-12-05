/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012  Michael Kolling and John Rosenberg 
 
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
import javax.swing.text.Element;

import bluej.editor.moe.MoeSyntaxDocument;
import bluej.editor.moe.Token;
import bluej.parser.CodeSuggestions;
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
    byte type;
    private static byte SL_NORMAL = 0;
    private static byte SL_SPECIAL = 1;
    private static byte ML_NORMAL = 2;
    private static byte ML_JAVADOC = 3;
    private static byte ML_SPECIAL = 4;
    
    private static byte [] colours = {
        Token.COMMENT1,
        Token.COMMENT3,
        Token.COMMENT1,
        Token.COMMENT2,
        Token.COMMENT3
    };
    
    public CommentNode(ParsedNode parentNode, LocatableToken token)
    {
        super(parentNode);
        type = getCommentType(token);
    }
    
    /**
     * Determine the comment type from the token.
     */
    private static byte getCommentType(LocatableToken token)
    {
        String text = token.getText();
        if (token.getType() == JavaTokenTypes.ML_COMMENT) {
            if (text.startsWith("/*#")) {
                return ML_SPECIAL;
            }
            if (text.startsWith("/**#")) {
                return ML_SPECIAL;
            }
            if (text.startsWith("/**")) {
                return ML_JAVADOC;
            }
            return ML_NORMAL;
        }
        
        // Single line
        if (text.startsWith("//#")) {
            return SL_SPECIAL;
        }
        
        return SL_NORMAL;
    }
    
    public boolean isJavadocComment()
    {
        return type == ML_JAVADOC;
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
        Token tok = new Token(length, colours[type]);
        tok.next = new Token(0, Token.END);
        return tok;
    }

    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    @Override
    public int textInserted(MoeSyntaxDocument document, int nodePos, int insPos, int length,
            NodeStructureListener listener)
    {
        // grow ourself:
        int newSize = getSize() + length;
        resize(newSize);
        document.scheduleReparse(insPos, length);
        return ALL_OK;
    }

    @Override
    public int textRemoved(MoeSyntaxDocument document, int nodePos, int delPos, int length,
            NodeStructureListener listener)
    {
        // shrink ourself:
        int newSize = getSize() - length;
        resize(newSize);
        document.scheduleReparse(delPos, 0);
        return ALL_OK;
    }

    @Override
    protected int reparseNode(Document document, int nodePos, int offset, int maxParse,
            NodeStructureListener listener)
    {
        // Make a reader and parser
        int pline = document.getDefaultRootElement().getElementIndex(nodePos) + 1;
        int pcol = nodePos - document.getDefaultRootElement().getElement(pline - 1).getStartOffset() + 1;
        Reader r = new DocumentReader(document, nodePos, nodePos + getSize());
        JavaLexer lexer = new JavaLexer(r, pline, pcol, nodePos);

        LocatableToken commentToken = lexer.nextToken();
        if (commentToken.getType() != JavaTokenTypes.SL_COMMENT &&
                commentToken.getType() != JavaTokenTypes.ML_COMMENT) {
            return REMOVE_NODE;
        }
        
        byte newType = getCommentType(commentToken);
        if (type <= SL_SPECIAL && newType > SL_SPECIAL) {
            // changed from single to multi-line
            return REMOVE_NODE;
        }
        else if (type > SL_SPECIAL && newType <= SL_SPECIAL) {
            // changed from multi-line to single line
            if (getOffsetFromParent() == 0 && getParentNode().isCommentAttached()) {
                return REMOVE_NODE;
            }
        }
        
        type = newType;
        
        int newEnd = lineColToPos(document, commentToken.getEndLine(),
                commentToken.getEndColumn());
        int newSize = newEnd - nodePos;
        ((MoeSyntaxDocument)document).markSectionParsed(nodePos, newSize);
        if (getSize() != newSize) {
            setSize(newSize);
            return NODE_SHRUNK;
        }
        
        return ALL_OK;
    }
    
    
    private static int lineColToPos(Document document, int line, int col)
    {
        Element map = document.getDefaultRootElement();
        Element lineEl = map.getElement(line - 1);
        return lineEl.getStartOffset() + col - 1;
    }
    
    @Override
    public CodeSuggestions getExpressionType(int pos, Document document)
    {
        return null;
    }
}
