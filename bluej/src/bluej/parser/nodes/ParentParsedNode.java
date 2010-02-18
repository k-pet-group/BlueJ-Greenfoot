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

import java.util.HashMap;
import java.util.Map;

import javax.swing.text.Document;

import bluej.debugger.gentype.Reflective;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.editor.moe.Token;
import bluej.parser.DocumentReader;
import bluej.parser.JavaParser;
import bluej.parser.TokenStream;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * An abstract ParsedNode which delegates to child nodes.
 * 
 * @author Davin McCall
 */
public class ParentParsedNode extends ParsedNode
{
    protected Map<String,FieldNode> variables = new HashMap<String,FieldNode>();
    
    protected ParentParsedNode()
    {
        super();
    }
    
    public ParentParsedNode(ParsedNode myParent)
    {
        super(myParent);
    }
    
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(0, Token.END); // dummy
        if (length == 0) {
            return tok;
        }
        Token dummyTok = tok;
        
        NodeAndPosition np = getNodeTree().findNodeAtOrAfter(pos, nodePos);
        
        int cp = pos;
        while (np != null && np.getPosition() < (pos + length)) {
            if (cp < np.getPosition()) {
                int nextTokLen = np.getPosition() - cp;
                tok.next = tokenizeText(document, cp, nextTokLen);
                while (tok.next.id != Token.END) tok = tok.next;
                cp = np.getPosition();
            }
            
            int remaining = pos + length - cp;
            if (remaining > np.getSize() - cp + np.getPosition()) {
                remaining = np.getSize() - cp + np.getPosition();
            }
            if (remaining == 0) {
                break;
            }
            tok.next = np.getNode().getMarkTokensFor(cp, remaining, np.getPosition(), document);
            cp += remaining;
            while (tok.next.id != Token.END) {
                tok = tok.next;
            }
            np = getNodeTree().findNodeAtOrAfter(cp, nodePos);
        }
        
        // There may be a section left
        if (cp < pos + length) {
            int nextTokLen = pos + length - cp;
            tok.next = tokenizeText(document, cp, nextTokLen);
            while (tok.next.id != Token.END) tok = tok.next;
        }

