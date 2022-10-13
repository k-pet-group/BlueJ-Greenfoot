/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2017  Michael Kolling and John Rosenberg
 
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

import java.util.Iterator;


/**
 * Represents a set of ParsedNode using a (red/black) tree structure.
 *
 * @author davmac
 */
public class NodeTree<T extends RBTreeNode<T>>
{
    // The following two instance variables specify the position of the contained ParsedNode,
    // relative to the position that this NodeTree represents.
    private int pnodeOffset; // offset of the ParsedNode in this tree node from the node start
    private int pnodeSize; // size of the ParsedNode in this tree node

    private NodeTree<T> parent;

    private NodeTree<T> left;
    private T pnode;
    private NodeTree<T> right; // offset pnodeOffset + pnodeSize

    private boolean black;  // true = black, false = red

    /**
     * Construct an empty node tree.
     */
    public NodeTree()
    {
        black = true;
    }

    /**
     * Get an iterator which iterates through nodes in the tree, in order
     * 
     * @param offset  The position of node containing the node tree
     */
    public Iterator<NodeAndPosition<T>> iterator(int offset)
    {
        return new NodeTreeIterator<T>(offset, this);
    }

    /**
     * Find the ParsedNode leaf corresponding to a certain position within the parent.
     * Returns null if no leaf contains exactly the given position.
     * 
     * <p>Note that the leaf range may intersect the position parameter at any point -
     * start, end, or anywhere in between. The "leftmost" match will always be
     * returned i.e. if the position falls exactly between two leaves, the leftmost
     * will be found.
     */
    public NodeAndPosition<T> findNode(int pos)
    {
        return findNode(pos, 0);
    }

    public NodeAndPosition<T> findNode(int pos, int startpos)
    {
        if (pnode == null) {
            return null; // empty node tree
        }

        if (startpos + pnodeOffset >= pos) {
            NodeAndPosition<T> r = null;
            if (left != null) {
                r = left.findNode(pos, startpos);
            }
            if (r == null && startpos + pnodeOffset == pos) {
                // Corner case: we match exactly the starting position.
                // We favour the leftmost branch in this case, so as
                // to always return the leftmost matching node.
                r = new NodeAndPosition<T>(pnode, startpos + pnodeOffset, pnodeSize);
            }
            return r;
        }

        if (startpos + pnodeSize + pnodeOffset >= pos) {
            return new NodeAndPosition<T>(pnode, startpos + pnodeOffset, pnodeSize);
        }

        if (right != null) {
            return right.findNode(pos, startpos + pnodeOffset + pnodeSize);
        }

        return null;
    }

    /**
     * Find the (rightmost) node which occurs at or before the given position.
     */
    public NodeAndPosition<T> findNodeAtOrBefore(int pos)
    {
        return findNodeAtOrBefore(pos, 0);
    }

    /**
     * Find the (rightmost) node at or before the given position (when this node's own position
     * is given by {@code startpos}).
     */
    public NodeAndPosition<T> findNodeAtOrBefore(int pos, int startpos)
    {
        if (pnode == null) {
            return null; // empty node tree
        }

        if (startpos + pnodeOffset > pos) {
            if (left != null) {
                return left.findNodeAtOrBefore(pos, startpos);
            }
            else {
                return null;
            }
        }

        if (startpos + pnodeSize + pnodeOffset > pos) {
            // This node straddles the requested position, so there can't be any other node to our
            // right which is still at-or-before the position.
            return new NodeAndPosition<T>(pnode, startpos + pnodeOffset, pnodeSize);
        }

        NodeAndPosition<T> rval = null;
        if (right != null) {
            //pos -= (pnodeOffset + pnodeSize);
            rval = right.findNodeAtOrBefore(pos, startpos + pnodeOffset + pnodeSize);
        }

        if (rval == null) {
            rval = new NodeAndPosition<T>(pnode, startpos + pnodeOffset, pnodeSize);
        }

        return rval;
    }

