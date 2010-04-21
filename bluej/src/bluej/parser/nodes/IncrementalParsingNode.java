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
    /** Node ends at end of the "last" token */
    protected final static int PP_ENDS_NODE_AFTER = 3;
    /** Parse completely failed. The node must be removed and the parent re-parsed. */
    protected final static int PP_EPIC_FAIL = 4;
    /** The "last" token ends the state. The new state begins after it. */
    protected final static int PP_ENDS_STATE = 5;
    /** The "last" token is the beginning of the next state */
    protected final static int PP_BEGINS_NEXT_STATE = 6;
    /** The current state fails, requiring a regression to the previous parse state */
    protected final static int PP_REGRESS_STATE = 7;
    /** Pull the next child up behind the "last" token and continue parsing inside it */
    protected final static int PP_PULL_UP_CHILD = 8;
    /**
     * Abort the parse. This must be safe; either the parse has completed or an
     * appropriate re-parse has been scheduled. params.abortPos must be set.
     */
    protected final static int PP_ABORT = 9;
    
    
    private final static int MAX_PARSE_PIECE = 8000;
    
    
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
        return false;
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
        int originalOffset = offset;
        
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
        NodeAndPosition<ParsedNode> boundaryNap = nap;
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

        // Find the next child node, which we may bump into when we are parsing.
        NodeAndPosition<ParsedNode> nextChild = childQueue.peek();
        
        // Make a reader and parser
        int pline = document.getDefaultRootElement().getElementIndex(offset) + 1;
        int pcol = offset - document.getDefaultRootElement().getElement(pline - 1).getStartOffset() + 1;
        Reader r = new DocumentReader(document, offset, parseEnd);
        EditorParser parser = new EditorParser(document, r, pline, pcol, buildScopeStack());
                
        LocatableToken laToken = parser.getTokenStream().LA(1);
        int tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
        nap = boundaryNap;
        
        boolean extendPrev = false;
        if (nap != null) {
            if (! nap.getNode().complete) {
                // Two cases:
                //
                // 1. the node marks its own end. In this case the node might
                // not have extended as far as possible - consider if{} without an else.
                // The else clause must attach to the if{} block if it is inserted later.
                // (Similarly for try/catch/catch/finally).
                //
                // 2. the node end is marked by something in the parent node (i.e this node).
                // in that case the node should be extended only the node end matches the
                // original reparse offset (originalOffset).
                //
                // At the moment we can't tell which of the two cases, so we always extend.
                extendPrev = true;
            }
        }
        
        if (extendPrev) {
            int tokend = lineColToPos(document, laToken.getEndLine(), laToken.getEndColumn());
            // The first token we read "joins on" to the end of the incomplete previous node.
            // So, we'll attempt to add it in.
            nextChild = removeOverwrittenChildren(childQueue, tokend, listener);
            int oldSize = nap.getSize(); // record original size
            nap.setSize(tokend - nap.getPosition()); // append the token
            int pr = nap.getNode().reparseNode(document, nap.getPosition(), tokpos, listener);
            if (pr == REMOVE_NODE) {
                removeChild(nap, listener);
                // Schedule from the original offset, as we may get stuck in a loop otherwise
                // (Because the piecemeal parse amount is less than the original node size).
                ((MoeSyntaxDocument)document).scheduleReparse(originalOffset,
                        tokend - originalOffset);
                return ALL_OK;
            }
            else {
                if (nap.getNode().getSize() != oldSize) {
                    if (! nap.getNode().complete) {
                        // just reschedule
                        //((MoeSyntaxDocument)document).scheduleReparse(originalOffset, 0);
                        return ALL_OK;
                    }
                    offset = nap.getEnd();
                    pline = document.getDefaultRootElement().getElementIndex(offset) + 1;
                    pcol = offset - document.getDefaultRootElement().getElement(pline - 1).getStartOffset() + 1;
                    r = new DocumentReader(document, offset, parseEnd);
                    parser = new EditorParser(document, r, pline, pcol, buildScopeStack());
                    laToken = parser.getTokenStream().LA(1);
                    tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
                }
            }
        }
                
        int ttype = laToken.getType();
        
        int nextStatePos = (state < stateMarkers.length) ? stateMarkers[state] : -1;
        nextStatePos += (nextStatePos == -1) ? 0 : nodePos;
        
        ParseParams pparams = new ParseParams();
        pparams.listener = listener;
        pparams.parser = parser;
        pparams.tokenStream = parser.getTokenStream();
        pparams.document = (MoeSyntaxDocument) document;
        pparams.nodePos = nodePos;
        pparams.childQueue = childQueue;
        
        while (! isNodeEndMarker(ttype)) {
            
            if (nextChild != null && nextChild.getPosition() <= tokpos) {
                if (isDelimitingNode(nextChild)) {
                    processChildQueue(nodePos, childQueue, nextChild);
                    parser.completedNode(this, nodePos, nextChild.getPosition() - nodePos);
                    pparams.document.markSectionParsed(offset, nextChild.getPosition() - offset);
                    return ALL_OK; // we're done!
                }
            }
                        
            // Do a partial parse and check the result
            int ppr = doPartialParse(pparams, state);
            nextChild = childQueue.peek();
            if (ppr == PP_ENDS_NODE || ppr == PP_ENDS_NODE_AFTER || (ppr == PP_INCOMPLETE
                    && last.getType() != JavaTokenTypes.EOF)) {
                complete = (ppr != PP_INCOMPLETE);
                int pos;
                if (ppr == PP_ENDS_NODE_AFTER) {
                    pos = lineColToPos(document, last.getEndLine(), last.getEndColumn());
                }
                else {
                    pos = lineColToPos(document, last.getLine(), last.getColumn());
                }
                pparams.document.markSectionParsed(offset, pos - offset);
                int newsize = pos - nodePos;
                if (newsize != getSize()) {
                    setSize(newsize);
                    endNodeCleanup(pparams, state, Integer.MAX_VALUE, nodePos + newsize);
                    return NODE_SHRUNK;
                }
                endNodeCleanup(pparams, state, Integer.MAX_VALUE, nodePos + newsize);
                return ALL_OK;
            }
            else if (ppr == PP_INCOMPLETE) {
                // Due to check above, we can be sure that last is the EOF token.
                if (parseEnd != nodePos + getSize()) {
                    // Partial parse - we should just re-schedule.
                    pparams.document.scheduleReparse(parseEnd, 0);
                    pparams.document.markSectionParsed(offset, parseEnd - offset);
                    return ALL_OK;
                }
                complete = false;
                // Fall through to EOF condition below
            }
            else if (ppr == PP_EPIC_FAIL) {
                removeOverwrittenChildren(childQueue, Integer.MAX_VALUE, listener);
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
                    nextChild = removeOverwrittenChildren(childQueue, pos, listener);
                    processChildQueue(nodePos, childQueue, nextChild);
                    parser.completedNode(this, nodePos, pos - nodePos);
                    pparams.document.markSectionParsed(offset, pos - offset);
                    return ALL_OK;
                }
                stateMarkers[state] = pos - nodePos;
                marksEnd[state] = (ppr == PP_ENDS_STATE);
                state++;
                nextStatePos = (state < stateMarkers.length) ? stateMarkers[state] : -1;
            }
            else if (ppr == PP_REGRESS_STATE) {
                state--;
                int rppos = stateMarkers[state] + nodePos;
                pparams.document.scheduleReparse(rppos, Math.max(offset - rppos, 0));
                stateMarkers[state] = -1;
                int epos = lineColToPos(document, last.getLine(), last.getColumn());
                removeOverwrittenChildren(childQueue, epos, listener);
                processChildQueue(nodePos, childQueue, nextChild);
                return ALL_OK;
            }
            else if (ppr == PP_PULL_UP_CHILD) {
                nextChild = childQueue.peek();
                processChildQueue(nodePos, childQueue, nextChild);
                int epos = lineColToPos(document, last.getEndLine(), last.getEndColumn());
                if (nextChild.getPosition() != epos) {
                    int slideAmount = nextChild.getPosition() - epos;
                    if (slideAmount < 0) {
                        throw new NullPointerException(); // DAV
                    }
                    nextChild.getNode().getContainingNodeTree().slideStart(-slideAmount);
                    pparams.document.scheduleReparse(stateMarkers[state] + nodePos, slideAmount);
                }
                parser.completedNode(this, nodePos, epos - nodePos);
                pparams.document.markSectionParsed(offset, epos - offset);
                return ALL_OK;
            }
            else if (ppr == PP_ABORT) {
                nextChild = removeOverwrittenChildren(childQueue, pparams.abortPos, listener);
                processChildQueue(nodePos, childQueue, nextChild);
                parser.completedNode(this, nodePos, pparams.abortPos - nodePos);
                pparams.document.markSectionParsed(offset, pparams.abortPos - offset);
                return ALL_OK;
            }
            
            LocatableToken nlaToken = parser.getTokenStream().LA(1);
            if (nlaToken == laToken) {
                // We didn't manage to parse anything?
                parser.getTokenStream().nextToken();
                nlaToken = parser.getTokenStream().LA(1);
            }
            
            // If we've overwritten state markers / old children, invalidate them.
            int nlaPos = lineColToPos(document, nlaToken.getLine(), nlaToken.getColumn());
            for (int i = state; i < stateMarkers.length; i++) {
                if (stateMarkers[i] + nodePos < nlaPos) {
                    stateMarkers[i] = -1;
                }
            }
                        
            if (nlaToken.getType() == JavaTokenTypes.EOF) {
                endNodeCleanup(pparams, state, parseEnd, parseEnd);
                if (parseEnd < nodePos + getSize()) {
                    // We had limited the parse amount deliberately. Schedule a continuation.
                    parser.completedNode(this, nodePos, getSize());
                    pparams.document.markSectionParsed(offset, parseEnd - offset);
                    pparams.document.scheduleReparse(parseEnd, 0);
                    return ALL_OK;
                }
                if (! complete) {
                    // The parsed piece wants more...
                    ParsedNode parentNode = getParentNode();
                    if (parentNode != null && parentNode.growChild(document,
                            new NodeAndPosition<ParsedNode>(this, nodePos, getSize()), listener)) {
                        // Successfully grew... now do some more parsing
                        pparams.document.markSectionParsed(offset, parseEnd - offset);
                        pparams.document.scheduleReparse(parseEnd, nodePos + getSize() - parseEnd);
                        return NODE_GREW;
                    }
                    else if (nodePos + getSize() < document.getLength()) {
                        // No option but to reparse the parent node.
                        return REMOVE_NODE;
                    }
                }
                pparams.document.markSectionParsed(offset, parseEnd - offset);
                return checkEnd(document, nodePos, listener);
            }
            
            laToken = nlaToken;
            ttype = laToken.getType();
            tokpos = nlaPos;
        }

        complete = true;
        
        removeOverwrittenChildren(childQueue, Integer.MAX_VALUE, listener);
        tokpos = lineColToPos(document, laToken.getLine(), laToken.getColumn());
        pparams.document.markSectionParsed(offset, tokpos - offset);
        // Did we shrink?
        int newsize = tokpos - nodePos;
        parser.completedNode(this, nodePos, newsize);
        if (newsize < getSize()) {
            setSize(newsize);
            return NODE_SHRUNK;
        }
        
        return ALL_OK;
    }
    
    /**
     * Check whether the next token is the boundary (beginning) of a delimiting node, in which
     * case we may be able to finish re-parsing. Returns true if a boundary has been reached;
     * in this case params.abortpos will be set appropriately.
     * This is intended as a utility for use by subclasses.
     */
    protected boolean checkBoundary(ParseParams params, LocatableToken token)
    {
        int lpos = lineColToPos(params.document, token.getLine(), token.getColumn());
        LocatableToken hidden = token.getHiddenBefore();
        int hpos = hidden != null ? lineColToPos(params.document, hidden.getLine(), hidden.getColumn()) : lpos;
        NodeAndPosition<ParsedNode> nextChild = params.childQueue.peek();
        while (nextChild != null) {
            if (isDelimitingNode(nextChild) && (nextChild.getPosition() == lpos
                    || nextChild.getPosition() == hpos)) {
                params.abortPos = nextChild.getPosition();
                return true;
            }
            if (nextChild.getPosition() > lpos) {
                break;
            }
            childRemoved(nextChild, params.listener);
            params.childQueue.poll();
            nextChild = params.childQueue.peek();
        }
        return false;
    }
    
    private void endNodeCleanup(ParseParams params, int state, int rpos, int epos)
    {
        while (++state < stateMarkers.length) {
            if (stateMarkers[state] < rpos) {
                stateMarkers[state] = -1;
            }
        }
        removeOverwrittenChildren(params.childQueue, rpos,
                params.listener);
        params.parser.completedNode(this, params.nodePos, epos - params.nodePos);
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
            int epos, NodeStructureListener listener)
    {
        NodeAndPosition<ParsedNode> nextChild = childQueue.peek();
        while (nextChild != null && (epos > nextChild.getPosition() || epos >= nextChild.getEnd())) {
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
            if (stateMarkers[i] > (delPos - nodePos) || (marksEnd[i] && stateMarkers[i] == (delPos - nodePos))) {
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
                        int pr = reparseNode(document, nodePos, offset, listener);
                        return pr == ALL_OK ? NODE_GREW : pr;
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
            child.setSize(newsize);
            childResized((MoeSyntaxDocument)document, mypos, child);
            return true;
        }
        
        int myEnd = mypos + getSize();
        if (nap != null) {
            // Next child is pushing up against the one which wants to grow - so we'll
            // have to remove it.
            removeChild(nap, listener);
            child.setSize(nap.getEnd() - child.getPosition());
            if (myEnd == nap.getEnd() && marksOwnEnd()) {
                complete = false;
            }
            childResized((MoeSyntaxDocument)document, mypos, child);
            return true;
        }
        
        // The child can soak up anything remaining at the end of this node.
        if (myEnd > child.getEnd()) {
            int newsize = myEnd - child.getPosition();
            child.resize(newsize);
            if (marksOwnEnd()) {
                complete = false;
            }
            childResized((MoeSyntaxDocument)document, mypos, child);
            return true;
        }
        
        // Maybe this node can grow, and then its child can also grow.
        ParsedNode parentNode = getParentNode();
        if (parentNode != null && parentNode.growChild(document,
                new NodeAndPosition<ParsedNode>(this, mypos, getSize()), listener)) {
            myEnd = mypos + getSize();
            ((MoeSyntaxDocument) document).scheduleReparse(myEnd, 0);
            complete = false;
            int newsize = myEnd - child.getPosition();
            child.resize(newsize);
            childResized((MoeSyntaxDocument)document, mypos, child);
            return true;
        }
        
        return false;
    }
}
