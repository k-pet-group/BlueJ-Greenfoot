package bluej.debugger.jdi;

import javax.swing.tree.DefaultMutableTreeNode;

import com.sun.jdi.*;

class JdiThreadNode extends DefaultMutableTreeNode
{
	public JdiThreadNode()
	{
	}
	
	public JdiThreadNode(ThreadGroupReference tgr)
	{
		setUserObject(tgr);
	}
	
	public JdiThreadNode(JdiThread jt)
	{
		setUserObject(jt);	
	}

	public boolean getAllowsChildren()
	{
		return (getUserObject() == null) || (getUserObject() instanceof ThreadGroupReference);
	}

	public String toString()
	{
		if (getUserObject() != null) {
			try {
				return getUserObject().toString();
			}
			catch (ObjectCollectedException oce) {
				return "collected";
			}
		}

		return "All Threads";
	}
}
