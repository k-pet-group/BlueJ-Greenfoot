/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014,2017  Michael Kolling and John Rosenberg
 
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
package bluej.editor.moe;

import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedNode;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A representation of document events in a MoeSyntaxDocuments. As well as textual
 * changes, this can include information about node structure changes.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class MoeSyntaxEvent implements NodeStructureListener
{
    private final int offset;
    private final int length;
    private final MoeSyntaxDocument document;
    private final List<NodeAndPosition<ParsedNode>> removedNodes = new ArrayList<>();
    private final Map<ParsedNode, NodeChangeRecord> changedNodes = new HashMap<>();
    private final boolean insert;
    private final boolean remove;

    public MoeSyntaxEvent(MoeSyntaxDocument document, int offset, int length, boolean isInsert, boolean isRemove)
    {
        this.document = document;
        this.offset = offset;
        this.length = length;
        this.insert = isInsert;
        this.remove = isRemove;
    }
    
    /**
     * Get a list of nodes removed as part of this event.
     */
    public List<NodeAndPosition<ParsedNode>> getRemovedNodes()
    {
        return removedNodes;
    }
    
    /**
     * Get a collection of nodes which changed position as part of this event.
     */
    public Collection<NodeChangeRecord> getChangedNodes()
    {
        return changedNodes.values();
    }

    
    // -------------- NodeStructureListener interface ------------------

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void nodeRemoved(NodeAndPosition<ParsedNode> node)
    {
        removedNodes.add(node);
        changedNodes.remove(node.getNode());
    }

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public void nodeChangedLength(NodeAndPosition<ParsedNode> nap, int oldPos,
            int oldSize)
    {
        // We try to optimize a little by storing the original position of any
        // changed node. If the node is then changed back to the original position,
        // we can forget about the change.
        NodeChangeRecord r = changedNodes.get(nap.getNode());
        if (r == null) {
            if (nap.getPosition() != oldPos || nap.getSize() != oldSize) {
                r = new NodeChangeRecord();
                r.nap = nap;
                r.originalPos = oldPos;
                r.originalSize = oldSize;
                changedNodes.put(nap.getNode(), r);
            }
        }
        else {
            if (nap.getPosition() == r.originalPos && nap.getSize() == r.originalSize) {
                changedNodes.remove(nap.getNode());
            }
            else {
                r.nap = nap;
            }
        }
    }

    public int getOffset()
    {
        return offset;
    }

    public int getLength()
    {
        return length;
    }

    public boolean isInsert()
    {
        return insert;
    }

    public boolean isRemove()
    {
        return remove;
    }

    /**
     * Node change record. Purely used for passing data around, hence public fields.
     */
    @OnThread(Tag.Any)
    public class NodeChangeRecord
    {
        public int originalPos;
        public int originalSize;
        public NodeAndPosition<ParsedNode> nap;
    }
}
