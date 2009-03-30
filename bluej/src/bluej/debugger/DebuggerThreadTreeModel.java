/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * An interface defining the trees the debugger uses to represent threads.
 * 
 * The primary objects that we should be dealing with are the standard TreeModel
 * objects. The special functions added in this interface are the only way we
 * can map those TreeModel nodes into usable DebuggerThread objects and visa
 * versa.
 * 
 * The TreeModel is used as the model for a Swing tree widget (JTree). A
 * synchronisation mechanism must be provided to ensure that modifications to
 * the tree only occur on the event handling thread.
 * 
 * @author Andrew Patterson
 * @version $Id: DebuggerThreadTreeModel.java 6215 2009-03-30 13:28:25Z polle $
 */
public interface DebuggerThreadTreeModel
    extends TreeModel
{
    /**
     * Return the DebuggerThread object stored in a tree node.
     * 
     * @param node
     *            an object returned from one of the TreeModel methods ie.
     *            getChild(), getRoot() etc
     * @return the DebuggerThread represented by this node or null if this node
     *         does not represent a DebuggerThread.
     */
    public DebuggerThread getNodeAsDebuggerThread(Object node);

    /**
     * Find a node path in the tree that leads to a particular DebuggerThread.
     * 
     * @param thr
     *            the DebuggerThread to look for
     * @return the TreePath to a node holding the thr or null if one does not
     *         exist
     */
    public TreePath findNodeForThread(DebuggerThread thr);

    /**
     * Define the interface for a synchronization method.
     */
    public interface SyncMechanism
    {
        public void invokeLater(Runnable r);
    }

    /**
     * Set the synchronisation method used to control access to the tree model.
     * This can be used to enforce Swing threading safety.
     */
    public void setSyncMechanism(SyncMechanism s);
}