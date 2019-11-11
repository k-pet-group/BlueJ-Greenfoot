/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2019  Michael Kolling and John Rosenberg 
 
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

import bluej.parser.nodes.ReparseableDocument.Element;
import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.ExpressionTypeInfo;
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
    private static enum Type
    {
        SL_NORMAL(true, TokenType.COMMENT_NORMAL),
        SL_SPECIAL(true, TokenType.COMMENT_SPECIAL),
        ML_NORMAL(false, TokenType.COMMENT_NORMAL),
        ML_JAVADOC(false, TokenType.COMMENT_JAVADOC),
        ML_SPECIAL(false, TokenType.COMMENT_SPECIAL);

        private final boolean singleLine;
        private final TokenType tokenType;

        private Type(boolean singleLine, TokenType tokenType)
        {
            this.singleLine = singleLine;
            this.tokenType = tokenType;
        }
    };

    private Type type;
    
    public CommentNode(ParsedNode parentNode, LocatableToken token)
    {
        super(parentNode);
        type = getCommentType(token);
    }
    
    /**
     * Determine the comment type from the token.
     */
    private static Type getCommentType(LocatableToken token)
    {
        String text = token.getText();
        if (token.getType() == JavaTokenTypes.ML_COMMENT) {
            if (text.startsWith("/*#")) {
                return Type.ML_SPECIAL;
            }
            if (text.startsWith("/**#")) {
                return Type.ML_SPECIAL;
            }
            if (text.startsWith("/**")) {
                return Type.ML_JAVADOC;
            }
            return Type.ML_NORMAL;
        }
        
        // Single line
        if (text.startsWith("//#")) {
            return Type.SL_SPECIAL;
        }
        
        return Type.SL_NORMAL;
    }
    
    public boolean isJavadocComment()
    {
        return type == Type.ML_JAVADOC;
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
            ReparseableDocument document)
    {
        Token tok = new Token(length, type.tokenType);
        tok.next = new Token(0, TokenType.END);
        return tok;
    }

    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }
    
    @Override
    public int textInserted(ReparseableDocument document, int nodePos, int insPos, int length,
            NodeStructureListener listener)
    {
        // grow ourself:
        int newSize = getSize() + length;
        resize(newSize);
        document.scheduleReparse(insPos, length);
        return ALL_OK;
    }

    @Override
    public int textRemoved(ReparseableDocument document, int nodePos, int delPos, int length,
            NodeStructureListener listener)
    {
        // shrink ourself:
        int newSize = getSize() - length;
        resize(newSize);
        document.scheduleReparse(delPos, 0);
        return ALL_OK;
    }

    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos, int offset, int maxParse,
            NodeStructureListener listener)
    {
        // Make a reader and parser
        int pline = document.getDefaultRootElement().getElementIndex(nodePos) + 1;
        int pcol = nodePos - document.getDefaultRootElement().getElement(pline - 1).getStartOffset() + 1;
        Reader r = document.makeReader(nodePos, nodePos + getSize());
        JavaLexer lexer = new JavaLexer(r, pline, pcol, nodePos);

        LocatableToken commentToken = lexer.nextToken();
        if (commentToken.getType() != JavaTokenTypes.SL_COMMENT &&
                commentToken.getType() != JavaTokenTypes.ML_COMMENT) {
            return REMOVE_NODE;
        }
        
        Type newType = getCommentType(commentToken);
        if (type.singleLine && !newType.singleLine) {
            // changed from single to multi-line
            return REMOVE_NODE;
        }
        else if (!type.singleLine && newType.singleLine) {
            // changed from multi-line to single line
            if (getOffsetFromParent() == 0 && getParentNode().isCommentAttached()) {
                return REMOVE_NODE;
            }
        }
        
        type = newType;
        
        int newEnd = lineColToPos(document, commentToken.getEndLine(),
                commentToken.getEndColumn());
        int newSize = newEnd - nodePos;
        document.markSectionParsed(nodePos, newSize);
        if (getSize() != newSize) {
            setSize(newSize);
            return NODE_SHRUNK;
        }
        
        return ALL_OK;
    }
    
    
    private static int lineColToPos(ReparseableDocument document, int line, int col)
    {
        Element map = document.getDefaultRootElement();
        Element lineEl = map.getElement(line - 1);
        return lineEl.getStartOffset() + col - 1;
    }
    
    @Override
    public ExpressionTypeInfo getExpressionType(int pos, ReparseableDocument document)
    {
        return null;
    }
}
