package bluej.pkgmgr.dependency;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;

import java.util.Properties;

/**
 * An "extends" dependency between two (class) targets in a package
 *
 * @author Michael Kolling
 * @version $Id: ExtendsDependency.java 2755 2004-07-07 15:52:12Z mik $
 */
public class ExtendsDependency extends Dependency
{
    public ExtendsDependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        super(pkg, from, to);
    }

    public ExtendsDependency(Package pkg)
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
        props.put(prefix + ".type", "ExtendsDependency");
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