    /**
     * Find a node ending at or after the given position.
     * 
     * @param pos       The position to start the search from
     */
    public NodeAndPosition<T> findNodeAtOrAfter(int pos)
    {
        return findNodeAtOrAfter(pos, 0);
    }

    /**
     * Find a node ending at or after the given position, accounting for this node representing
     * a specified start position
     * .
     * @param pos       The position to start the search from
     * @param startpos  The offset to assume the tree represents
     */
    public NodeAndPosition<T> findNodeAtOrAfter(int pos, int startpos)
    {
        if (pnode == null) {
            return null; // empty node tree
        }

        if (startpos + pnodeOffset >= pos) {
            if (left != null) {
                NodeAndPosition<T> rval = left.findNodeAtOrAfter(pos, startpos);
                if (rval != null) {
                    return rval;
                }
            }
        }

        if (startpos + pnodeSize + pnodeOffset >= pos) {
            return new NodeAndPosition<T>(pnode, startpos + pnodeOffset, pnodeSize);
        }

        NodeAndPosition<T> rval = null;
        if (right != null) {
            rval = right.findNodeAtOrAfter(pos, startpos + pnodeOffset + pnodeSize);
        }
        return rval;
    }

    /**
     * Set the size of the contained ParsedNode. This is to be used in cases where the
     * node has shrunk or grown because of text being removed or inserted, not for cases
     * when the node is taking on more (or less) text from the document (see also setSize()).
     */
    public void resize(int newSize)
    {
        int delta = newSize - pnodeSize;
        pnodeSize = newSize;
        NodeTree<T> nt = this;
        while (nt.parent != null) {
            if (nt.parent.left == nt) {
                nt.parent.pnodeOffset += delta;
            }
            nt = nt.parent;
        }
    }
    
    /**
     * Set the size of the contained ParsedNode, without moving nodes to the right of it.
     * See also resize().
     */
    public void setSize(int newSize)
    {
        int delta = newSize - pnodeSize;
        pnodeSize = newSize;
        NodeTree<T> nt = this.right;
        while (nt != null) {
            nt.pnodeOffset -= delta;
            nt = nt.left;
        }
    }

    /**
     * Move the node. This also has the effect of moving all following nodes.
     * @param offset  The amount by which to move the node
     */
    public void slideNode(int offset)
    {
        pnodeOffset += offset;
        NodeTree<T> nt = this;
        while (nt.parent != null) {
            if (nt.parent.left == nt) {
                nt.parent.pnodeOffset += offset;
            }
            nt = nt.parent;
        }
    }
    
    /**
     * Move the node's beginning, but not its end position. This shrinks or grows
     * the node accordingly. The position of any subsequent node is not affected.
     */
    public void slideStart(int offset)
    {
        pnodeOffset += offset;
        pnodeSize -= offset;
    }

    /**
     * Get the size of the contained ParsedNode.
     */
    public int getNodeSize()
    {
        return pnodeSize;
    }

    /**
     * Insert a node into the tree, without affecting the position of other nodes.
     */
    public void insertNode(T newNode, int pos, int size)
    {
        if (pnode == null) {
            pnode = newNode;
            pnode.setContainingNodeTree(this);
            pnodeOffset = pos;
            pnodeSize = size;
            size = pos + size;
        }
        else {
            if (pos < pnodeOffset) {
                assert(pos + size <= pnodeOffset);
                if (left == null) {
                    left = new NodeTree<T>(this, newNode, pos, size);
                    fixupNewNode(left);
                }
                else {
                    left.insertNode(newNode, pos, size);
                }
            }
            else {
                assert(pnodeOffset + pnodeSize <= pos);
                pos -= (pnodeOffset + pnodeSize);
                if (right == null) {
                    right = new NodeTree<T>(this, newNode, pos, size);
                    fixupNewNode(right);
                }
                else {
                    right.insertNode(newNode, pos, size);
                }
            }
        }
    }

