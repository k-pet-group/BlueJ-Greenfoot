package bluej.parser;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;

import org.syntax.jedit.tokenmarker.Token;

public abstract class ParsedNode
{
	private NodeTree nodeTree;
	
	public ParsedNode()
	{
		nodeTree = new NodeTree();
	}
	
	protected NodeTree getNodeTree()
	{
	    return nodeTree;
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
	
	/**
	 * This node is shortened, it no longer needs all the text assigned to it.
	 */
	protected void nodeShortened(int newLength) {}
	
	/**
	 * This node has become incomplete (needs to be extended).
	 */
	protected void nodeIncomplete() {}
	
	/**
	 * This node should be re-parsed in full.
	 */
	//protected void reparseNode() {}
}
