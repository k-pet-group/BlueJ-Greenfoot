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
import java.util.LinkedList;
import java.util.Stack;

import javax.swing.text.Document;

import bluej.parser.DocumentReader;
import bluej.parser.EditorParser;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * An abstract base class for nodes which can do incremental parsing.<p>
 * 
 * We assume that such a node is broken into pieces which can be parsed separately.
 * At least some such pieces will form complete sub-nodes which allow us to determine
 * where we can re-parse from if a modification is made.<p>
 * 
 * Sub-classes must provide implementations for several methods to parse a piece,
 * determine whether a subnode represents a complete piece, etc.
 * 
 * @author Davin McCall
 */
public abstract class IncrementalParsingNode extends ParentParsedNode
{
    public IncrementalParsingNode(ParsedNode parent)
    {
        super(parent);
    }
    
    /**
     * Check whether the given node represents a complete parsed piece. If
     * it does, we can safely resume incremental parsing just beyond its
     * end. Also, if we are incrementally parsing, and we complete a piece
     * at the boundary with this node, we don't need to continue parsing.
     */
    protected abstract boolean isDelimitingNode(NodeAndPosition nap);
    
    /**
     * Actually perform a partial parse. If possible, this method should return
     * the last token forming part of the parsed piece or null if there was a
     * parsing error. (It is safe to always return null).
     */
    protected abstract LocatableToken doPartialParse(EditorParser parser);
    
    protected boolean lastPartialCompleted(EditorParser parser, LocatableToken token)
    {
        return token != null;
    }
    
    protected boolean isNodeEndMarker(int tokenType)
    {
        return tokenType == JavaTokenTypes.RCURLY;
    }

    @Override
    protected void reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
    {
        // Find the nearest container node prior to the reparse point.
        NodeAndPosition nap = null;
        if (offset > nodePos) {
            nap = getNodeTree().findNodeAtOrBefore(offset - 1, nodePos);
        }
        
        while (nap != null && !isDelimitingNode(nap)) {
            if (nap.getPosition() > nodePos) {
                nap = getNodeTree().findNodeAtOrBefore(nap.getPosition() - 1, nodePos);
            }
            else {
                nap = null;
            }
        }
        
        NodeAndPosition nextNap = null;
        if (nap != null) {
            nextNap = nap.nextSibling();
            if (nap.getEnd() > offset) {
                // The reparse position straddles the child node. Remove the child.
                removeChild(nap, listener);
                offset = nap.getPosition(); // re-parse from where the removed child was
            }
            else {
                offset = nap.getEnd();
            }
        }
        else {
            offset = nodePos; // reparse from this node's beginning
        }
        
        // Pull out the current child nodes into a queue. We re-insert them if we get the opportunity;
        // otherwise we'll have to dispose of them properly later.
        LinkedList<NodeAndPosition> childQueue = new LinkedList<NodeAndPosition>();
        if (nap == null) {
            nap = findNodeAtOrAfter(offset, nodePos);
        }
        else {
            nap = nextNap;
        }
        while (nap != null) {
            childQueue.add(nap);
            nextNap = nap.nextSibling();
            nap.getNode().remove();
            nap = nextNap;
        }
        
        // Make a reader and parser
        int pline = document.getDefaultRootElement().getElementIndex(offset) + 1;
        int pcol = offset - document.getDefaultRootElement().getElement(pline - 1).getStartOffset() + 1;
        Reader r = new DocumentReader(document, offset, nodePos + getSize());
        EditorParser parser = new EditorParser(document, r, pline, pcol, buildScopeStack());
        
        // Find the next child node, which we may bump into when we are parsing.
        NodeAndPosition nextChild = childQueue.poll();
        
        LocatableToken laToken = parser.getTokenStream().LA(1);
        int ttype = laToken.getType();
        if (ttype == JavaTokenTypes.EOF) {
            while (nextChild != null) {
                childRemoved(nextChild, listener);
                nextChild = childQueue.poll();
            }
            return;
        }
        
        while (! isNodeEndMarker(ttype)) {
            
            int tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
            if (nextChild != null && nextChild.getPosition() <= tokpos) {
                if (isDelimitingNode(nextChild)) {
                    break; // we're done!
                }
            }
            
            LocatableToken last = doPartialParse(parser);
            if (parser.getTokenStream().LA(1) == laToken) {
                // We didn't manage to parse anything?
                parser.getTokenStream().nextToken();
                laToken = parser.getTokenStream().LA(1);
                ttype = laToken.getType();
                continue;
            }
                        
            if (nextChild != null) {
                // Perhaps we've now overwritten part of nextChild, or otherwise we may have pushed
                // it further back.
                int epos;
                if (last != null) {
                    epos = lineColToPos(document, last.getEndLine(), last.getEndColumn());
                }
                else {
                    epos = lineColToPos(document, parser.getTokenStream().LA(1).getLine(),
                            parser.getTokenStream().LA(1).getColumn());
                }
                
                while (epos > nextChild.getPosition()) {
                    // Remove nextChild, we've eaten into it.
                    //NodeAndPosition sibling = nextChild.nextSibling();
                    childRemoved(nextChild, listener);
                    nextChild = childQueue.poll();
                    if (nextChild == null) {
                        break;
                    }
                }
            }
            
            LocatableToken nlaToken = parser.getTokenStream().LA(1);
            if (nlaToken.getType() == JavaTokenTypes.EOF) {
                if (! lastPartialCompleted(parser, last)) {
                    // The parsed piece wants more...
                    if (getParentNode().growChild(document,
                            new NodeAndPosition(this, nodePos, getSize()), listener)) {
                        // Successfully grew... now do some more parsing
                        reparseNode(document, nodePos, tokpos, listener);
                    }
                }
                return;
            }
            
            laToken = nlaToken;
            ttype = laToken.getType();
        }

        // Process the child queue
        while (nextChild != null) {
            insertNode(nextChild.getNode(), nextChild.getPosition() - nodePos, nextChild.getSize());
            nextChild = childQueue.poll();
        }
        
        if (isNodeEndMarker(ttype)) {
            // Did we shrink?
            int tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
            int newsize = tokpos - nodePos;
            if (newsize < getSize()) {
                setSize(newsize);
                getParentNode().childShrunk(document,
                        new NodeAndPosition(this, nodePos, newsize), listener);
            }
        }
    }    
    