    /**
     * Remove this node from the tree. Position of following nodes is preserved.
     */
    public void remove()
    {
        if (left == null || right == null) {
            one_child_remove();
        }
        else {
            NodeTree<T> sub = left;
            int nmoffset = 0;
            while (sub.right != null) {
                nmoffset += (sub.pnodeOffset + sub.pnodeSize);
                sub = sub.right;
            }
            swapNodeData(this, sub);

            pnodeOffset += nmoffset;
            int rchange = (sub.pnodeOffset + sub.pnodeSize) - (pnodeOffset + pnodeSize);
            right.adjustLeftOffsets(rchange);

            sub.one_child_remove();
        }
    }

    /**
     * Get the relative position of the contained parsed node to the parent node (root of the
     * NodeTree).
     */
    public int getPosition()
    {
        int pos = pnodeOffset;

        NodeTree<T> parent = this.parent;
        NodeTree<T> current = this;

        while (parent != null) {
            if (current == parent.right) {
                pos += parent.pnodeOffset + parent.pnodeSize;
            }
            current = parent;
            parent = current.parent;
        }

        return pos;
    }

    private void adjustLeftOffsets(int amount)
    {
        NodeTree<T> nt = this;
        while (nt != null) {
            nt.pnodeOffset += amount;
            nt = nt.left;
        }
    }

    /**
     * Re-structure the tree so that one node (with) takes the place of another (dest).
     * The "dest" node (and any of its subtrees, except "with") will then no longer be part
     * of the tree.
     */
    private static <T extends RBTreeNode<T>> void replace_node(NodeTree<T> dest, NodeTree<T> with)
    {
        if (dest.parent != null) {
            if (dest.parent.left == dest) {
                dest.parent.left = with;
            }
            else {
                dest.parent.right = with;
            }
        }
        if (with != null) {
            with.parent = dest.parent;
        }
    }

    /**
     * Remove, in the special case that one or both children are null.
     */
    private void one_child_remove()
    {
        if (left == null && right == null) {
            pnode = null;
            if (parent != null) {
                if (black) {
                    delete_case_1();
                }
                // If we're not black, we are red. So removing us doesn't change
                // the number of black nodes in any path from the parent.
                if (parent.left == this) {
                    parent.left = null;
                }
                else {
                    parent.right = null;
                }
            }
        }
        else {
            // We must be black. The child must be red.
            if (parent == null) {
                // Special case - mustn't move the root.
                if (left == null) {
                    int offset = pnodeOffset + pnodeSize;
                    swapNodeData(this, right);
                    pnodeOffset += offset;
                    right = null;
                }
                else {
                    swapNodeData(this, left);
                    left = null;
                }
                black = true;
            }
            else if (left == null) {
                int offset = pnodeOffset + pnodeSize;
                replace_node(this, right);
                right.adjustLeftOffsets(offset);
                right.black = true;
            }
            else {
                replace_node(this, left);
                left.black = true;
            }
        }
    }

    private NodeTree<T> getSibling()
    {
        if (parent != null) {
            if (parent.left == this) {
                return parent.right;
            }
            else {
                return parent.left;
            }
        }
        else {
            return null;
        }
    }

    /**
     * A black node was deleted. We need to add a black node to this path
     * (or remove one from all other paths).
     */
    private void delete_case_1()
    {
        // If we get here, the current node is black (which will not change).
        // We may or may not have any children; regardless, we must have a sibling.
        if (parent != null) {
            // delete case 2
            NodeTree<T> sibling = getSibling();
            if (! sibling.black) {
                // we are black, so our red sibling must have children...
                parent.black = false;
                sibling.black = true;
                if (this == parent.left) {
                    rotateLeft(parent);
                }
                else {
                    rotateRight(parent);
                }
                // ... one of which is our new sibling.
            }
            delete_case_3();
        }
    }

