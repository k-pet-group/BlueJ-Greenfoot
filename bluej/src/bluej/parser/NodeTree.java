package bluej.parser;

import java.util.Iterator;
import java.util.Stack;

/**
 * Represents a set of ParsedNode using a (red/black) tree structure.
 *
 * @author davmac
 */
public class NodeTree
{
	// The following two instance variables specify the position of the contained ParsedNode,
	// relative to the position that this NodeTree represents.
	private int pnodeOffset; // offset of the ParsedNode in this tree node from the node start
	private int pnodeSize; // size of the ParsedNode in this tree node
	
	private NodeTree parent;
	
	private NodeTree left;
	private ParsedNode pnode;
	private NodeTree right; // offset pnodeOffset + pnodeSize
	
	private boolean black;  // true = black, false = red
	
	/**
	 * Construct an empty node tree.
	 */
	public NodeTree()
	{
		black = true;
	}
	
	public Iterator<ParsedNode> iterator()
	{
		return new NodeTreeIterator(this);
	}
	
	/**
	 * Find the ParsedNode leaf corresponding to a certain position within the parent.
	 * Returns null if no leaf contains exactly the given position.
	 */
	public NodeAndPosition findNode(int pos)
	{
	    return findNode(pos, 0);
	}
	
	public NodeAndPosition findNode(int pos, int startpos)
	{		
        if (pnode == null) {
            return null; // empty node tree
        }
        
		if (startpos + pnodeOffset > pos) {
			if (left != null) {
				return left.findNode(pos, startpos);
			}
			else {
				return null;
			}
		}
				
		if (startpos + pnodeSize + pnodeOffset > pos) {
			return new NodeAndPosition(pnode, startpos + pnodeOffset, pnodeSize);
		}
		
		if (right != null) {
			return right.findNode(pos, startpos + pnodeOffset + pnodeSize);
		}
		
		return null;
	}
	
	public NodeAndPosition findNodeAtOrBefore(int pos)
	{
	    return findNodeAtOrBefore(pos, 0);
	}
	
