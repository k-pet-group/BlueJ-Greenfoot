package bluej.pkgmgr.dependency;

import java.util.Properties;

//import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;

/**
 * An "implements" dependency between two (class) targets in a package
 *
 * @author  Michael Kolling
 * @version $Id: ImplementsDependency.java 2755 2004-07-07 15:52:12Z mik $
 */
public class ImplementsDependency extends Dependency
{
	public ImplementsDependency(Package pkg, DependentTarget from, DependentTarget to)
	{
		super(pkg, from, to);
	}

	public ImplementsDependency(Package pkg)
	{
		this(pkg, null, null);
	}

	public void load(Properties props, String prefix)
	{
		super.load(props, prefix);
	}

	public void save(Properties props, String prefix)
	{
		super.save(props, prefix);
		props.put(prefix + ".type", "ImplementsDependency");
	}
    
    public void remove()
    {
        pkg.removeArrow(this);
    }
    
    public boolean isResizable()
    {
        return false;
    }
}