    private void delete_case_3()
    {
        // To get here, we must have both a parent and a sibling.
        NodeTree<T> sibling = getSibling();
        if (parent.black && sibling.black && isBlack(sibling.left) && isBlack(sibling.right)) {
            // That's a lot of black.
            sibling.black = false; // remove a black node from sibling path...
            parent.delete_case_1(); // and continue up the tree.
        }
        else {
            // delete case 4
            if (! parent.black && sibling.black && isBlack(sibling.left) && isBlack(sibling.right)) {
                sibling.black = false;
                parent.black = true;
            }
            else {
                // delete case 5
                if (isBlack(sibling)) {
                    if (this == parent.left && isBlack(sibling.right) && !isBlack(sibling.left)) {
                        sibling.black = false;
                        sibling.left.black = true;
                        rotateRight(sibling);
                    }
                    else if (this == parent.right && isBlack(sibling.left) && !isBlack(sibling.right)) {
                        sibling.black = false;
                        sibling.right.black = true;
                        rotateLeft(sibling);
                    }
                }

                // delete case 6
                sibling.black = parent.black;
                parent.black = true;
                if (this == parent.left) {
                    sibling.right.black = true;
                    rotateLeft(parent);
                }
                else {
                    sibling.left.black = true;
                    rotateRight(parent);
                }
            }
        }
    }

    /**
     * Clear the tree - remove all nodes
     */
    public void clear()
    {
        left = null;
        pnode = null;
        right = null;
    }

    /**
     * This node has been inserted into the tree. Fix up the tree to maintain balance.
     */
    private static <T extends RBTreeNode<T>> void fixupNewNode(NodeTree<T> n)
    {
        if (n.parent == null) {
            n.black = true;
            return;
        }

        if (n.parent.isBlack()) {
            return; // ok - we are balanced
        }

        // We know from here on the parent is red.

        NodeTree<T> grandparent = n.getGrandparent(); // cannot be null (root is always black).
        NodeTree<T> uncle = n.getUncle();
        if (! isBlack(uncle)) {
            uncle.black = true;
            n.parent.black = true;
            grandparent.black = false;
            fixupNewNode(grandparent);
            return;
        }

        NodeTree<T> parent = n.parent;
        if (n == parent.right && parent == grandparent.left) {
            rotateLeft(parent);
            //grandparent = parent;
            //parent = n;
            //n = n.left;
        }
        else if (n == parent.left && parent == grandparent.right) {
            rotateRight(n.parent);
            //grandparent = parent;
            //parent = n;
            //n = n.right;
        }

        parent.black = true;
        grandparent.black = false;
        if (n == parent.left && parent == grandparent.left) {
            rotateRight(grandparent);
        }
        else {
            rotateLeft(grandparent);
        }
    }
    
    /**
     * Swap the data of two nodes. This doesn't correctly adjust the
     * pnode offset in either node.
     * 
     * @param n  The first node
     * @param m  The second node
     */
    private static <T extends RBTreeNode<T>> void swapNodeData(NodeTree<T> n, NodeTree<T> m)
    {
        T pn = n.pnode;
        int offset = n.pnodeOffset;
        int size = n.pnodeSize;

        n.pnode = m.pnode;
        n.pnodeOffset = m.pnodeOffset;
        n.pnodeSize = m.pnodeSize;

        m.pnode = pn;
        m.pnodeOffset = offset;
        m.pnodeSize = size;

        if (n.pnode != null) {
            n.pnode.setContainingNodeTree(n);
        }

        if (m.pnode != null) {
            m.pnode.setContainingNodeTree(m);
        }
    }

    private static <T extends RBTreeNode<T>> void rotateLeft(NodeTree<T> n)
    {
        // Right child of n becomes n's parent
        // We swap the data to avoid actually moving node n.
        swapNodeData(n, n.right);
        boolean nblack = n.black;
        n.black = n.right.black;
        n.right.black = nblack;

        n.pnodeOffset += n.right.pnodeOffset + n.right.pnodeSize;
        
        if (n.left == null) {
            // A simple case.
            //assert(n.right.left == null);
            n.left = n.right;
            n.right = n.left.right;
            if (n.right != null) {
                n.right.parent = n;
            }
            n.left.right = null;
            return;
        }
        
        NodeTree<T> oldLeft = n.left;
        n.left = n.right;
        n.right = n.left.right;
        if (n.right != null) {
            n.right.parent = n;
        }
        n.left.right = n.left.left;
        n.left.left = oldLeft;
        if (oldLeft != null) {
            oldLeft.parent = n.left;
        }
    }
    
