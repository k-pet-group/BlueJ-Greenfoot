/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Element;

import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

/**
 * A representation of document events in a MoeSyntaxDocuments. As well as textual
 * changes, this can include information about node structure changes.
 * 
 * @author Davin McCall
 */
public class MoeSyntaxEvent implements DocumentEvent, NodeStructureListener
{
    private MoeSyntaxDocument document;
    private DocumentEvent srcEvent;
    private List<NodeAndPosition<ParsedNode>> removedNodes =
        new LinkedList<NodeAndPosition<ParsedNode>>();
    private Map<ParsedNode,NodeChangeRecord> changedNodes =
        new HashMap<ParsedNode,NodeChangeRecord>();
    private EventType eventType;
    
    public MoeSyntaxEvent(MoeSyntaxDocument document, DocumentEvent srcEvent)
    {
        this.document = document;
        this.srcEvent = srcEvent;
        eventType = srcEvent.getType();
    }
    
    public MoeSyntaxEvent(MoeSyntaxDocument document)
    {
        this.document = document;
        eventType = EventType.CHANGE;
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
    
    // -------------- DocumentListener interface ------------------
    
    public ElementChange getChange(Element elem)
    {
        return srcEvent != null ? srcEvent.getChange(elem) : null;
    }
    
    public MoeSyntaxDocument getDocument()
    {
        return document;
    }
    
    public int getLength()
    {
        return srcEvent != null ? srcEvent.getLength() : 0;
    }

    public int getOffset()
    {
        return srcEvent != null ? srcEvent.getOffset() : 0;
    }
    
    public EventType getType()
    {
        return eventType;
    }
    
    // -------------- NodeStructureListener interface ------------------
    
    public void nodeRemoved(NodeAndPosition<ParsedNode> node)
    {
        removedNodes.add(node);
        changedNodes.remove(node.getNode());
    }
    
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
    
    /**
     * Node change record. Purely used for passing data around, hence public fields.
     */
    public class NodeChangeRecord
    {
        public int originalPos;
        public int originalSize;
        public NodeAndPosition<ParsedNode> nap;
    }
}