	public NodeAndPosition findNodeAtOrBefore(int pos, int startpos)
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
			return new NodeAndPosition(pnode, startpos + pnodeOffset, pnodeSize);
		}
		
		NodeAndPosition rval = null;
		if (right != null) {
			pos -= (pnodeOffset + pnodeSize);
			rval = right.findNodeAtOrBefore(pos, startpos + pnodeOffset + pnodeSize);
		}
		
		if (rval == null) {
			rval = new NodeAndPosition(pnode, startpos + pnodeOffset, pnodeSize);
		}
		
		return rval;
	}
	
	public NodeAndPosition findNodeAtOrAfter(int pos)
	{
	    return findNodeAtOrAfter(pos, 0);
	}
	
	public NodeAndPosition findNodeAtOrAfter(int pos, int startpos)
	{
	    if (pnode == null) {
	        return null; // empty node tree
	    }
	    
        if (startpos + pnodeOffset > pos) {
            if (left != null) {
                NodeAndPosition rval = left.findNodeAtOrAfter(pos, startpos);
                if (rval != null) {
                    return rval;
                }
            }
        }
                
        if (startpos + pnodeSize + pnodeOffset > pos) {
            return new NodeAndPosition(pnode, startpos + pnodeOffset, pnodeSize);
        }
        
        NodeAndPosition rval = null;
        if (right != null) {
            rval = right.findNodeAtOrAfter(pos, startpos + pnodeOffset + pnodeSize);
        }
        return rval;
	}
	
	/**
	 * Set the size of the contained ParsedNode. This is to be used in cases where the
	 * node has shrunk or grown because of text being removed or inserted, not for cases
	 * when the node is taking on more (or less) text from the document.
	 */
	public void setNodeSize(int newSize)
	{
	    pnodeSize = newSize;
	}
	
	/**
	 * Get the size of the contained ParsedNode.
	 */
	public int getNodeSize()
	{
	    return pnodeSize;
	}
	
	public void insertNode(ParsedNode newNode, int pos, int size)
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
					left = new NodeTree(this, newNode, pos, size);
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
					right = new NodeTree(this, newNode, pos, size);
					fixupNewNode(right);
				}
				else {
					right.insertNode(newNode, pos, size);
				}
			}
		}
	}

	/**
	 * Remove this node from the tree.
	 */
	public void remove()
	{
	    if (left == null || right == null) {
	        one_child_remove();
	    }
	    else {
	        NodeTree sub = left;
	        int nmoffset = 0;
	        while (sub.right != null) {
                nmoffset += (sub.pnodeOffset + sub.pnodeSize);
	            sub = sub.right;
	        }
	        swapNodeData(this, sub);
	        
	        pnodeOffset -= nmoffset;
	        int rchange = (sub.pnodeOffset + sub.pnodeSize) - (pnodeOffset + pnodeSize);
	        right.adjustLeftOffsets(rchange);
	        
	        sub.one_child_remove();
	    }
	}
	
	private void adjustLeftOffsets(int amount)
	{
	    NodeTree nt = this;
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
	private static void replace_node(NodeTree dest, NodeTree with)
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
	
	private void one_child_remove()
	{
	    if (left == null && right == null) {
	        pnode = null;
	        if (parent != null) {
                if (black) {
                    delete_case_1();
                }
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
	
	private NodeTree getSibling()
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
	    if (parent != null) {
	        // delete case 2
	        NodeTree sibling = getSibling();
	        if (! isBlack(sibling)) {
	            parent.black = false;
	            sibling.black = true;
	            if (this == parent.left) {
	                rotateLeft(parent);
	            }
	            else {
	                rotateRight(parent);
	            }
	        }
            delete_case_3();
	    }
	}
	
	private void delete_case_3()
	{
	    NodeTree sibling = getSibling();
	    if (parent.black && sibling.black && isBlack(sibling.left) && isBlack(sibling.right)) {
	        // That's a lot of black.
	        sibling.black = false;
	        parent.delete_case_1();
	    }
	    else {
            // delete case 4
	        if (! parent.black && sibling.black && sibling.left.black && sibling.right.black) {
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
	private static void fixupNewNode(NodeTree n)
	{
		if (n.parent == null) {
		    n.black = true;
		    return;
		}
		
		if (n.parent.isBlack()) {
			return; // ok - we are balanced
		}
		
		// We know from here on the parent is red.
		
		NodeTree grandparent = n.getGrandparent(); // cannot be null (root is always black).
		NodeTree uncle = n.getUncle();
		if (! isBlack(uncle)) {
			uncle.black = true;
			n.parent.black = true;
			grandparent.black = false;
			fixupNewNode(grandparent);
			return;
		}
		
		if (n == n.parent.right && n.parent == grandparent.left) {
			rotateLeft(n.parent);
			n = n.left;
			grandparent = n.getGrandparent();
		}
		else if (n == n.parent.left && n.parent == grandparent.right) {
			rotateRight(n.parent);
			n = n.right;
			grandparent = n.getGrandparent();
		}
		
		n.parent.black = true;
		grandparent.black = false;
		if (n == n.parent.left && n.parent == grandparent.left) {
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
	private static void swapNodeData(NodeTree n, NodeTree m)
	{
	    ParsedNode pn = n.pnode;
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
	
	private static void rotateLeft(NodeTree n)
	{
		// Right child of n becomes n's parent
		// We swap the data to avoid actually moving node n.
		swapNodeData(n, n.right);
		boolean nblack = n.black;
		n.black = n.right.black;
		n.right.black = nblack;
		
		n.pnodeOffset += n.right.pnodeOffset + n.right.pnodeSize;
		
		NodeTree oldLeft = n.left;
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
	
	private static void rotateRight(NodeTree n)
	{
		// Left child of n becomes n's parent
		// We swap the data to avoid actually moving node n.
	    swapNodeData(n, n.left);
        boolean nblack = n.black;
        n.black = n.left.black;
        n.left.black = nblack;
		
        n.right.pnodeOffset -= (n.pnodeOffset + n.pnodeSize);
        
		NodeTree oldRight = n.right;
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
	}
	
	private NodeTree getGrandparent()
	{
		if (parent != null) {
			return parent.parent;
		}
		else {
			return null;
		}
	}
	
	private NodeTree getUncle()
	{
	    return parent.getSibling();
	}
	
	private NodeTree(NodeTree parent, ParsedNode node, int offset, int size)
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
	
	private static boolean isBlack(NodeTree n)
	{
	    return n == null || n.black;
	}
	
	/**
	 * A class to represent a [node, position] tuple.
	 */
	public static class NodeAndPosition
	{
	    private ParsedNode parsedNode;
	    private int position;
	    private int size;
	    
	    public NodeAndPosition(ParsedNode pn, int position, int size)
	    {
	        this.parsedNode = pn;
	        this.position = position;
	        this.size = size;
	    }
	    
	    public ParsedNode getNode()
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
	}
	
	/**
	 * An iterator through a node tree.
	 */
	private static class NodeTreeIterator implements Iterator<ParsedNode>
	{
		Stack<NodeTree> stack;
		int pos = 0; // 0 - left, 1 = middle, 2 = right
		
		public NodeTreeIterator(NodeTree tree)
		{
			stack = new Stack<NodeTree>();
			if (tree.pnode != null) {
				stack.push(tree);
				if (tree.left == null) {
					pos = 1;
				}
			}
		}
		
		public boolean hasNext()
		{
			return !stack.isEmpty();
		}
		
		public ParsedNode next()
		{
			NodeTree top = stack.peek();
			while (pos == 0) {
				top = top.left;
				stack.push(top);
				if (top.left == null) {
					pos = 1;
				}
			}
			
			if (pos == 1) {
				pos = 2;
				if (top.right == null) {
					downStack();
				}
				return top.pnode;
			}
			
			// pos == 2
			top = top.right;
			stack.push(top);
			pos = (top.left != null) ? 0 : 1;
			return next();
		}
		
		private void downStack()
		{
			NodeTree top = stack.pop();
			while (!stack.isEmpty() && stack.peek().right == top) {
				top = stack.pop();
			}
			pos = 1; // middle!
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