    private static <T extends RBTreeNode<T>> void rotateRight(NodeTree<T> n)
    {
        // Left child of n becomes n's parent
        // We swap the data to avoid actually moving node n.
        swapNodeData(n, n.left);
        boolean nblack = n.black;
        n.black = n.left.black;
        n.left.black = nblack;

        if (n.right == null) {
            // A simple case.
            //assert(n.left.right == null);
            n.right = n.left;
            n.left = n.right.left;
            if (n.left != null) {
                n.left.parent = n;
            }
            n.right.left = null;
            n.right.pnodeOffset -= (n.pnodeOffset + n.pnodeSize);
            return;
        }
                
        NodeTree<T> oldRight = n.right;
        n.right = n.left;
        n.left = n.right.left;
        if (n.left != null) {
            n.left.parent = n;
        }
        n.right.left = n.right.right;
        n.right.right = oldRight;
        if (oldRight != null) {
            oldRight.parent = n.right;
        }
        
        n.right.pnodeOffset -= (n.pnodeOffset + n.pnodeSize);
    }
    
    private NodeTree<T> getGrandparent()
    {
        if (parent != null) {
            return parent.parent;
        }
        else {
            return null;
        }
    }

    private NodeTree<T> getUncle()
    {
        return parent.getSibling();
    }

    private NodeTree(NodeTree<T> parent, T node, int offset, int size)
    {
        this.parent = parent;
        pnode = node;
        pnode.setContainingNodeTree(this);
        this.pnodeSize = size;
        this.pnodeOffset = offset;
        black = false; // initial colour is red
    }

    private boolean isBlack()
    {
        return black;
    }

    private static boolean isBlack(NodeTree<?> n)
    {
        return n == null || n.black;
    }

    /**
     * A class to represent a [node, position] tuple.
     */
    public static class NodeAndPosition<T extends RBTreeNode<T>>
    {
        private T parsedNode;
        private int position;
        private int size;

        public NodeAndPosition(T pn, int position, int size)
        {
            this.parsedNode = pn;
            this.position = position;
            this.size = size;
        }

        public T getNode()
        {
            return parsedNode;
        }

        public int getPosition()
        {
            return position;
        }

        public int getSize()
        {
            return size;
        }
        
        public int getEnd()
        {
            return position + size;
        }
        
        /**
         * Find the next sibling node - that is, the sibling that occurs closest after this one in
         * terms of position. If the node tree is manipulated only via methods on this object then
         * a call to nextSibling() is valid, otherwise the return is undefined,
         * 
         * @return  The next sibling, or null if there is no next sibling.
         */
        public NodeAndPosition<T> nextSibling()
        {
            NodeTree<T> nt = parsedNode.getContainingNodeTree();

            if (nt == null) // If we are top-level, no siblings:
                return null;

            if (nt.right != null) {
                // go right and then as far left as possible
                int offs = position + nt.pnodeSize;
                nt = nt.right;
                while (nt.left != null) {
                    nt = nt.left;
                }
                return new NodeAndPosition<T>(nt.pnode, offs + nt.pnodeOffset, nt.pnodeSize);
            }
            
            // Otherwise go up until we have gone up to the right
            int offs = position - nt.pnodeOffset;
            while (nt.parent != null) {
                if (nt.parent.left == nt) {
                    nt = nt.parent;
                    return new NodeAndPosition<T>(nt.pnode, offs + nt.pnodeOffset, nt.pnodeSize);
                }
                nt = nt.parent;
                offs -= (nt.pnodeOffset + nt.pnodeSize); 
            }
            
            return null; // no prior node
        }
        
