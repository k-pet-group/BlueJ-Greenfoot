package bluej.debugger;

import javax.swing.tree.*;

/**
 * An interface defining the trees the debugger uses to
 * represent threads.
 * 
 * The primary objects that we should be dealing with are
 * the standard TreeModel objects. The special functions
 * added in this interface are the only way we can map
 * those TreeModel nodes into usable DebuggerThread objects
 * and visa versa.
 *
 * @author  Andrew Patterson
 * @version $Id: DebuggerThreadTreeModel.java 2214 2003-10-15 03:18:49Z ajp $
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

    /**
     * Find a node path in the tree that leads to a particular
     * DebuggerThread.
     * 
     * @param  thr the DebuggerThread to look for
     * @return     the TreePath to a node holding the thr or null
     *             if one does not exist
     */	
	public TreePath findNodeForThread(DebuggerThread thr);
}
