package bluej.debugger.jdi;

import java.util.Enumeration;

import javax.swing.tree.DefaultTreeModel;

import bluej.debugger.*;

import com.sun.jdi.*;

/**
 * A wrapper around DefaultTreeModel that helps us
 * store JdiThreads.
 * 
 * Our tree stores the data we are interested in
 * the userObject field of the TreeNode (in our
 * tree this type is called JdiThreadNode). The data can
 * either be null (for the root), a ThreadGroupReference
 * (for thread groups) or a JdiThread object (for actual
 * threads).
 * 
 * @author  Andrew Patterson
 * @version $Id: JdiThreadTreeModel.java 2030 2003-06-11 07:58:29Z ajp $
 */
public class JdiThreadTreeModel extends DefaultTreeModel
	implements DebuggerThreadTreeModel
{
	/**
	 * Construct a tree model holding threads.
	 * 
	 * @param root
	 */
    public JdiThreadTreeModel(JdiThreadNode root)
    {
    	super(root, true);
    }

	/**
	 * Return the DebuggerThread object stored in
	 * a tree node.
	 * 
	 * @param node  an object of type JdiThreadNode.
	 * @returns     the user object of the node typecast
	 *              to a DebuggerThread object or null if
	 *              this node does not contain a DebuggerThread.
	 */
	public DebuggerThread getNodeAsDebuggerThread(Object node)
	{
		Object o = ((JdiThreadNode) node).getUserObject();
		if (o instanceof JdiThread) {
			return (JdiThread)o;

		}
		return null;
	}

	/**
	 * Return the root node as a JdiThreadNode.
	 */
	JdiThreadNode getThreadRoot()
	{
		return (JdiThreadNode) getRoot();
	}

	/**
	 * Find a node in the tree holding a particular
	 * ThreadGroupReference.
	 * 
	 * @param tgr  the ThreadGroupReference to look for.
	 * @return     the JdiThreadNode holding the tgr or null
	 *             if one does not exist.
	 */
	JdiThreadNode findThreadNode(ThreadGroupReference tgr)
	{
		Enumeration en = getThreadRoot().breadthFirstEnumeration();
	
		while(en.hasMoreElements()) {
			JdiThreadNode n = (JdiThreadNode) en.nextElement();

			Object o = n.getUserObject();
			if (o instanceof ThreadGroupReference) {
				ThreadGroupReference otgr = (ThreadGroupReference)o;

				if (otgr.equals(tgr))
					return n;				
			}
		}
		return null;
	}
		
	/**
	 * Find a node in the tree holding a particular
	 * ThreadReference.
	 * 
	 * @param tgr  the ThreadReference to look for.
	 * @return     the JdiThreadNode holding the tr or null
	 *             if one does not exist.
	 */
	JdiThreadNode findThreadNode(ThreadReference tr)
	{
		Enumeration en = getThreadRoot().breadthFirstEnumeration();
	
		while(en.hasMoreElements()) {
			JdiThreadNode n = (JdiThreadNode) en.nextElement();
			
			Object o = n.getUserObject();
			if (o instanceof JdiThread) {
				JdiThread jt = (JdiThread)o;

				if (jt.getRemoteThread().equals(tr))
					return n;				
			}
		}
				
		return null;
	}
}
