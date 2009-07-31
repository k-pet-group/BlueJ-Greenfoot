package bluej.parser;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

public abstract class ParsedNode
{
	/** The NodeTree containing the child nodes of this node */
    private NodeTree nodeTree;
    /** The NodeTree node (belonging to the parent parse node) which contains this node */
    private NodeTree containingNodeTree;
    /** The parent ParsedNode which contains us */
    private ParsedNode parentNode;
	
	public ParsedNode()
	{
		nodeTree = new NodeTree();
	}
	
	ParsedNode(ParsedNode parentNode)
	{
	    this.parentNode = parentNode;
	}
	
	/**
	 * Set the containing node tree. This is normally only called by NodeTree when inserting
	 * this node into the tree.
	 */
	public void setContainingNodeTree(NodeTree cnode)
	{
	    containingNodeTree = cnode;
	}
	
	/**
	 * Insert the given text.
	 * 
	 * The result can be:
	 * - text absorbed, no change to node structure
	 * - node terminates earlier (eg ';' or '}' inserted)
	 * - subnode created, and this node extended (eg insert '{')
	 */
	public abstract void textInserted(int nodePos, DocumentEvent event);
	
	public abstract void textRemoved(int nodePos, DocumentEvent event);
	
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
	protected void reparseNode(Document document, int offset) {}
}
