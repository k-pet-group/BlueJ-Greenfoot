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

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Stack;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import bluej.parser.DocumentReader;
import bluej.parser.EditorParser;
import bluej.parser.EscapedUnicodeReader;
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
 * determine whether a subnode represents a complete piece, etc.<p>
 * 
 * IncrementalParsingNode has basic support for sequential parse states, where a node
 * consists of several parts in sequence and each part must be parsed differently. The
 * "stateMarkers" array contains the offset (from the node beginning) of each state
 * transition; subclasses should assign it an array of appropriate size. A value of -1
 * in any entry means the marker is invalid.
 * 
 * @author Davin McCall
 */
public abstract class IncrementalParsingNode extends ParentParsedNode
{
    int [] stateMarkers = new int[0];
    
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
    protected int reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
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
        
        int state = getCurrentState(offset - nodePos);
        int nextStatePos = (state < stateMarkers.length) ? stateMarkers[state] + nodePos : -1;
        
        LocatableToken laToken = parser.getTokenStream().LA(1);
        int ttype = laToken.getType();
        if (ttype == JavaTokenTypes.EOF) {
            while (nextChild != null) {
                childRemoved(nextChild, listener);
                nextChild = childQueue.poll();
            }
            parser.completedNode(this, nodePos, getSize());
            return checkEnd(document, nodePos, listener);
        }
        
        while (! isNodeEndMarker(ttype)) {
            
            int tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
            if (nextChild != null && nextChild.getPosition() <= tokpos) {
                if (isDelimitingNode(nextChild)) {
                    break; // we're done!
                }
            }
            if (tokpos == nextStatePos && tokpos == (nodePos + stateMarkers[state])) {
                // If we reached the next state state position, we're at a safe boundary
                // Note that we cache "nextStatePos" because a partial parse might move the
                // state marker to the next token; it is only safe to stop parsing if the
                // state boundary hasn't moved.
                break;
            }
            
            // We may have transitioned to the next (or a later?) state
            while (state < stateMarkers.length && (tokpos - nodePos) >= stateMarkers[state]) {
                state++;
                nextStatePos = (state < stateMarkers.length) ? stateMarkers[state] + nodePos : -1;
            }
            
            LocatableToken last = doPartialParse(parser);
            if (parser.getTokenStream().LA(1) == laToken) {
                // We didn't manage to parse anything?
                parser.getTokenStream().nextToken();
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
                        int rep = reparseNode(document, nodePos, tokpos, listener);
                        return rep == ALL_OK ? NODE_GREW : rep;
                    }
                }
                parser.completedNode(this, nodePos, getSize());
                checkEnd(document, nodePos, listener);
                return ALL_OK;
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
                return NODE_SHRUNK;
            }
        }
        return ALL_OK;
    }    
    
    private Stack<ParsedNode> buildScopeStack()
    {
        Stack<ParsedNode> r = new Stack<ParsedNode>();
        ParsedNode pn = this;
        do {
            r.add(0, pn);
            pn = pn.getParentNode();
        } while (pn != null);
        
        return r;
    }
    
    /**
     * Convert a line and column number to an absolute position.
     */
    private static int lineColToPos(Document document, int line, int col)
    {
        return document.getDefaultRootElement().getElement(line - 1).getStartOffset() + col - 1;
    }
    
    private int getCurrentState(int pos)
    {
        for (int i = stateMarkers.length - 1; i >= 0; i--) {
            if (pos >= stateMarkers[i] && stateMarkers[i] >= 0) {
                return i + 1;
            }
        }
        return 0;
    }
    
    @Override
    public int textInserted(Document document, int nodePos, int insPos,
            int length, NodeStructureListener listener)
    {
        for (int i = 0; i < stateMarkers.length; i++) {
            if (stateMarkers[i] > (insPos - nodePos)) {
                stateMarkers[i] += length;
            }
        }
        return super.textInserted(document, nodePos, insPos, length, listener);
    }
    
    @Override
    public int textRemoved(Document document, int nodePos, int delPos,
            int length, NodeStructureListener listener)
    {
        for (int i = 0; i < stateMarkers.length; i++) {
            if (stateMarkers[i] > (delPos - nodePos)) {
                stateMarkers[i] -= length;
                if (stateMarkers[i] < (delPos - nodePos)) {
                    // The removed text straddles the state marker
                    stateMarkers[i] = -1;
                }
            }
        }
        return super.textRemoved(document, nodePos, delPos, length, listener);
    }
    
    /**
     * Check if a single line comment exists at the end of this node, which is not properly
     * terminated - that is, it ends before the end of the line. This can happen if such a
     * comment is inserted into an existing node which ends on the same line.
     */
    private int checkEnd(Document document, int nodePos, NodeStructureListener listener)
    {
        int end = nodePos + getSize();
        if (end >= document.getLength()) {
            return ALL_OK;
        }
        NodeAndPosition nap = findNodeAt(end - 1, nodePos);
        if (nap == null) {
            return ALL_OK;
        }
        int offset = nap.getPosition();
        if (offset + nap.getSize() < end
                || nap.getNode().getNodeType() != ParsedNode.NODETYPE_COMMENT) {
            // The final child node isn't a comment, or it ends before the end of this node.
            return ALL_OK;
        }
        
        Reader r = new DocumentReader(document, offset, nodePos + getSize());
        EscapedUnicodeReader eur = new EscapedUnicodeReader(r);
        try {
            if (eur.read() == '/' && eur.read() == '/') {
                // It's a single-line comment
                String str = document.getText(end, 1);
                if (str.charAt(0) != '\n') {
                    // The comment should extend to the end of the line, but it doesn't.
                    if (getParentNode().growChild(document,
                            new NodeAndPosition(this, nodePos, getSize()), listener)) {
                        // Successfully grew... now do some more parsing
                        return reparseNode(document, nodePos, offset, listener);
                    }
                    return REMOVE_NODE;
                }
            }
        }
        catch (IOException ioe) {}
        catch (BadLocationException ble) {
            // We might actually get this, but it's fine to return.
        }
        return ALL_OK;
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
        
        return false;
    }
}