    protected Stack<ParsedNode> buildScopeStack()
    {
        Stack<ParsedNode> r = new Stack<ParsedNode>();
        ParsedNode pn = this;
        do {
            r.add(0, pn);
            pn = pn.getParentNode();
        } while (pn != null);
        
        return r;
    }
    
    private static int lineColToPos(Document document, int line, int col)
    {
        return document.getDefaultRootElement().getElement(line - 1).getStartOffset() + col - 1;
    }
    
    @Override
    protected boolean growChild(Document document, NodeAndPosition child,
            NodeStructureListener listener)
    {
        int mypos = child.getPosition() - child.getNode().getOffsetFromParent();
        
        NodeAndPosition nap = child.nextSibling();
        if (nap != null && nap.getPosition() > child.getEnd()) {
            int newsize = nap.getPosition() - child.getPosition();
            child.getNode().resize(newsize);
            return true;
        }
        
        if (nap != null) {
            // Next child is pushing up against the one which wants to grow - so we'll
            // have to remove it.
            removeChild(nap, listener);
            child.getNode().resize(nap.getEnd() - child.getPosition());
            return true;
        }
        
        // The child can soak up anything remaining at the end of this node.
        int myEnd = mypos + getSize();
        if (myEnd > child.getEnd()) {
            int newsize = myEnd - child.getPosition();
            child.getNode().resize(newsize);
            return true;
        }
        
        // Maybe this node can grow, and then its child can also grow.
        if (getParentNode().growChild(document,
                new NodeAndPosition(this, mypos, getSize()), listener)) {
            int newsize = myEnd - child.getPosition();
            child.getNode().resize(newsize);
            return true;
        }
        
        getParentNode().reparseNode(document, mypos, mypos, listener);
        return false;
    }
}
