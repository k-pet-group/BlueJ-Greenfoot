package bluej.pkgmgr;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.*;
import javax.swing.AbstractAction;

import bluej.Config;
import bluej.graph.*;
import bluej.graph.Edge;
import bluej.pkgmgr.target.*;
import bluej.utility.Debug;

/**
 * A dependency between two targets in a package.
 *
 * @author  Michael Cahill
 * @version $Id: Dependency.java 2038 2003-06-17 13:00:58Z fisker $
 */
public abstract class Dependency extends Edge
{
    Package pkg;
    private static final String removeStr = Config.getString("pkgmgr.classmenu.remove");

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

    public abstract void draw(Graphics2D g);
    public abstract void highlight(Graphics2D g);

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
           pkg.removeArrow(Dependency.this);
			
		}
    }

    public String toString()
    {
        return getFrom().getIdentifierName() +
                " --> " + getTo().getIdentifierName();
    }
}
