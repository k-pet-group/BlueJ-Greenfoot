package bluej.pkgmgr;

import bluej.graph.Edge;
import bluej.utility.Utility;
import java.util.Properties;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Color;

/**
 ** @version $Id: Dependency.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** A dependency between two targets in a package
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
			throw new Error("Failed to find 'from' target " + fromName);
		String toName = props.getProperty(prefix + ".to");
		this.to = pkg.getTarget(toName);
		if(this.to == null)
			throw new Error("Failed to find 'to' target " + toName);
	}

	public void save(Properties props, String prefix)
	{
		props.put(prefix + ".from", ((Target)from).getName());
		props.put(prefix + ".to", ((Target)to).getName());
	}

	public abstract void draw(Graphics g);
	public abstract boolean contains(int x, int y);
	public abstract void highlight(Graphics g);
}
