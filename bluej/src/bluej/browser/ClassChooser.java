package bluej.browser;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.plaf.basic.*;
import javax.swing.event.*;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.zip.*;
import java.util.jar.*;

import java.io.*;

import bluej.Config;
import bluej.utility.ModelessMessageBox;
import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.classmgr.ClassMgr;
import bluej.classmgr.ClassPathEntry;

/**
 * A JPanel subclass which displays all the classes in a package. ie
 * show all classes in java.lang.
 * Allows the user to select a particular class which fires an action
 * event indicating the class chosen.
 * 
 * @author Andy Marks
 * @author Andrew Patterson
 * @version $Id: ClassChooser.java 265 1999-11-05 04:31:07Z ajp $
 */
public class ClassChooser extends JPanel {

    private JTree tree = null;

    /**
     * Create a new empty ClassChooser.
     */
    public ClassChooser()  {

        setLayout(new BorderLayout());
        
        tree = new JTree(new Vector());

//        tree.setCellRenderer(new QuickCellRenderer());
//        tree.setRowHeight(40);
        
        tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)(e.getPath().getLastPathComponent());
                    Class cl = (Class)node.getUserObject();

                    fireActionEvent(cl.getName());
                }
            });


        JScrollPane scroller = new JScrollPane(tree);
        {
            scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }

        add(scroller);      
             
    }

    public void openPackage(String[] pkgname)
    {
    }            

    public void openPackage(DefaultTreeModel dtm)
    {
        tree.setModel(dtm);
        tree.setRootVisible(false);
    }
    
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }


    // Notify all listeners that have registered interest for
    // notification on this event type.  The event instance 
    // is lazily created using the parameters passed into 
    // the fire method.

    protected void fireActionEvent(String className) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, className));
            }	       
        }
    }	
}

/*
public class QuickCellRenderer implements TreeCellRenderer {


    public Component getTreeCellRendererComponent(JTree tree,
						  Object value,
						  boolean sel,
						  boolean expanded,
						  boolean leaf,
						  int row,
						  boolean hasFocus) 
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        Class cl = (Class)node.getUserObject();
        return new Target(cl.getName());
    }

}
  */
  