        tok.next = new Token(0, Token.END);
        return dummyTok.next;
    }
    
    /**
     * Insert a FieldNode representing a variable/field declaration into this node.
     */
    public void insertVariable(FieldNode varNode, int pos, int size)
    {
        super.insertNode(varNode, pos, size);
        variables.put(varNode.getName(), varNode);
    }
    
    /**
     * Insert a field child (alias for insertVariable).
     */
    public void insertField(FieldNode child, int position, int size)
    {
        insertVariable(child, position, size);
    }
    
    @Override
    public JavaEntity getValueEntity(String name, Reflective querySource)
    {
        FieldNode var = variables.get(name);
        if (var != null) {
            JavaEntity fieldType = var.getFieldType().resolveAsType();
            if (fieldType != null) {
                return new ValueEntity(fieldType.getType());
            }
        }
        return super.getValueEntity(name, querySource);
    }
    
    protected static Token tokenizeText(Document document, int pos, int length)
    {
        DocumentReader dr = new DocumentReader(document, pos);
        TokenStream lexer = JavaParser.getLexer(dr,1,1);
        TokenStream tokenStream = new JavaTokenFilter(lexer, null);

        Token dummyTok = new Token(0, Token.END);
        Token token = dummyTok;
        
        int curcol = 1;
        while (length > 0) {
            LocatableToken lt = (LocatableToken) tokenStream.nextToken();

            if (lt.getLine() > 1 || lt.getColumn() - curcol >= length) {
                token.next = new Token(length, Token.NULL);
                token = token.next;
                break;
            }
            if (lt.getColumn() > curcol) {
                // some space before the token
                token.next = new Token(lt.getColumn() - curcol, Token.NULL);
                token = token.next;
                length -= token.length;
                curcol += token.length;
            }

            byte tokType = Token.NULL;
            if (JavaParser.isPrimitiveType(lt)) {
                tokType = Token.PRIMITIVE;
            }
            else if (JavaParser.isModifier(lt)) {
                tokType = Token.KEYWORD1;
            }
            else if (lt.getType() == JavaTokenTypes.STRING_LITERAL) {
                tokType = Token.LITERAL1;
            }
            else if (lt.getType() == JavaTokenTypes.CHAR_LITERAL) {
                tokType = Token.LITERAL2;
            }
            else {
                switch (lt.getType()) {
                case JavaTokenTypes.LITERAL_assert:
                case JavaTokenTypes.LITERAL_for:
                case JavaTokenTypes.LITERAL_switch:
                case JavaTokenTypes.LITERAL_while:
                case JavaTokenTypes.LITERAL_do:
                case JavaTokenTypes.LITERAL_try:
                case JavaTokenTypes.LITERAL_catch:
                case JavaTokenTypes.LITERAL_throw:
                case JavaTokenTypes.LITERAL_finally:
                case JavaTokenTypes.LITERAL_return:
                case JavaTokenTypes.LITERAL_case:
                case JavaTokenTypes.LITERAL_break:
                case JavaTokenTypes.LITERAL_if:
                case JavaTokenTypes.LITERAL_else:
                case JavaTokenTypes.LITERAL_new:
                    tokType = Token.KEYWORD1;
                    break;

                case JavaTokenTypes.LITERAL_class:
                case JavaTokenTypes.LITERAL_package:
                case JavaTokenTypes.LITERAL_import:
                case JavaTokenTypes.LITERAL_extends:
                case JavaTokenTypes.LITERAL_interface:
                case JavaTokenTypes.LITERAL_enum:
                    tokType = Token.KEYWORD2;
                    break;

                case JavaTokenTypes.LITERAL_this:
                case JavaTokenTypes.LITERAL_null:
                case JavaTokenTypes.LITERAL_super:
                case JavaTokenTypes.LITERAL_true:
                case JavaTokenTypes.LITERAL_false:
                    tokType = Token.KEYWORD3;
                    break;

                default:
                }
            }
            int toklen = lt.getLength();
            if (lt.getEndLine() > 1) {
                toklen = length;
            }
            token.next = new Token(toklen, tokType);
            token = token.next;
            length -= toklen;
            curcol += toklen;
        }
        
        token.next = new Token(0, Token.END);
        return dummyTok.next;
    }

    public void textInserted(Document document, int nodePos, int insPos,
            int length)
    {
        NodeAndPosition child = getNodeTree().findNode(insPos, nodePos);
        if (child != null) {
            ParsedNode cnode = child.getNode();
            NodeTree cnodeTree = cnode.getContainingNodeTree();
            // grow the child node
            cnodeTree.setNodeSize(cnodeTree.getNodeSize() + length);
            // inform the child node of the change
            child.getNode().textInserted(document, child.getPosition(), insPos, length);
        }
        else {
            // We must handle the insertion ourself
            // TODO
            // for now just do a full reparse
            reparseNode(document, nodePos, 0);
        }
    }
    
    public void textRemoved(Document document, int nodePos, int delPos,
            int length)
    {
        int endPos = delPos + length;
        
        NodeAndPosition child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
        
        if (child != null && child.getPosition() < delPos) {
            // Remove the end portion (or middle) of the child node
            int childEndPos = child.getPosition() + child.getSize();
            if (childEndPos > endPos) {
                // Remove the middle of the child node
                child.getNode().textRemoved(document, child.getPosition() + nodePos, delPos, length);
                NodeTree childTree = child.getNode().getContainingNodeTree();
                childTree.setNodeSize(childTree.getNodeSize() - length);

                reparseNode(document, nodePos, 0);
                ((MoeSyntaxDocument) document).documentChanged();
                return;
            }
            else {
                // Remove the end portion of the child node
                int rlength = childEndPos - delPos; // how much is removed
                child.getNode().textRemoved(document, child.getPosition() + nodePos, delPos, rlength);
                NodeTree childTree = child.getNode().getContainingNodeTree();
                childTree.setNodeSize(childTree.getNodeSize() - length);
                length -= rlength;
                endPos -= rlength;
            }
            child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
        }
        
        if (child != null) {
            int childPos = child.getPosition();
            int childLen = child.getSize();
            
            while (childPos + childLen < endPos) {
                // The whole child should be removed
                child.getNode().getContainingNodeTree().remove();
                child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
                if (child == null) {
                    break;
                }
                childPos = child.getPosition();
                childLen = child.getSize();
            }
            
            if (child != null) {
                if (childPos < endPos) {
                    int slideLen = childPos - delPos;
                    child.getNode().getContainingNodeTree().slideNode(-slideLen);
                    length -= slideLen;
                    child.getNode().textRemoved(document, childPos, delPos, length - slideLen);
                    child.getNode().getContainingNodeTree().setNodeSize(child.getSize() - length);
                }
                else {
                    child.getNode().getContainingNodeTree().slideNode(-length);
                }
            }
            
        }
        
        reparseNode(document, nodePos, 0);
        ((MoeSyntaxDocument) document).documentChanged();
    }

    /**
     * Re-parse the node. The default implementation passes the request down to the parent.
     * The tree root must provide a different implementation.
     */
    protected void reparseNode(Document document, int nodePos, int offset)
    {
        // Get own offset
        int noffset = offset;
        if (getContainingNodeTree() != null) {
            noffset += getContainingNodeTree().getPosition();
        }
        getParentNode().reparseNode(document, nodePos - noffset, 0);
    }
    
}