        /**
         * Find the previous sibling node - that is, the sibling that occurs closest before this one in
         * terms of position. If the node tree is manipulated only via methods on this object then
         * a call to prevSibling() is valid, otherwise the return is undefined,
         * 
         * @return  The previous sibling, or null if there is no next sibling.
         */
        public NodeAndPosition<T> prevSibling()
        {
            NodeTree<T> nt = parsedNode.getContainingNodeTree();
            if (nt.left != null) {
                // go left and then as far right as possible
                int offs = position - nt.pnodeOffset;
                nt = nt.left;
                while (nt.right != null) {
                    offs += nt.pnodeOffset + nt.pnodeSize;
                    nt = nt.right;
                }
                return new NodeAndPosition<T>(nt.pnode, offs + nt.pnodeOffset, nt.pnodeSize);
            }
            
            // Otherwise go up until we have gone up to the left
            while (nt.parent != null) {
                int offs = position - nt.pnodeOffset;
                if (nt.parent.right == nt) {
                    nt = nt.parent;
                    return new NodeAndPosition<T>(nt.pnode, offs - nt.pnodeSize, nt.pnodeSize);
                }
                nt = nt.parent;
            }
            
            return null; // no prior node
        }
        
        /**
         * Slide the node and all following nodes by the given amount.
         */
        public void slide(int amount)
        {
            getNode().slide(amount);
            position += amount;
        }
        
        /**
         * Slide the start of the node by the given amount, but leave its end in place.
         */
        public void slideStart(int amount)
        {
            getNode().slideStart(amount);
            position += amount;
            size -= amount;
        }
        
        /**
         * Resize the node. Any following nodes will move accordingly.
         */
        public void resize(int newSize)
        {
            getNode().resize(newSize);
            size = newSize;
        }
        
        /**
         * Set the size of the contained node, without moving following nodes. It is the
         * caller's responsibility to ensure that setting the new size does not cause the
         * node to overlap following nodes.
         * @param newSize  The new size of the node.
         */
        public void setSize(int newSize)
        {
            getNode().setSize(newSize);
            size = newSize;
        }
        
        /**
         * Set the size as recorded in the NodeAndPosition object, without
         * affecting the relative node.
         */
        public void setNapSize(int newSize)
        {
            size = newSize;
        }
    }
    
    /**
     * An iterator through a node tree.
     */
    private static class NodeTreeIterator<T extends RBTreeNode<T>> implements Iterator<NodeAndPosition<T>>
    {
        //Stack<NodeTree> stack;
        int pos = 0; // 0 - left, 1 = middle, 2 = right
        int offset = 0;
        NodeTree<T> current = null;

        /**
         * Construct a new NodeTreeIterator over the given tree
         * @param offset     The offset of the tree
         * @param tree       The tree
         * @param leftFirst  Whether to process the left branch first (otherwise iteration starts
         *                   at the node in {@code tree}.
         */
        public NodeTreeIterator(int offset, NodeTree<T> tree)
        {
            this.offset = offset;
            if (tree.pnode != null) {
                current = tree;
                if (tree.left == null) {
                    pos = 1;
                }
            }
        }

        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public boolean hasNext()
        {
            return current != null;
        }

        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public NodeAndPosition<T> next()
        {
            while (true) {
                while (pos == 0) {
                    current = current.left;
                    if (current.left == null) {
                        pos = 1;
                    }
                }
                NodeTree<T> top = current;

                if (pos == 1) {
                    pos = 2;
                    NodeAndPosition<T> rval = new NodeAndPosition<T>(top.pnode,
                            top.pnodeOffset + offset,
                            top.pnodeSize);
                    if (top.right == null) {
                        downStackRight();
                    }
                    return rval;
                }

                // pos == 2
                if (top.right == null) {
                    throw new NullPointerException();
                }
                offset += top.pnodeOffset + top.pnodeSize;
                top = top.right;
                current = top;
                pos = (top.left != null) ? 0 : 1;
            }
        }
        
        private void downStackRight()
        {
            NodeTree<T> top = current;
            current = current.parent;
            while (current != null && current.right == top) {
                top = current;
                current = current.parent;
                offset -= top.pnodeOffset + top.pnodeSize; 
            }
            pos = 1; // middle!
        }
    }
}
