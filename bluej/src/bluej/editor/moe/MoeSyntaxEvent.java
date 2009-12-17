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
package bluej.editor.moe;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Element;

import bluej.parser.nodes.NodeStructureListener;
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
    private List<NodeAndPosition> addedNodes = new LinkedList<NodeAndPosition>();
    private List<NodeAndPosition> removedNodes = new LinkedList<NodeAndPosition>();
    
    public MoeSyntaxEvent(MoeSyntaxDocument document, DocumentEvent srcEvent)
    {
        this.document = document;
        this.srcEvent = srcEvent;
    }
    
    /**
     * Get a list of nodes created due to this edit.
     */
    public List<NodeAndPosition> getAddedNodes()
    {
        return addedNodes;
    }
    
    /**
     * Get a list of nodes removed due to this edit.
     */
    public List<NodeAndPosition> getRemovedNodes()
    {
        return removedNodes;
    }
    
    // -------------- DocumentListener interface ------------------
    
    public ElementChange getChange(Element elem)
    {
        return srcEvent.getChange(elem);
    }
    
    public MoeSyntaxDocument getDocument()
    {
        return document;
    }
    
    public int getLength()
    {
        return srcEvent.getLength();
    }

    public int getOffset()
    {
        return srcEvent.getOffset();
    }
    
    public EventType getType()
    {
        return srcEvent.getType();
    }
    
    // -------------- NodeStructureListener interface ------------------
    
    public void nodeAdded(NodeAndPosition node)
    {
        addedNodes.add(node);
    }
    
    public void nodeRemoved(NodeAndPosition node)
    {
        removedNodes.add(node);
    }
}
