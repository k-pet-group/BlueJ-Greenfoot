package bluej.pkgmgr.dependency;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.DependentTarget;

/**
 * The parent of ExtendsDependency and ImplementsDependency.
 *
 * These kinds of dependency are shown with an open-triangle-head arrow
 * in the class diagram.
 */
public abstract sealed class ExtendsOrImplementsDependency extends Dependency permits ExtendsDependency, ImplementsDependency
{
    public ExtendsOrImplementsDependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        super(pkg, from, to);
    }
}
