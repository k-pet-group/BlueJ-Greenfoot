package bluej.debugger;

import javax.swing.tree.*;

/**
 * An interface defining the trees the debugger uses to
 * represent threads.
 *
 * @author  Andrew Patterson
 * @version $Id: DebuggerThreadTreeModel.java 2037 2003-06-17 05:54:51Z ajp $
 */
public interface DebuggerThreadTreeModel extends TreeModel
{
	/**
	 * Return the DebuggerThread object stored in
	 * a tree node.
	 * 
	 * @param node  an object returned from one of the TreeModel
	 *              methods ie. getChild(), getRoot() etc
	 * @return      the DebuggerThread represented by this node
	 *              or null if this node does not represent a
	 *              DebuggerThread.
	 */
	public DebuggerThread getNodeAsDebuggerThread(Object node);
	
	public TreePath findNodeForThread(DebuggerThread thr);
}
