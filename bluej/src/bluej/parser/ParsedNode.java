package bluej.parser;

import java.util.List;

import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

import bluej.parser.NodeTree.NodeAndPosition;

public abstract class ParsedNode
{
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

    public boolean equals(Object obj)
    {
        return obj == this;
    }
    
    public final NodeAndPosition findNodeAtOrAfter(int position, int startpos)
    {
        return nodeTree.findNodeAtOrAfter(position, startpos);
    }
    
    /**
     * Set the containing node tree. This is normally only called by NodeTree when inserting
     * this node into the tree.
     */
    public void setContainingNodeTree(NodeTree cnode)
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
