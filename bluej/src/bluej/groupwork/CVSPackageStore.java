package bluej.groupwork;

import bluej.pkgmgr.Package;

/**
 ** @version $Id: CVSPackageStore.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** An implementation of a GroupPackageStore using CVS
 **/
public class CVSPackageStore extends GroupPackageStore
	implements CVSUserInterface
{
	private CVSOperation op;
	static Properties props = bluej.Main.loadProps("cvs.ini");
	static final String prefix = "bluej.cvs";
	
	public CVSPackageStore()
	{
		String hostname = props.getProperty(prefix + ".hostname", "localhost");
		String rootdirname = props.getProperty(prefix + ".rootdirname");
		String tempdirname = props.getProperty(prefix + ".tempdirname");
		String localrootdirname = props.getProperty(prefix + ".localrootdirname", ".");
		String username = props.getProperty(prefix + ".username");
		String password = props.getProperty(prefix + ".password");
		
		this.op = new CVSOperation(hostname, rootdirname, tempdirname);
		op.setIdentity(username, password);
	}
	
	public void createGroupPackage(Package pkg)
	{
	}
	
	public String[] listPackages()
	{
	}
	
	public GroupPackage openGroupPackage(String name)
	{
	}
	
	public synchronized void configure()
	{
		String hostname = props.getProperty(prefix + ".hostname", "localhost");
		String rootdirname = props.getProperty(prefix + ".rootdirname");
		String tempdirname = props.getProperty(prefix + ".tempdirname");
		String localrootdirname = props.getProperty(prefix + ".localrootdirname", ".");
		String username = props.getProperty(prefix + ".username");
		String password = props.getProperty(prefix + ".password");
		
		this.op = new CVSOperation(hostname, rootdirname, tempdirname);
		op.setIdentity(username, password);
		
		// ..., then
		bluej.Main.saveProps("cvs.ini", props);
	}

	// CVSUserInterface methods
	public void uiDisplayProgressMsg(String message)
	{
		System.out.println(message);
	}

	public void uiDisplayProgramError(String error)
	{
		System.err.println(error);
	}

	public void uiDisplayResponse(CVSResponse response)
	{
		System.out.println("CVS reponse: " + response);
	}
}
