package bluej.groupwork;

import bluej.pkgmgr.Package;

/**
 ** @version $Id: CVSOperation.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 **
 ** Helper methods for CVS
 **/
public class CVSOperation
{
	static final int connMethod = CVSRequest.METHOD_INETD;
	static final int cvsPort = 2401;
		
	String repository;
	String localRootDirName;
		
	CVSClient client;
	CVSProject project;
	CVSEntryVector entries = new CVSEntryVector();
	
	public CVSOperation(String hostname, String rootDirName, String tempDirName)
	{
		client = new CVSClient(hostname, cvsPort);
		project = new CVSProject(client);
		project.setPServer(true);
		project.setConnectionMethod(connMethod);
		setDirectories(rootDirName, tempDirName);
	}
	
	public void setDirectories(String rootDirName, String tempDirName)
	{
		project.setRootDirectory(rootDirName);
		project.setTempDirectory(tempDirName);
	}
	
	public void setRepository(String repository)
	{
		this.repository = repository;
		project.setRepository(repository);
	}
	
	public void setLocalRoot(String localRootDirName)
	{
		this.localRootDirName = localRootDirName;
		project.setLocalRootDirectory(localRootDirName);
	}
	
	public void setIdentity(String userName, String password)
	{
		project.setUserName(userName);
		project.setPassword(CVSScramble.scramblePassword(password, 'A'));
	}
	
	public void setEntries(CVSEntryVector entries)
	{
		this.entries = entries;
	}
	
	public void setup()
	{
		String repositoryDirName = localRootDirName + "/"+ repository;
		
		File repositoryDir = new File(repositoryDirName);
		
		if(repositoryDir.exists())
		{
			project.openProject(repositoryDir, client);
			getAllEntries(project.getEntryList());
		}
		else
		{
			if(!repositoryDir.mkdirs())
			{
				System.err.println(
					"Could not create directory '"
					+ repositoryDir.getPath() + "'" );
				return;
			}
		}
	}
	
	public synchronized void execute(String command, CVSUserInterface if)
	{
		CVSRequest request = new CVSRequest();
		request.setUserInterface(if);
		boolean okay = request.parseControlString(command);

		if(!okay)
		{
			System.err.println("CVSRequest.parseControlString failed to parse: '" + command + "' because " + request.getVerifyFailReason());
			System.exit(1);
		}

		if(!request.queueResponse)
			request.responseHandler = project;

		request.setPort(cvsPort);

		request.setEntries(entries);

		CVSArgumentVector arguments = new CVSArgumentVector();
		request.appendArguments(arguments);

		okay = project.performCVSRequest(request);
		if(!okay)
			System.err.println("Something went wrong performing the request");

		project.writeAdminFiles();
		
		if(request.isRedirected())
			request.endRedirection();
	}

	/** Recurse through subdirectories, adding all entries to this.entries **/
	public void getAllEntries()
	{
		getAllEntries(project.getEntryList());
	}
	
	/** Recurse through subdirectories, adding all entries to this.entries **/
	protected void getAllEntries(CVSEntryVector fromEntries)
	{
		if(fromEntries == null)
			throw new NullPointerException("fromEntries == null");

		for(int i = 0; i < fromEntries.size(); ++i)
		{
			CVSEntry	entry = fromEntries.entryAt(i);

			this.entries.appendEntry(entry);
			
			if(entry.isDirectory())
				getAllEntries(entry.getEntryList());
		}
	}
}
