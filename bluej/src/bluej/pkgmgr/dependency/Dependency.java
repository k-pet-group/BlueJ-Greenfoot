package bluej.pkgmgr.dependency;

import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.*;
import javax.swing.AbstractAction;

import bluej.Config;
import bluej.graph.*;
import bluej.graph.Edge;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;
import bluej.utility.Debug;

/**
 * A dependency between two targets in a package.
 *
 * @author  Michael Cahill
 * @version $Id: Dependency.java 2472 2004-02-09 13:00:47Z fisker $
 */
public abstract class Dependency extends Edge implements Selectable
{
    Package pkg;
    private static final String removeStr = Config.getString("pkgmgr.classmenu.remove");
    protected boolean selected = false;
    protected static final float strokeWithDefault = 1.0f;
    protected static final float strokeWithSelected = 2.0f;

    public Dependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        super(from, to);
        this.pkg = pkg;
    }

    public Dependency(Package pkg)
    {
        this(pkg, null, null);
    }

    public boolean equals(Object other)
    {
        if(!(other instanceof Dependency))
            return false;
        Dependency d = (Dependency)other;
        return (d != null) && (d.from == from) && (d.to == to);
    }

    public int hashCode()
    {
        return to.hashCode() - from.hashCode();
    }

    public void repaint()
    {
        if (pkg.getEditor() != null) {
            pkg.getEditor().repaint();
        }
    }


    public DependentTarget getFrom()
    {
        return (DependentTarget)from;
    }

    public DependentTarget getTo()
    {
        return (DependentTarget)to;
    }

    public void load(Properties props, String prefix)
    {
        String fromName = props.getProperty(prefix + ".from");
        this.from = pkg.getTarget(fromName);
        if(this.from == null)
            Debug.reportError("Failed to find 'from' target " + fromName);
        String toName = props.getProperty(prefix + ".to");
        this.to = pkg.getTarget(toName);
        if(this.to == null)
            Debug.reportError("Failed to find 'to' target " + toName);
    }

    public void save(Properties props, String prefix)
    {
        props.put(prefix + ".from", ((DependentTarget)from).getIdentifierName());
        props.put(prefix + ".to", ((DependentTarget)to).getIdentifierName());
    }


    public void popupMenu(int x, int y, GraphEditor editor) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new RemoveAction());
        editor.add(menu);
        menu.show(editor, x, y);
    }
    
    private class RemoveAction extends AbstractAction
    {
        public RemoveAction()
        {
            putValue(NAME, removeStr);
        }

		public void actionPerformed(ActionEvent e) {
           remove();
			
		}
    }

    public String toString()
    {
        return getFrom().getIdentifierName() +
                " --> " + getTo().getIdentifierName();
    }
    
    /* (non-Javadoc)
     * @see bluej.graph.Selectable#setSelected(boolean)
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    /* (non-Javadoc)
     * @see bluej.graph.Selectable#isSelected()
     */
    public boolean isSelected() {
        return selected;
    }
    
    
    
	/* (non-Javadoc)
	 * @see bluej.graph.Selectable#isHandle(int, int)
	 */
	public boolean isHandle(int x, int y) {
		return false;
	}

	/* (non-Javadoc)
	 * @see bluej.graph.Selectable#isResizeing()
	 */
	public boolean isResizing() {
		return false;
	}

	/* (non-Javadoc)
	 * @see bluej.graph.Selectable#setResizeing(boolean)
	 */
	public void setResizing(boolean resizing) {
	}

}
