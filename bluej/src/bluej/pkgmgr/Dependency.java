package bluej.pkgmgr;

import java.awt.Graphics2D;
import java.util.Properties;

import bluej.graph.Edge;
import bluej.utility.Debug;

/**
 * A dependency between two targets in a package.
 *
 * @author  Michael Cahill
 * @version $Id: Dependency.java 1700 2003-03-13 03:34:20Z ajp $
 */
public abstract class Dependency extends Edge
{
    Package pkg;

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
    public abstract boolean contains(int x, int y);
    public abstract void highlight(Graphics2D g);

    public String toString()
    {
        return getFrom().getIdentifierName() +
                " --> " + getTo().getIdentifierName();
    }
}
