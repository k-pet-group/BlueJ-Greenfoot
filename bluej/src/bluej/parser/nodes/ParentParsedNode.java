/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2019  Michael Kolling and John Rosenberg 
 
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

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * An abstract ParsedNode which delegates to child nodes.
 * 
 * @author Davin McCall
 */
public abstract class ParentParsedNode extends ParsedNode
{    
    public ParentParsedNode(ParsedNode myParent)
    {
        super(myParent);
    }
            
    @Override
    public int textInserted(ReparseableDocument document, int nodePos, int insPos,
                            int length, NodeStructureListener listener)
    {
        // grow ourself:
        int newSize = getSize() + length;
        resize(newSize);
        
        NodeAndPosition<ParsedNode> child = getNodeTree().findNodeAtOrAfter(insPos, nodePos);
        if (child != null && (child.getPosition() < insPos
                || child.getPosition() == insPos && child.getNode().growsForward())) {
            ParsedNode cnode = child.getNode();
            // let the child handle the change.
            int r = cnode.textInserted(document, child.getPosition(), insPos, length, listener);
            if (r == NODE_GREW || r == NODE_SHRUNK) {
                newSize = child.getNode().getSize();
                child = new NodeAndPosition<ParsedNode>(cnode, child.getPosition(), newSize);
                childResized(document, nodePos, child);
                //return reparseNode(document, nodePos, child.getPosition() + newSize, listener);
                document.scheduleReparse(child.getPosition() + newSize, 0);
                return ALL_OK;
            }
            else if (r == REMOVE_NODE) {
                removeChild(child, listener);
                //return reparseNode(document, nodePos, child.getPosition(), listener);
                document.scheduleReparse(child.getPosition(), child.getSize());
                return ALL_OK;
            }
            return ALL_OK;
        }
        else {
            // We must handle the insertion ourself
            // Slide any children:
            if (child != null) {
                child.getNode().getContainingNodeTree().slideNode(length);
            }
            //return reparseNode(document, nodePos, insPos, listener);
            return handleInsertion(document, nodePos, insPos, length, listener);
        }
    }
    
    /**
     * Handle the case of text being inserted directly into this node (not a child).
     */
    @OnThread(Tag.FXPlatform)
    protected int handleInsertion(ReparseableDocument document, int nodePos, int insPos, int length,
            NodeStructureListener listener)
    {
        document.scheduleReparse(insPos, length);
        return ALL_OK;
    }
    
    @Override
    public int textRemoved(ReparseableDocument document, int nodePos, int delPos,
            int length, NodeStructureListener listener)
    {
        // shrink ourself:
        int newSize = getSize() - length;
        resize(newSize);
        
        int endPos = delPos + length;
        
        NodeAndPosition<ParsedNode> child = getNodeTree().findNodeAtOrAfter(delPos, nodePos);
        while (child != null && child.getEnd() == delPos) {
            if (! child.getNode().marksOwnEnd()) {
                child.getNode().setComplete(false);
            }
            child = child.nextSibling();
        }
        
        if (child != null && child.getPosition() < delPos) {
            // Remove the end portion (or middle) of the child node
            int childEndPos = child.getEnd();
            if (childEndPos >= endPos) {
                // Remove the middle of the child node
                int r = child.getNode().textRemoved(document, child.getPosition(), delPos, length, listener);
                if (r == REMOVE_NODE) {
                    removeChild(child, listener);
                    document.scheduleReparse(child.getPosition(), child.getSize());
                }
                else if (r != ALL_OK) {
                    newSize = child.getNode().getSize();
                    if (newSize < child.getSize()) {
                        document.scheduleReparse(child.getPosition() + newSize, child.getSize() - newSize);
                    }
                    else {
                        document.scheduleReparse(child.getPosition() + newSize, 0);
                    }
                }
                return ALL_OK;
            }
            
            // Remove the end portion of the child node
            int rlength = childEndPos - delPos; // how much is removed

            // Remove any following nodes as necessary
            NodeAndPosition<ParsedNode> next = child.nextSibling();
            while (next != null && next.getPosition() < endPos) {
                NodeAndPosition<ParsedNode> nnext = next.nextSibling();
                removeChild(next, listener);
                next = nnext;
            }

            if (next != null) {
                // Slide the portion which remains
                next.getNode().slide(rlength - length);
            }
            // Inform the child of the removed text 
            int r = child.getNode().textRemoved(document, child.getPosition(), delPos, rlength, listener);
            int reparseOffset;
            if (r == REMOVE_NODE) {
                reparseOffset = child.getPosition();
                removeChild(child, listener);
            }
            else {
                reparseOffset = child.getPosition() + child.getNode().getSize();
            }

            return handleDeletion(document, nodePos, reparseOffset, listener);
        }
        
        // Any child node that has its beginning removed is just removed.
        while (child != null && child.getPosition() < endPos) {
            NodeAndPosition<ParsedNode> nextChild = child.nextSibling();
            removeChild(child, listener);
            child = nextChild;
        }

        if (child != null) {
            child.slide(-length);
            if (child.getPosition() == delPos && child.getNode().growsForward()) {
                // The child had text immediately preceding it removed, and it
                // grows forward, meaning that the parent probably determines where
                // it starts. If we schedule a re-parse at delPos, the child will
                // get re-parsed instead of the parent - so we just remove the child.
                removeChild(child, listener);
            }
        }
        
        return handleDeletion(document, nodePos, delPos, listener);
    }
    
    /**
     * Handle the case of text being removed directly from this node (rather than a
     * child node).
     */
    @OnThread(Tag.FXPlatform)
    protected int handleDeletion(ReparseableDocument document, int nodePos, int dpos,
            NodeStructureListener listener)
    {
        if (nodePos + getSize() == dpos && marksOwnEnd()) {
            complete = false;
        }
        
        document.scheduleReparse(dpos, 0);
        return ALL_OK;
    }
    
    /*
     * Default implementation, just causes the parent to re-parse
     */
    @Override
    @OnThread(Tag.FXPlatform)
    protected int reparseNode(ReparseableDocument document, int nodePos, int offset, int maxParse, NodeStructureListener listener)
    {
        return REMOVE_NODE;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    protected boolean growChild(ReparseableDocument document, NodeAndPosition<ParsedNode> child,
            NodeStructureListener listener)
    {
        // Without any further knowledge, we're just going to have to do a full reparse.
        // Subclasses should override this to improve performance.
        return false;
    }
    
}
