package bluej.groupwork;

import bluej.pkgmgr.Package;

/**
 ** @version $Id: CVSPackage.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** An implementation of a GroupPackage using CVS
 **/
public abstract class CVSGroupPackage extends GroupPackage
{
	/** Do a CVS update for the package **/
	public void update()
	{
	}
	
	/** Do a CVS commit for the package **/
	public void commit(String msg, Vector targets)
	{
	}

	/** Do a CVS status for the target **/
	public String status(Target t)
	{
	}

	/** Do a CVS edit for the target **/
	public String edit(Target t)
	{
	}

	/** Unedit the target and revert to the checked-in version **/
	public String revert(Target t)
	{
	}

	/** Do a CVS users for the target **/
	public String[] users(Target t)
	{
	}
}
