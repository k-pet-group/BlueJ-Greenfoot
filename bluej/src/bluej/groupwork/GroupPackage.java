package bluej.groupwork;

import bluej.pkgmgr.Package;

/**
 ** @version $Id: GroupPackage.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** An abstract class representing a group package
 **/
public abstract class GroupPackage extends Package
{
	/** Incorporate changes from the store into the local copy of the package **/
	public abstract void update();
	
	/**
	 ** Incorporate changes from the local copy of the targets into the store
	 ** @param msg the message
	 ** @param targets the targets to commit
	 **/
	public abstract void commit(String msg, Vector targets);
	
	/**
	 ** Incorporate changes from the local copy of the package into the store
	 ** @param msg the message
	 **/
	public void commit(String msg)
	{
		commit(msg, this.targets);
	}
	
	/**
	 ** Get a string describing the status of the target
	 ** @param t the target
	 **/
	public abstract String status(Target t);
	
	/**
	 ** Get a list of the users who are editing the target
	 ** @param t the target
	 **/
	public abstract String[] users(Target t);
}

