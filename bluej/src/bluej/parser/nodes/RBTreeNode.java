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
public class RBTreeNode
{
    private NodeTree<?> containingNodeTree;

    /**
     * Set the containing node tree. This is normally only called by NodeTree when inserting
     * this node into the tree.
     */
    protected final void setContainingNodeTree(NodeTree<?> cnode)
    {
        containingNodeTree = cnode;
    }
    
    /**
     * Get the containing node tree for this node.
     * @return
     */
    protected final NodeTree<?> getContainingNodeTree()
    {
        return containingNodeTree;
    }
}
