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
import bluej.utility.JavaNames;
import bluej.utility.Debug;
import bluej.classmgr.ClassMgr;
import bluej.classmgr.ClassPathEntry;
import bluej.pkgmgr.Package;

/**
 * A JPanel subclass which displays all the classes in a package. ie
 * show all classes in java.lang.
 * Allows the user to select a particular class which fires an action
 * event indicating the class chosen.
 *
 * @author Andy Marks
 * @author Andrew Patterson
 * @version $Id: ClassChooser.java 532 2000-06-08 07:46:08Z ajp $
 */
public class ClassChooser extends JPanel {

    private FlowPanel flowpanel = null;

    /**
     * Create a new empty ClassChooser.
     */
    public ClassChooser()  {

        setLayout(new BorderLayout());

/*        tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)(e.getPath().getLastPathComponent());
                    Class cl = (Class)node.getUserObject();

                    fireActionEvent(cl.getName());
                }
            });

        Box b = new Box(BoxLayout.Y_AXIS);
        {
            String[] d = {"Collection", "Collectable", "free", "four"};
            String[] d2 = {"lang", "io", "awt", "swing"};
            interfaces = new JList(d);
            packages = new JList(d2);

            b.add(packages);
            b.add(interfaces);
            b.add(tree);

        }

        MouseListener ml = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if(selRow != -1) {
                    popupClick(selRow, selPath);
                }
            }
        };
        tree.addMouseListener(ml);
  */
        flowpanel = new FlowPanel();

        JScrollPane scroller = new JScrollPane(flowpanel);
        {
            scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }

        add(scroller);
    }

    public void openPackage(String packageName, String[] classes, String[] packages)
    {
        flowpanel.removeAll();

        for(int i=0; i<classes.length; i++)
        {
            try {
                String className = packageName + "." + classes[i];
                Class packageClass = ClassMgr.loadBlueJClass(className);

                flowpanel.add(new ClassTarget(packageClass));
            }
            catch(ClassNotFoundException cfe) { }
        }

        for(int i=0; i<packages.length; i++)
        {
            String nestedPackageName = packageName + "." + packages[i];

            flowpanel.add(new PackageTarget(nestedPackageName));
        }

        flowpanel.invalidate();
        flowpanel.revalidate();
        flowpanel.repaint();
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

    /**
     *  Construct a tree representing the classe heirarchy
     *  contained within one level of a java package.
     *
     *  @param packageName  a dot delimitered package name ie. "java.io"
     *  @param classNames   an array of strings indicating the classes within this
     *                      package ie { "File", "PrintStream" }
     */
    private TreeModel analysePackage(String packageName, String[] classNames)
    {
        TreeModel classes = new DefaultTreeModel(new DefaultMutableTreeNode(Object.class));

        for(int i=0; i<classNames.length; i++)
        {
            try {
                String className = packageName + "." + classNames[i];
                Class packageClass = ClassMgr.loadBlueJClass(className);

                if (packageClass.isPrimitive() ||
                    packageClass.isInterface() ||
                    packageClass == Object.class)
                {
                    continue;
                }

                insertInto((DefaultMutableTreeNode)(classes.getRoot()), packageClass);
            }
            catch(ClassNotFoundException cfe) { }
        }

        return classes;
    }

    /**
     *  Insert the class (a real class, not an interface or primitive)
     *  into its correct place in a tree representing the class
     *  heirarchy
     *
     *  @param root         the node representing a superclass of cl
     *  @param cl           the class to insert into the tree
     */
    private void insertInto(DefaultMutableTreeNode root, Class cl)
    {
        // precondition: root's user object is a superclass of cl

        Class rootClass = (Class)root.getUserObject();

        if (!rootClass.isAssignableFrom(cl))
            throw new IllegalArgumentException("Precondition wasn't true for " + rootClass.getName() + " " + cl.getName());

        // we will probably have to insert this node so we create it now
        DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(cl);

        DefaultMutableTreeNode reparentToClass[] = new DefaultMutableTreeNode[root.getChildCount()];
        int reparentCount = 0;

        // look for a child node that we are either a superclass or subclass
        // of
        for(int i=0; i<root.getChildCount(); i++)
        {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)root.getChildAt(i);
            Class childClass = (Class)childNode.getUserObject();

            if (cl.isAssignableFrom(childClass))
            {
                // cl is a superclass of the child so we need to reparent
                // the child to be a real child of classNode
                // but we can't reparent yet because it will stuff up our
                // enumerating so we store them in an array and reparent them
                // in a batch later on

                reparentToClass[reparentCount++] = childNode;
            }

            if (childClass.isAssignableFrom(cl))
            {
                if (reparentCount > 0)
                    throw new IllegalStateException("reparentCount > 0");

                // childClass is a superclass of cl so we recurse down the tree
                insertInto(childNode, cl);
                return;
            }
        }

        for(int i=0; i<reparentCount; i++)
            classNode.add(reparentToClass[i]);

        root.add(classNode);
    }

}

class FlowPanel extends JPanel  {

/*    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    public int getScrollableUnitIncrement(Rectangle visibleRect,
                                      int orientation,
                                      int direction)
    {
        return 10;
}

    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                       int orientation,
                                       int direction)
    {
        return 20;
    }

    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
*/
    public FlowPanel()
    {
        setLayout(new FlowLayout(FlowLayout.LEFT, 16, 16));
    }

    public Color getBackground() { return Color.white; }

    public Dimension getPreferredSize()
    {
        Dimension supdim = super.getPreferredSize();

        return new Dimension(1,getHeight());
    }


}

class QuickCellRenderer extends DefaultTreeCellRenderer {

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

        return super.getTreeCellRendererComponent(tree,
                        JavaNames.stripPrefix(cl.getName()),
                        sel, expanded, leaf, row, hasFocus);
    }

}

  