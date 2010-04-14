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

import bluej.editor.moe.MoeSyntaxDocument;
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
    protected int [] stateMarkers = new int[0];
    protected boolean [] marksEnd = new boolean[0];
    
    /** The final token in the last partial parse. Should be set by doPartialParse if possible. */
    protected LocatableToken last;
    
    // Partial parse status values
    protected final static int PP_OK = 0;
    /** Node ends, due to a parser error */
    protected final static int PP_INCOMPLETE = 1;
    /** Node ends just before the "last" token */
    protected final static int PP_ENDS_NODE = 2;
    /** Parse completely failed. The node must be removed and the parent re-parsed. */
    protected final static int PP_EPIC_FAIL = 3;
    /** The "last" token ends the state. The new state begins. */
    protected final static int PP_ENDS_STATE = 4;
    /** The "last" token is the beginning of the next state */
    protected final static int PP_BEGINS_NEXT_STATE = 5;
    
    protected final static int PP_REGRESS_STATE = 6;
    /** Pull the next child up behind the "last" token and continue parsing inside it */
    protected final static int PP_PULL_UP_CHILD = 7;
    
    
    private final static int MAX_PARSE_PIECE = 8000;
    private final static int MIN_PARSE_PIECE = 6000;
    
    
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
    protected abstract boolean isDelimitingNode(NodeAndPosition<ParsedNode> nap);
    
    /**
     * Actually perform a partial parse. If possible, this method should set
     * "last" to the last token forming part of the parsed piece or null if there was a
     * parsing error. (It is safe to always set it to null).<p>
     * 
     * The return value is one of the PP_ constants: PP_OK, PP_ENDS_NODE if the parse
     * succeeds but requires that the node ends immediately, PP_EPIC_FAIL if the parse
     * fails and indicates that the node is not what it purports to be.
     */
    protected abstract int doPartialParse(ParseParams params, int state);
    
    protected boolean lastPartialCompleted(EditorParser parser, LocatableToken token, int state)
    {
        return token != null;
    }
    
    protected boolean isNodeEndMarker(int tokenType)
    {
        return tokenType == JavaTokenTypes.RCURLY;
    }
    
    
    /**
     * Returns true if this node marks its own end, that is, the token signifying
     * the end of this node is contained within this node itself, rather than in
     * the parent node.
     */
    protected abstract boolean marksOwnEnd();

    @Override
    protected int reparseNode(Document document, int nodePos, int offset, NodeStructureListener listener)
    {
        int parseEnd = Math.min(offset + MAX_PARSE_PIECE, nodePos + getSize());
        int state = getCurrentState(offset - nodePos);
        
        // Find the nearest container node or state boundary prior to the reparse point.
        int stateBoundary = (state != 0) ? stateMarkers[state - 1] + nodePos : nodePos;
        NodeAndPosition<ParsedNode> nap = null;
        if (offset > stateBoundary) {
            nap = getNodeTree().findNodeAtOrBefore(offset - 1, nodePos);
        }
        
        while (nap != null && !isDelimitingNode(nap)) {
            if (nap.getPosition() >= stateBoundary) {
                nap = getNodeTree().findNodeAtOrBefore(nap.getPosition() - 1, nodePos);
            }
            else {
                nap = null;
            }
        }
        
        NodeAndPosition<ParsedNode> nextNap = null;
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
            offset = stateBoundary; // reparse from previous state marker
        }
        
        // Pull out the current child nodes into a queue. We re-insert them if we get the opportunity;
        // otherwise we'll have to dispose of them properly later.
        LinkedList<NodeAndPosition<ParsedNode>> childQueue = new LinkedList<NodeAndPosition<ParsedNode>>();
        if (nap == null) {
            nap = findNodeAtOrAfter(offset + 1, nodePos);
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
        Reader r = new DocumentReader(document, offset, parseEnd);
        EditorParser parser = new EditorParser(document, r, pline, pcol, buildScopeStack());
        
        // Find the next child node, which we may bump into when we are parsing.
        NodeAndPosition<ParsedNode> nextChild = childQueue.peek();
        
        int nextStatePos = (state < stateMarkers.length) ? stateMarkers[state] : -1;
        nextStatePos += (nextStatePos == -1) ? 0 : nodePos;
        
        LocatableToken laToken = parser.getTokenStream().LA(1);
        int ttype = laToken.getType();
        
        ParseParams pparams = new ParseParams();
        pparams.listener = listener;
        pparams.parser = parser;
        pparams.tokenStream = parser.getTokenStream();
        pparams.document = (MoeSyntaxDocument) document;
        pparams.nodePos = nodePos;
        pparams.childQueue = childQueue;
        
        while (! isNodeEndMarker(ttype)) {
            
            int tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
            
            int overwritePos = tokpos;
            LocatableToken hiddenBefore = laToken.getHiddenBefore();
            if (hiddenBefore != null) {
                overwritePos = lineColToPos(document, hiddenBefore.getLine(),
                        hiddenBefore.getColumn());
            }
            
            nextChild = removeOverwrittenChildren(childQueue, nextChild, overwritePos, listener);
            if (nextChild != null && nextChild.getPosition() <= tokpos) {
                if (isDelimitingNode(nextChild)) {
                    processChildQueue(nodePos, childQueue, nextChild);
                    // parser.completedNode(this, nodePos, tokpos - nodePos);
                    parser.completedNode(this, nodePos, nextChild.getPosition() - nodePos);
                    return ALL_OK; // we're done!
                }
            }
            if (tokpos == nextStatePos && tokpos == (nodePos + stateMarkers[state])
                    || overwritePos == nextStatePos && overwritePos == (nodePos + stateMarkers[state])) {
                // If we reached the next state state position, we're at a safe boundary
                // Note that we cache "nextStatePos" because a partial parse might move the
                // state marker to the next token; it is only safe to stop parsing if the
                // state boundary hasn't moved.
                removeOverwrittenChildren(childQueue, nextChild, nextStatePos, listener);
                processChildQueue(nodePos, childQueue, nextChild);
                parser.completedNode(this, nodePos, nextStatePos - nodePos);
                return ALL_OK;
            }
            
            // We may have transitioned to the next (or a later?) state
            while (state < stateMarkers.length && (tokpos - nodePos) >= stateMarkers[state] && stateMarkers[state] != -1) {
                state++;
                nextStatePos = (state < stateMarkers.length) ? stateMarkers[state] + nodePos : -1;
            }
            
            // Do a partial parse and check the result
            int ppr = doPartialParse(pparams, state);
            nextChild = childQueue.peek();
            if (ppr == PP_ENDS_NODE || (ppr == PP_INCOMPLETE
                    && last.getType() != JavaTokenTypes.EOF)) {
                endNodeCleanup(pparams, state);
                int pos = lineColToPos(document, last.getLine(), last.getColumn());
                int newsize = pos - nodePos;
                if (newsize != getSize()) {
                    setSize(newsize);
                    return NODE_SHRUNK;
                }
                return ALL_OK;
            }
            else if (ppr == PP_INCOMPLETE) {
                // Due to check above, we can be sure that last is the EOF token.
                if (parseEnd != nodePos + getSize()) {
                    // Partial parse - we should just re-schedule.
                    pparams.document.scheduleReparse(parseEnd, 1);
                    return ALL_OK;
                }
                // Fall through to EOF condition below
            }
            else if (ppr == PP_EPIC_FAIL) {
                removeOverwrittenChildren(childQueue, nextChild, Integer.MAX_VALUE, listener);
                return REMOVE_NODE;
            }
            else if (ppr == PP_ENDS_STATE || ppr == PP_BEGINS_NEXT_STATE) {
                int pos;
                if (ppr == PP_ENDS_STATE) {
                    pos = lineColToPos(document, last.getEndLine(), last.getEndColumn());
                }
                else {
                    pos = lineColToPos(document, last.getLine(), last.getColumn());
                }
                if (stateMarkers[state] == (pos - nodePos)) {
                    // We transitioned to the existing state border.
                    nextChild = removeOverwrittenChildren(childQueue, nextChild, pos, listener);
                    processChildQueue(nodePos, childQueue, nextChild);
                    parser.completedNode(this, nodePos, pos - nodePos);
                    return ALL_OK;
                }
                stateMarkers[state] = pos - nodePos;
                marksEnd[state] = (ppr == PP_ENDS_STATE);
                state++;
            }
            else if (ppr == PP_REGRESS_STATE) {
                state--;
                ((MoeSyntaxDocument) document).scheduleReparse(stateMarkers[state] + nodePos, 1);
                stateMarkers[state] = -1;
                return ALL_OK;
            }
            else if (ppr == PP_PULL_UP_CHILD) {
                nextChild = childQueue.peek();
                processChildQueue(nodePos, childQueue, nextChild);
                int epos = lineColToPos(document, last.getEndLine(), last.getEndColumn());
                if (nextChild.getPosition() != epos) {
                    nextChild.getNode().getContainingNodeTree().slideStart(epos - nextChild.getPosition());
                    ((MoeSyntaxDocument) document).scheduleReparse(stateMarkers[state] + nodePos, 1);
                }
                parser.completedNode(this, nodePos, epos - nodePos);
                if (! nextChild.getNode().complete) {
                    ((MoeSyntaxDocument) document).scheduleReparse(epos + nextChild.getNode().getSize(), 1);
                }
                return ALL_OK;
            }
            
            LocatableToken nlaToken = parser.getTokenStream().LA(1);
            if (nlaToken == laToken) {
                // We didn't manage to parse anything?
                parser.getTokenStream().nextToken();
                nlaToken = parser.getTokenStream().LA(1);
            }
                        
            if (nlaToken.getType() == JavaTokenTypes.EOF) {
                int epos = lineColToPos(document, nlaToken.getLine(), nlaToken.getColumn());
                nextChild = removeOverwrittenChildren(childQueue, nextChild, epos, listener);
                if (parseEnd < nodePos + getSize()) {
                    // We had limited the parse amount deliberately. Schedule a continuation.
                    ((MoeSyntaxDocument) document).scheduleReparse(parseEnd - 1, 1);
                    parser.completedNode(this, nodePos, getSize());
                    return ALL_OK;
                }
                if (! lastPartialCompleted(parser, last, state)) {
                    // The parsed piece wants more...
                    ParsedNode parentNode = getParentNode();
                    if (parentNode != null && parentNode.growChild(document,
                            new NodeAndPosition<ParsedNode>(this, nodePos, getSize()), listener)) {
                        // Successfully grew... now do some more parsing
                        int rep = reparseNode(document, nodePos, tokpos, listener);
                        return (rep == REMOVE_NODE) ? REMOVE_NODE : NODE_GREW;
                    }
                    else if (nodePos + getSize() < document.getLength()) {
                        // No option but to reparse the parent node.
                        return REMOVE_NODE;
                    }
                }
                parser.completedNode(this, nodePos, getSize());
                return checkEnd(document, nodePos, listener);
            }
            
            laToken = nlaToken;
            ttype = laToken.getType();
        }

        complete = true;
        
        removeOverwrittenChildren(childQueue, nextChild, Integer.MAX_VALUE, listener);
        int tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
        // Did we shrink?
        int newsize = tokpos - nodePos;
        parser.completedNode(this, nodePos, newsize);
        if (newsize < getSize()) {
            setSize(newsize);
            return NODE_SHRUNK;
        }
        
        return ALL_OK;
    }
    
    private void endNodeCleanup(ParseParams params, int state)
    {
        while (++state < stateMarkers.length) {
            stateMarkers[state] = -1;
        }
        NodeAndPosition<ParsedNode> nextChild = params.childQueue.peek();
        removeOverwrittenChildren(params.childQueue, nextChild,
                Integer.MAX_VALUE, params.listener);
        
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
     * If during parsing we reach some point (epos) then we have overwritten any old child nodes
     * which overlap or occur before epos and so we need to remove them properly.
     */
    private NodeAndPosition<ParsedNode> removeOverwrittenChildren(LinkedList<NodeAndPosition<ParsedNode>> childQueue,
            NodeAndPosition<ParsedNode> nextChild, int epos, NodeStructureListener listener)
    {
        while (nextChild != null && epos > nextChild.getPosition()) {
            // Remove nextChild, we've eaten into it.
            childRemoved(nextChild, listener);
            childQueue.removeFirst();
            nextChild = childQueue.peek();
        }
        
        return nextChild;
    }
    
    /**
     * Restore children in the child queue which were removed temporarily, but not actually overwritten during parsing.
     */
    private void processChildQueue(int nodePos, LinkedList<NodeAndPosition<ParsedNode>> childQueue,
            NodeAndPosition<ParsedNode> nextChild)
    {
        while (nextChild != null) {
            insertNode(nextChild.getNode(), nextChild.getPosition() - nodePos, nextChild.getSize());
            childQueue.removeFirst();
            nextChild = childQueue.peek();
        }
    }
    
    /**
     * Convert a line and column number to an absolute position.
     */
    protected static int lineColToPos(Document document, int line, int col)
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
            if (stateMarkers[i] > (insPos - nodePos)
                    || (stateMarkers[i] == (insPos - nodePos) && !marksEnd[i])) {
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
                if (stateMarkers[i] < (delPos - nodePos)
                        || (stateMarkers[i] == (delPos - nodePos) && marksEnd[i])) {
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
        NodeAndPosition<ParsedNode> nap = findNodeAt(end - 1, nodePos);
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
                    ParsedNode parentNode = getParentNode();
                    if (parentNode != null && parentNode.growChild(document,
                            new NodeAndPosition<ParsedNode>(this, nodePos, getSize()), listener)) {
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
    protected boolean growChild(Document document, NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        int mypos = child.getPosition() - child.getNode().getOffsetFromParent();
        
        NodeAndPosition<ParsedNode> nap = child.nextSibling();
        if (nap != null && nap.getPosition() > child.getEnd()) {
            int newsize = nap.getPosition() - child.getPosition();
            child.getNode().setSize(newsize);
            return true;
        }
        
        int myEnd = mypos + getSize();
        if (nap != null) {
            // Next child is pushing up against the one which wants to grow - so we'll
            // have to remove it.
            removeChild(nap, listener);
            child.getNode().setSize(nap.getEnd() - child.getPosition());
            if (myEnd == nap.getEnd() && marksOwnEnd()) {
                complete = false;
            }
            return true;
        }
        
        // The child can soak up anything remaining at the end of this node.
        if (myEnd > child.getEnd()) {
            int newsize = myEnd - child.getPosition();
            child.getNode().resize(newsize);
            if (marksOwnEnd()) {
                complete = false;
            }
            return true;
        }
        
        // Maybe this node can grow, and then its child can also grow.
        ParsedNode parentNode = getParentNode();
        if (parentNode != null && parentNode.growChild(document,
                new NodeAndPosition<ParsedNode>(this, mypos, getSize()), listener)) {
            myEnd = mypos + getSize();
            ((MoeSyntaxDocument) document).scheduleReparse(myEnd, 1);
            complete = false;
            int newsize = myEnd - child.getPosition();
            child.getNode().resize(newsize);
            return true;
        }
        
        return false;
    }
}
