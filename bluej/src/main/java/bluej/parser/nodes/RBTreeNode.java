/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009-2010  Michael Kolling and John Rosenberg 

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

/**
 * An RBTreeNode represents the value in a tree node represented by a NodeTree.
 *  
 * @author Davin McCall
 */
public class RBTreeNode<T extends RBTreeNode<T>>
{
    private NodeTree<T> containingNodeTree;

    /**
     * Set the containing node tree. This is normally only called by NodeTree when inserting
     * this node into the tree.
     */
    protected final void setContainingNodeTree(NodeTree<T> cnode)
    {
        containingNodeTree = cnode;
    }
    
    /**
     * Get the containing node tree for this node.
     * @return
     */
    protected final NodeTree<T> getContainingNodeTree()
    {
        return containingNodeTree;
    }
    
    /**
     * Move the node. This also has the effect of moving all following nodes.
     * @param offset  The amount by which to move the node
     */
    public void slide(int amount)
    {
        getContainingNodeTree().slideNode(amount);
    }

    /**
     * Move the node's beginning, but not its end position. This shrinks or grows
     * the node accordingly. The position of following nodes is not affected.
     */
    public void slideStart(int offset)
    {
        getContainingNodeTree().slideStart(offset);
    }
    
    /**
     * Set the size of this node. Following nodes shift position according to the change in
     * size; this should normally be used when inserting or removing text from the node.
     * @param newSize  The new node size
     */
    public void resize(int newSize)
    {
        getContainingNodeTree().resize(newSize);
    }
    
    /**
     * Set the size of this node, without moving following nodes. It is the caller's
     * responsibility to ensure that setting the new size does not cause this node
     * to overlap following nodes.
     * @param newSize  The new size of this node.
     */
    public void setSize(int newSize)
    {
        getContainingNodeTree().setSize(newSize);
    }
    
    public void remove()
    {
        getContainingNodeTree().remove();
    }
}
