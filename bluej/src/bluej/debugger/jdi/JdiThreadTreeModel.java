/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import java.util.Enumeration;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import bluej.debugger.DebuggerThread;
import bluej.debugger.DebuggerThreadTreeModel;

import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.ThreadReference;

/**
 * A wrapper around DefaultTreeModel that helps us store JdiThreads.
 * 
 * Our tree stores the data we are interested in the userObject field of the
 * TreeNode (in our tree this type is called JdiThreadNode). The data can either
 * be null (for the root), a ThreadGroupReference (for thread groups) or a
 * JdiThread object (for actual threads).
 * 
 * @author Andrew Patterson
 * @version $Id: JdiThreadTreeModel.java 8085 2010-08-17 03:21:40Z davmac $
 */
public class JdiThreadTreeModel extends DefaultTreeModel
    implements DebuggerThreadTreeModel
{
    private SyncMechanism syncer;

    /**
     * Construct a tree model holding threads.
     * 
     * @param root
     */
    public JdiThreadTreeModel(JdiThreadNode root)
    {
        super(root, true);
        syncer = new SyncMechanism() {
            public void invokeLater(Runnable r)
            {
                synchronized (getRoot()) {
                    r.run();
                }
            }
        };
    }

    /**
     * Set the synchronisation method used to control access to the tree model.
     * This can be used to enforce Swing threading safety.
     */
    public void setSyncMechanism(SyncMechanism s)
    {
        syncer = s;
    }

    /**
     * Invoke some code via the the installed synchronization method.
     * 
     * @param r
     *            The code to invoke in a synchronized fashion
     */
    public void syncExec(Runnable r)
    {
        syncer.invokeLater(r);
    }

    /**
     * Return the DebuggerThread object stored in a tree node.
     * 
     * @param node
     *            an object of type JdiThreadNode.
     * @returns the user object of the node typecast to a DebuggerThread object
     *          or null if this node does not contain a DebuggerThread.
     */
    public DebuggerThread getNodeAsDebuggerThread(Object node)
    {
        Object o = ((JdiThreadNode) node).getUserObject();
        if (o instanceof JdiThread) {
            return (JdiThread) o;

        }
        return null;
    }

    public TreePath findNodeForThread(DebuggerThread thr)
    {
        return new TreePath(findThreadNode(((JdiThread) thr).getRemoteThread()).getPath());
    }

    /**
     * Return the root node as a JdiThreadNode.
     */
    JdiThreadNode getThreadRoot()
    {
        return (JdiThreadNode) getRoot();
    }

    /**
     * Find a node in the tree holding a particular ThreadGroupReference.
     * 
     * @param tgr
     *            the ThreadGroupReference to look for.
     * @return the JdiThreadNode holding the tgr or null if one does not exist.
     */
    JdiThreadNode findThreadNode(ThreadGroupReference tgr)
    {
        Enumeration<?> en = getThreadRoot().breadthFirstEnumeration();

        while (en.hasMoreElements()) {
            JdiThreadNode n = (JdiThreadNode) en.nextElement();

            Object o = n.getUserObject();
            if (o instanceof ThreadGroupReference) {
                ThreadGroupReference otgr = (ThreadGroupReference) o;

                if (otgr.equals(tgr))
                    return n;
            }
        }
        return null;
    }

    /**
     * Find a node in the tree holding a particular ThreadReference.
     * 
     * @param tgr
     *            the ThreadReference to look for.
     * @return the JdiThreadNode holding the tr or null if one does not exist.
     */
    JdiThreadNode findThreadNode(ThreadReference tr)
    {
        Enumeration<?> en = getThreadRoot().breadthFirstEnumeration();

        while (en.hasMoreElements()) {
            JdiThreadNode n = (JdiThreadNode) en.nextElement();

            Object o = n.getUserObject();
            if (o instanceof JdiThread) {
                JdiThread jt = (JdiThread) o;

                if (jt.getRemoteThread().equals(tr))
                    return n;
            }
        }

        return null;
    }
}