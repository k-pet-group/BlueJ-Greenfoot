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
    
    @Override
    public Token getMarkTokensFor(int pos, int length, int nodePos,
            Document document)
    {
        Token tok = new Token(0, Token.END); // dummy
        if (length == 0) {
            return tok;
        }
        Token dummyTok = tok;
        
        NodeAndPosition<ParsedNode> np = getNodeTree().findNodeAtOrAfter(pos, nodePos);
        
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

    @Override
    public int textInserted(Document document, int nodePos, int insPos,
            int length, NodeStructureListener listener)
    {
        // grow ourself:
        int newSize = getSize() + length;
        resize(newSize);
        
        NodeAndPosition<ParsedNode> child = getNodeTree().findNodeAtOrAfter(insPos - 1, nodePos);
        if (child != null && (child.getPosition() < insPos
                || child.getPosition() == insPos && child.getNode().growsForward())) {
            ParsedNode cnode = child.getNode();
            // let the child handle the change.
            int r = cnode.textInserted(document, child.getPosition(), insPos, length, listener);
            if (r == NODE_GREW || r == NODE_SHRUNK) {
                newSize = child.getNode().getSize();
                return reparseNode(document, nodePos, child.getPosition() + newSize, listener);
            }
            else if (r == REMOVE_NODE) {
                removeChild(child, listener);
                return reparseNode(document, nodePos, child.getPosition(), listener);
            }
            return ALL_OK;
        }
        else {
            // We must handle the insertion ourself
            // Slide any children:
            if (child != null) {
                child.getNode().getContainingNodeTree().slideNode(length);
            }
            return reparseNode(document, nodePos, insPos, listener);
        }
    }
    
    @Override
    public int textRemoved(Document document, int nodePos, int delPos,
            int length, NodeStructureListener listener)
    {
        // shrink ourself:
        int newSize = getSize() - length;
        resize(newSize);
        
        int endPos = delPos + length;
        
        NodeAndPosition<ParsedNode> child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
        
        if (child != null && child.getPosition() < delPos) {
            // Remove the end portion (or middle) of the child node
            int childEndPos = child.getPosition() + child.getSize();
            if (childEndPos >= endPos) {
                // Remove the middle of the child node
                //child.getNode().resize(child.getSize() - length);
                int r = child.getNode().textRemoved(document, child.getPosition(), delPos, length, listener);
                if (r == REMOVE_NODE) {
                    removeChild(child, listener);
                    return reparseNode(document, nodePos, child.getPosition(), listener);
                }
                else if (r != ALL_OK) {
                    newSize = child.getSize();
                    return reparseNode(document, nodePos, child.getPosition() + newSize, listener);
                }
                return ALL_OK;
            }
            
            // Remove the end portion of the child node
            int rlength = childEndPos - delPos; // how much is removed

            // Remove any following nodes as necessary
            NodeAndPosition<ParsedNode> next = child.nextSibling();
            while (next != null && next.getEnd() < endPos) {
                NodeAndPosition<ParsedNode> nnext = next.nextSibling();
                removeChild(next, listener);
                next = nnext;
            }

            // Inform the child of the removed text 
            if (next != null) {
                next.getNode().getContainingNodeTree().slideNode(rlength - length);
            }
            //child.getNode().resize(child.getSize() - rlength);
            int r = child.getNode().textRemoved(document, child.getPosition(), delPos, rlength, listener);
            int reparseOffset;
            if (r == REMOVE_NODE) {
                reparseOffset = child.getPosition();
                removeChild(child, listener);
            }
            else {
                reparseOffset = child.getPosition() + child.getNode().getSize();
            }

            return reparseNode(document, nodePos, reparseOffset, listener);
        }
        
        while (child != null && child.getPosition() < endPos) {
            NodeAndPosition<ParsedNode> nextChild = child.nextSibling();
            removeChild(child, listener);
            child = nextChild;
        }

        if (child != null) {
            child.getNode().getContainingNodeTree().slideNode(-length);
        }
        
        return reparseNode(document, nodePos, delPos, listener);
    }
    
    /*
     * Default implementation, just causes the parent to re-parse
     */
    @Override
    protected int reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
    {
        return REMOVE_NODE;
    }
    
    @Override
    public void reparse(MoeSyntaxDocument document, int nodePos, int offset,
            NodeStructureListener listener)
    {
        NodeAndPosition<ParsedNode> nap = findNodeAt(offset, nodePos);
        if (nap != null) {
            nap.getNode().reparse(document, nap.getPosition(), offset, listener);
        }
        else {
            int r = reparseNode(document, nodePos, offset, listener);
            int size = getSize();
            if (r == REMOVE_NODE) {
                ParsedNode parent = getParentNode();
                parent.removeChild(new NodeAndPosition<ParsedNode>(this,
                        nodePos, getSize()), listener);
                document.scheduleReparse(nodePos, size);
            }
            else if (r == NODE_GREW || r == NODE_SHRUNK) {
                int nsize = getSize();
                document.scheduleReparse(nodePos + nsize, Math.min(size - nsize, 1));
            }
        }
    }
    
    @Override
    protected boolean growChild(Document document, NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        // Without any further knowledge, we're just going to have to do a full reparse.
        // Subclasses should override this to improve performance.
        return false;
    }
    
}
