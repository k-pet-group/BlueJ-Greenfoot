package bluej.pkgmgr;

import bluej.graph.Edge;
import bluej.utility.Utility;
import bluej.utility.Debug;
import java.util.Properties;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Color;

/**
 ** A dependency between two targets in a package
 **
 ** @author Michael Cahill
 **
 ** @version $Id: Dependency.java 485 2000-05-18 03:00:38Z mik $
 **/

public abstract class Dependency extends Edge
{
    Package pkg;

    public Dependency(Package pkg, Target from, Target to)
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

    public Target getFrom()
    {
	return (Target)from;
    }

    public Target getTo()
    {
	return (Target)to;
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
	props.put(prefix + ".from", ((Target)from).getName());
	props.put(prefix + ".to", ((Target)to).getName());
    }

    public abstract void draw(Graphics2D g);
    public abstract boolean contains(int x, int y);
    public abstract void highlight(Graphics2D g);

    public String toString()
    {
	return getFrom().getBaseName() + " --> " + getTo().getBaseName();
    }
}
