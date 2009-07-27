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
		if (startpos + pnodeOffset > pos) {
			if (left != null) {
				return left.findNode(pos, startpos);
			}
			else {
				return null;
			}
		}
				
		if (startpos + pnodeSize + pnodeOffset > pos) {
			return new NodeAndPosition(pnode, startpos + pnodeOffset);
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
		if (startpos + pnodeOffset > pos) {
			if (left != null) {
				return left.findNodeAtOrBefore(pos, startpos);
			}
			else {
				return null;
			}
		}
				
		if (startpos + pnodeSize + pnodeOffset > pos) {
			return new NodeAndPosition(pnode, startpos + pnodeOffset);
		}
		
		NodeAndPosition rval = null;
		if (right != null) {
			pos -= (pnodeOffset + pnodeSize);
			rval = right.findNodeAtOrBefore(pos, startpos + pnodeOffset + pnodeSize);
		}
		
		if (rval == null) {
			rval = new NodeAndPosition(pnode, startpos + pnodeOffset);
		}
		
		return rval;
	}
	
	public void insertNode(ParsedNode newNode, int pos, int size)
	{
		if (pnode == null) {
			pnode = newNode;
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
			return; // shouldn't actually happen
		}
		
		if (n.parent.isBlack()) {
			return; // ok - we are balanced
		}
		
		// We know from here on the parent is red.
		
		NodeTree grandparent = n.getGrandparent();
		NodeTree uncle = n.getUncle();
		if (uncle != null && uncle.isRed()) {
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
	 * Swap the data of two nodes.
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
	}
	
	private static void rotateLeft(NodeTree n)
	{
		// Right child of n becomes n's parent
		// We swap the data to avoid actually moving node n.
		swapNodeData(n, n.right);
		
		NodeTree oldLeft = n.left;
		n.left = n.right;
		n.right = n.left.right;
		n.left.right = n.left.left;
		n.left.left = oldLeft;
	}
	
	private static void rotateRight(NodeTree n)
	{
		// Left child of n becomes n's parent
		// We swap the data to avoid actually moving node n.
		ParsedNode pn = n.pnode;
		int offset = n.pnodeOffset;
		int size = n.pnodeSize;
		
		n.pnode = n.left.pnode;
		n.pnodeOffset = n.left.pnodeOffset;
		n.pnodeSize = n.left.pnodeSize;
		
		n.left.pnode = pn;
		n.left.pnodeOffset = offset;
		n.left.pnodeSize = size;
		
		NodeTree oldRight = n.right;
		n.right = n.left;
		n.left = n.right.left;
		n.right.left = n.right.right;
		n.right.right = oldRight;
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
		NodeTree grandParent = getGrandparent();
		if (grandParent == null) {
			return null;
		}
		else {
			if (grandParent.left == parent) {
				return grandParent.right;
			}
			else {
				return grandParent.left;
			}
		}
	}
	
	private NodeTree(NodeTree parent, ParsedNode node, int offset, int size)
	{
		this.parent = parent;
		pnode = node;
		this.pnodeSize = size;
		this.pnodeOffset = offset;
		black = false; // initial colour is red
	}
	
	private boolean isBlack()
	{
		return black;
	}
	
	private boolean isRed()
	{
		return !black;
	}
	
	/**
	 * A class to represent a [node, position] tuple.
	 */
	public static class NodeAndPosition
	{
	    private ParsedNode parsedNode;
	    private int position;
	    
	    public NodeAndPosition(ParsedNode pn, int position)
	    {
	        this.parsedNode = pn;
	        this.position = position;
	    }
	    
	    public ParsedNode getNode()
	    {
	        return parsedNode;
	    }
	    
	    public int getPosition()
	    {
	        return position;
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
