package bluej.groupwork;

import bluej.pkgmgr.Package;

/**
 ** @version $Id: GroupPackageStore.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** An abstract class representing a store for group packages
 **/
public abstract class GroupPackageStore
{
	public abstract void createGroupPackage(Package pkg);
	public abstract String[] listPackages();
	public abstract GroupPackage openGroupPackage(String name);
	
	public abstract void configure();
}
