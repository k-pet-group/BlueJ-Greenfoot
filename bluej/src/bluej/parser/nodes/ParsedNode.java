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

import java.util.List;

import javax.swing.text.Document;

import bluej.editor.moe.Token;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public abstract class ParsedNode
{
    public static final int NODETYPE_NONE = 0;
    public static final int NODETYPE_TYPEDEF = 1;
    public static final int NODETYPE_METHODDEF = 2;
    public static final int NODETYPE_ITERATION = 3; // for, while, etc
    public static final int NODETYPE_SELECTION = 4; // if/then, try/catch
    
    /** The NodeTree containing the child nodes of this node */
    private NodeTree nodeTree;
    /** The NodeTree node (belonging to the parent parse node) which contains this node */
    private NodeTree containingNodeTree;
    /** The parent ParsedNode which contains us */
    private ParsedNode parentNode;
    
    private boolean isInner = false;
	
    public ParsedNode()
    {
        nodeTree = new NodeTree();
    }
	
    ParsedNode(ParsedNode parentNode)
    {
        this();
        this.parentNode = parentNode;
    }

    /**
     * Get the type of this node. One of:
     * <ul>
     * <li>NODETYPE_NONE - unspecified
     * <li>NODETYPE_TYPEDEF - a type definition (class, interface etc)
     * <li>NODETYPE_METHODDEF - a method defintion
     * <li>NODETYPE_ITERATION - an iteration construct (for loop etc)
     * <li>NODETYPE_SELECTION - a selection construct (if/else etc)
     * </ul>
     */
    public int getNodeType()
    {
        return NODETYPE_NONE;
    }
    
    /**
     * Get the name of the entity this node represents. For methods, returns the method name.
     * May return null.
     */
    public String getName()
    {
        return null;
    }
    
    public boolean equals(Object obj)
    {
        return obj == this;
    }
    
    public void insertNode(ParsedNode child, int position, int size)
    {
        getNodeTree().insertNode(child, position, size);
    }
    
    public final NodeAndPosition findNodeAtOrAfter(int position, int startpos)
    {
        return nodeTree.findNodeAtOrAfter(position, startpos);
    }
    
    /**
     * Set the size of this node. Following nodes shift position according to the change in
     * size; this should normally be used when inserting or removing text from the node.
     * @param newSize  The new node size
     */
    public void setNodeSize(int newSize)
    {
        getContainingNodeTree().setNodeSize(newSize);
    }
    
    /**
     * Get the offset of this node from its parent node.
     */
    public int getOffsetFromParent()
    {
        return getContainingNodeTree().getPosition();
    }
    
    /**
     * Remove this node from the parent, without disturbing the position of any sibling nodes.
     */
    public void remove()
    {
        getContainingNodeTree().remove();
    }
    
    /**
     * Set the containing node tree. This is normally only called by NodeTree when inserting
     * this node into the tree.
     */
    void setContainingNodeTree(NodeTree cnode)
    {
        containingNodeTree = cnode;
    }
	
    public boolean isContainer()
    {
        return false;
    }
    
    /**
     * Is this an "inner" scope for highlighting purposes
     */
    public boolean isInner()
    {
        return isInner;
    }
    
    /**
     * Set whether this is an inner scope for highlighting purposes
     */
    public void setInner(boolean inner)
    {
        isInner = inner;
    }
	
    public int getLeftmostIndent(Document document, int nodePos, int tabSize)
    {
        return 0;
    }
	
    public void getNodeStack(List<NodeAndPosition> list, int pos, int nodepos)
    {
        list.add(new NodeAndPosition(this, nodepos, getSize()));
        NodeAndPosition subNode = getNodeTree().findNode(pos, nodepos);
        while (subNode != null) {
            list.add(subNode);
            subNode = subNode.getNode().getNodeTree().findNode(pos, subNode.getPosition());
        }
    }

    public int getSize()
    {
        return getContainingNodeTree().getNodeSize();
    }

    /**
     * Insert the given text.
     * 
     * The result can be:
     * - text absorbed, no change to node structure
     * - node terminates earlier (eg ';' or '}' inserted)
     * - subnode created, and this node extended (eg insert '{')
     */
    public abstract void textInserted(Document document, int nodePos, int insPos, int length);
	
    public abstract void textRemoved(Document document, int nodePos, int delPos, int length);

    public abstract Token getMarkTokensFor(int pos, int length, int nodePos, Document document);

    protected ParsedNode getParentNode()
    {
        return parentNode;
    }

    protected NodeTree getNodeTree()
    {
        return nodeTree;
    }

    protected NodeTree getContainingNodeTree()
    {
        return containingNodeTree;
    }

    /**
     * This node is shortened, it no longer needs all the text assigned to it.
     */
    protected void nodeShortened(int newLength) {}

    /**
     * This node has become incomplete (needs to be extended).
     */
    protected void nodeIncomplete() {}

    /**
     * This node should be re-parsed from the specified point.
     */
    protected void reparseNode(Document document, int nodePos, int offset) {}
}
