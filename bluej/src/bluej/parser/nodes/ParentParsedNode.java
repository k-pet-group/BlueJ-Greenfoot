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
import java.util.Iterator;
import java.util.Map;

import javax.swing.text.Document;

import bluej.debugger.gentype.Reflective;
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

    @Override
    public void textInserted(Document document, int nodePos, int insPos,
            int length, NodeStructureListener listener)
    {
        // grow ourself:
        int newSize = getSize() + length;
        resize(newSize);
        
        NodeAndPosition child = getNodeTree().findNodeAtOrAfter(insPos, nodePos);
        if (child != null && child.getPosition() < insPos) {
            ParsedNode cnode = child.getNode();
            // let the child handle the change.
            cnode.textInserted(document, child.getPosition(), insPos, length, listener);
        }
        else {
            // We must handle the insertion ourself
            // Slide any children:
            if (child != null) {
                child.getNode().getContainingNodeTree().slideNode(length);
            }
            reparseNode(document, nodePos, insPos, listener);
        }
    }
    
    @Override
    public void textRemoved(Document document, int nodePos, int delPos,
            int length, NodeStructureListener listener)
    {
        // shrink ourself:
        int newSize = getSize() - length;
        resize(newSize);
        
        int endPos = delPos + length;
        
        NodeAndPosition child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
        
        if (child != null && child.getPosition() < delPos) {
            // Remove the end portion (or middle) of the child node
            int childEndPos = child.getPosition() + child.getSize();
            if (childEndPos >= endPos) {
                // Remove the middle of the child node
                child.getNode().textRemoved(document, child.getPosition(), delPos, length, listener);
                return;
            }
            
            do {
                // Remove the end portion of the child node
                int rlength = childEndPos - delPos; // how much is removed
                child.getNode().textRemoved(document, child.getPosition(), delPos, rlength, listener);
                length -= rlength;
                endPos -= rlength;
                // This is done in a loop in case the child has enlarged itself
            } while (child.getEnd() > delPos && length > 0);
            if (length == 0) {
                return;
            }
            child = child.nextSibling();
        }
        
        while (child != null) {
            if (child.getPosition() >= endPos) {
                child.getNode().getContainingNodeTree().slideNode(-length);
                break;
            }
            
            // Either remove the whole child node, or its beginning. Seeing as it's
            // unlikely that removing the beginning of a node is helpful, we'll just
            // remove it all.
            NodeAndPosition nextChild = child.nextSibling();
            removeChild(child, listener);
            child = nextChild;
        }
        
        reparseNode(document, nodePos, delPos, listener);
    }

    /**
     * Remove a child node, and notify the NodeStructureListener that the child and
     * its descendants have been removed. 
     */
    protected final void removeChild(NodeAndPosition child, NodeStructureListener listener)
    {
        child.getNode().remove();
        childRemoved(child, listener);
    }
    
    protected void childRemoved(NodeAndPosition child, NodeStructureListener listener)
    {
        listener.nodeRemoved(child);
        removeChildren(child, listener);
    }
    
    /**
     * Notify the NodeStructureListener that all descendants of a particular node
     * are removed, due to the node itself having been removed. (Note this does not actually
     * remove the children from the parent node).
     */
    protected static void removeChildren(NodeAndPosition node, NodeStructureListener listener)
    {
        Iterator<NodeAndPosition> i = node.getNode().getChildren(node.getPosition());
        while (i.hasNext()) {
            NodeAndPosition nap = i.next();
            listener.nodeRemoved(nap);
            removeChildren(nap, listener);
        }
    }
    
    /**
     * Re-parse the node. The default implementation passes the request down to the parent.
     * The tree root must provide a different implementation.
     */
    @Override
    protected void reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
    {
        // Get own offset
        int noffset = 0;
        if (getContainingNodeTree() != null) {
            noffset = getContainingNodeTree().getPosition();
        }
        getParentNode().reparseNode(document, nodePos - noffset, offset, listener);
    }
    
    @Override
    protected void childShrunk(Document document, NodeAndPosition child,
            NodeStructureListener listener)
    {
        int mypos = child.getPosition() - child.getNode().getOffsetFromParent();
        reparseNode(document, mypos, child.getEnd(), listener);
    }
    
    @Override
    protected boolean growChild(Document document, NodeAndPosition child,
            NodeStructureListener listener)
    {
        // Without any further knowledge, we're just going to have to do a full reparse.
        // Subclasses should override this to improve performance.
        int mypos = child.getPosition() - child.getNode().getOffsetFromParent();
        reparseNode(document, mypos, mypos, listener);
        return false;
    }
    
}
