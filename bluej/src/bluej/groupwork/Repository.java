package bluej.groupwork;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;


import org.netbeans.lib.cvsclient.CVSRoot;
import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.netbeans.lib.cvsclient.command.add.AddCommand;
import org.netbeans.lib.cvsclient.command.checkout.CheckoutCommand;
import org.netbeans.lib.cvsclient.command.commit.CommitCommand;
import org.netbeans.lib.cvsclient.command.importcmd.ImportCommand;
import org.netbeans.lib.cvsclient.command.update.UpdateCommand;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.connection.ConnectionFactory;


import bluej.pkgmgr.Project;
/**
 * @author fisker
 *
 */
public class Repository {

	private String protocol;
	private String server;
	private String repository;
	private String module;
	private String user;
	private String scrambledPassword;
	
	private boolean reconnectBetweenCommands = true;
	
	//CVS specific members
	private CVSRoot cvsroot; 
	private GlobalOptions globalOptions;
	private Client client;
	private Project project;
	private Connection connection;
	
	// **** static declerations 
	public static Repository getInstance(Project project){
		if (!isGroupProject(project)){
			return null;
		}
		Repository repository = new Repository(project);
		repository.readSetupFile();
		repository.setupClient();
		//System.setProperty("javacvs.multiple_commands_warning", "false");
		return repository;
	}
	
	private static boolean isGroupProject(Project project){
		String cfgFilePath = project.getProjectDir() + "/team.cfg";
		File cfgFile = new File(cfgFilePath);
		return cfgFile.isFile();
	}
	
	private static boolean hasBeenCheckedOut(Project project){
		File cvsDir = new File(project.getProjectDir() + "/CVS");
		return cvsDir.isDirectory();
	}
	
	/**
	 * Get an array of Files that resides in the project folders.
	 * Should this method be moved to the Project class ?
	 * @param project the project
	 * @return array of Files 
	 */
	private static File[] getFilesInProject(Project project, boolean includePkgFiles){
		File projectDir = project.getProjectDir();
		List files = new LinkedList();
		traverseDirsForFiles(files, projectDir, includePkgFiles);
		return listToFileArray(files);
	}
	
	private static File[] getDirsInProjectWhichNeedToBeAdded(Project project) {
		File projectDir = project.getProjectDir();
		List files = new LinkedList();
		traverseDirsForDirs(files, projectDir);
		return listToFileArray(files);
	}
	
	
	private static void traverseDirsForFiles(List allFiles, File dir, boolean includePkgFiles){
		File[] files = dir.listFiles(new CodeFileFilter(includePkgFiles));
		if (files==null){
			return;
		}
		for(int i=0; i< files.length; i++ ){
			if (files[i].isFile()){
				allFiles.add(files[i]);
			}else{
				traverseDirsForFiles(allFiles, files[i], includePkgFiles);
			}
		}
	}
	
	/**
	 * Traverses a directory tree and records the directories it meat.
	 * @param allDirs a List in which the method will add the dirs it meets.
	 * @param dir the directory the search starts from.
	 */
	private static void traverseDirsForDirs(List allDirs, File dir){
		File[] files = dir.listFiles();
		boolean ok = false;
		if (files == null){
			return;
		}
		for(int i=0; i< files.length; i++ ){
			ok = !files[i].getName().equals("CVSROOT") &&
				 !files[i].getName().equals("CVS") && !isInCVS(files[i]);
			if (files[i].isDirectory()){
				if (ok) {
					allDirs.add(files[i]);
				}
				traverseDirsForDirs(allDirs, files[i]);
			}
		}
	}
	
	private static boolean isInCVS(File dir){
		char s = File.separatorChar;
		File cvsDir = new File(dir.getAbsolutePath() + s + "CVS");
		return cvsDir.exists();
	}
	
	/**
	 * @param unadded
	 */
	private static File[] listToFileArray(List unadded) {
		File[] files = new File[unadded.size()];
		int j = 0;
		for (Iterator i = unadded.iterator(); i.hasNext(); ) {
			File file = (File) i.next();
			files[j++] = file;
		}
		return files;	
	}
	
	// ** static declaration end
	
		
	private Repository(Project project){
		this.project = project;
		globalOptions = new GlobalOptions();
		globalOptions.setUseGzip(false);
	}
	
	// setup start
	
	/**
	 * @throws AuthenticationException
	 * @throws CommandAbortedException
	 */
	private void setupConnection() {
		connection = ConnectionFactory.getConnection(cvsroot);     
		try {
			connection.open();
			client.setConnection(connection);
		} catch (CommandAbortedException e) {
			e.printStackTrace();
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
	}
	
	private void setupClient(){
		//the client is created without a connection
		client = new Client(null, new StandardAdminHandler());
	    client.setLocalPath(project.getProjectDir().getParentFile().getAbsolutePath());
	    client.getEventManager().addCVSListener(new BasicListener());
	    client.dontUseGzipFileHandler();
	}

	
	private void disconnect(){
		if (reconnectBetweenCommands){
			try {
				client.getConnection().close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void readSetupFile(){
		File cfgFile = new File(project.getProjectDir() + "/team.cfg");
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(cfgFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		protocol = properties.getProperty("protocol");
		server = properties.getProperty("server");
		repository = properties.getProperty("groupname");
		module = project.getProjectName();
		user = properties.getProperty("user");
		scrambledPassword = properties.getProperty("password");
		
		//setup cvsroot and global options
		String cvsRoot = ":" + protocol+":"+user+":"+scrambledPassword+"@"+server+":/CVSNT/"+repository;
		System.out.println(cvsRoot);
		cvsroot = CVSRoot.parse(cvsRoot);
		globalOptions.setCVSRoot(cvsroot.toString());
	}

	// setup end
	
	// basic cvs commands begin
	
	private void checkout(){
		setupConnection();
		CheckoutCommand checkoutCommand = new CheckoutCommand(true, module);
		checkoutCommand.setRecursive(true);
		checkoutCommand.setPruneDirectories(false);
		SynchListener synchListener = new SynchListener();
		client.getEventManager().addCVSListener(synchListener);
		
		try {
			client.executeCommand(checkoutCommand, globalOptions);
		} catch (CommandAbortedException e2) {
			e2.printStackTrace();
		} catch (CommandException e2) {
			e2.printStackTrace();
		} catch (AuthenticationException e2) {
			e2.printStackTrace();
		}
		System.out.println("at Repository:236 RELOAD!!!!!!");
		synchListener.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(synchListener);
		disconnect();
		project.reloadAll();
	}
	
	
	private void addToRepository(File[] files){
		if (files.length < 1){
			return;
		}
		setupConnection();
		AddCommand addCommand = new AddCommand();
		addCommand.setFiles(files);
		SynchListener synchListener = new SynchListener();
		client.getEventManager().addCVSListener(synchListener);
		System.out.println("Cvs command: " + addCommand.getCVSCommand());
		try {
			client.executeCommand(addCommand, globalOptions);
		} catch (CommandAbortedException e) {
			e.printStackTrace();
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
		synchListener.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(synchListener);
		disconnect();
	}
	
	
	private void commitToRepository(File[] files){
		if (files.length < 1){
			return;
		}
		setupConnection();
		CommitCommand commitCommand = new CommitCommand();
		commitCommand.setMessage("");
		commitCommand.setFiles(files);
		SynchListener synchListener = new SynchListener();
		client.getEventManager().addCVSListener(synchListener);
		System.out.println("Cvs command: " + commitCommand.getCVSCommand());
		try {
			client.executeCommand(commitCommand, globalOptions);
		} catch (CommandAbortedException e) {
			e.printStackTrace();
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
		synchListener.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(synchListener);
		disconnect();
	}
	
	private void updateFromRepository(Project project){
		setupConnection();
		UpdateCommand command = new UpdateCommand();
		command.setCleanCopy(false);
		command.setRecursive(true);
		command.setBuildDirectories(true);
		command.setPruneDirectories(false);
		command.setCVSCommand('I',"bluej.pkg");// ignore bluej.pkg files
		SynchListener synchListener = new SynchListener();
		client.getEventManager().addCVSListener(synchListener);
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		System.out.println("Cvs command: " + command.getCVSCommand());
		try {
			client.executeCommand(command, globalOptions);
		} catch (CommandAbortedException e) {
			e.printStackTrace();
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
		synchListener.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(synchListener);
		disconnect();
	}
	
	private void importInRepository(Project project){
		setupConnection();
		
		ImportCommand importCommand = new ImportCommand();
		//importCommand.addWrapper(localPath + "/TestProject/simplePackage/SimpleClass.java", KeywordSubstitutionOptions.DEFAULT);
		//importCommand.addWrapper(localPath + "/TestProject/simplePackage/added.txt", KeywordSubstitutionOptions.DEFAULT);
		importCommand.setModule(project.getProjectName());
		importCommand.setReleaseTag("bluej");
		importCommand.setLogMessage("logMessage");
		importCommand.setVendorTag("Group_One");
		SynchListener synchListener = new SynchListener();
		client.getEventManager().addCVSListener(synchListener);
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		try {
			System.out.println("CVS: " + importCommand.getCVSCommand());
			client.executeCommand(importCommand, globalOptions);
		} catch (CommandAbortedException e) {
			e.printStackTrace();
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
		synchListener.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(synchListener);
		disconnect();
	}
	
	
	private void removeFromRepository(Project project){
		
	}
	
	// basic cvs commands end
	
	// util methods begin
	
	/**
	 * Get a list of UpdateResults that would result from an update.
	 * @return
	 */
	private List getUpdateResults(){
		setupConnection();
		UpdateCommand command = new UpdateCommand();
		UpdateListener updateListener = new UpdateListener();
		GlobalOptions globalOptions = new GlobalOptions();
		
		command.setCVSCommand('n',"");
		command.setCVSCommand('q',"");
		command.setRecursive(true);
		command.setBuildDirectories(true);
		command.setPruneDirectories(false);
		command.setCleanCopy(false);
		globalOptions.setCVSRoot(cvsroot.toString());
		globalOptions.setCVSCommand('n',"");
		globalOptions.setCVSCommand('q',"");
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		client.getEventManager().addCVSListener(updateListener);
		//client.dontUseGzipFileHandler();
		//System.out.println("Cvs command: " + command.getCVSCommand());
		//System.out.println("Cvs options: " + command.getOptString());
		try {
			client.executeCommand(command, globalOptions);
		} catch (CommandAbortedException e) {
			e.printStackTrace();
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
		List updateResults = updateListener.getUpdateResults();
		client.getEventManager().removeCVSListener(updateListener);
		disconnect();
		return updateResults;
	}
	
	/**
	 * 
	 * @return List of files in the sandbox that needs to be added to CVS
	 */
	private File[] getFilesInSandboxWhichNeedToBeAdded(){
		List updateResults = getUpdateResults();
		FilenameFilter filter = new CodeFileFilter();
		List files = new LinkedList();
		for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			File file = updateResultToFile(updateResult);
			if (filter.accept(new File(file.getParent()), file.getName()) && 
				updateResult.getStatusCode() == '?') {
				files.add(file);
				System.out.println("repository:380 added: " + file.getAbsoluteFile());
			}
		}
		return listToFileArray(files);
	}
	
	private File updateResultToFile(UpdateResult updateResult){
		String str;
		char s = File.separatorChar;
		str = project.getProjectDir().getAbsolutePath() + s + updateResult.getFilename();
		File file = new File(str);
		return file;
	}
	/**
	 * Look through the dirs and return an array of bluej.pkg files found
	 * @param dirs array of dirs that the method will look through
	 * @return an array of the found bluej.pkg files
	 */
	private File[] find_pkgFiles(File[] dirs){
		File[] pkgFiles;
		List pkgFilesList = new LinkedList();
		char s = File.separatorChar;
		for (int i = 0; i < dirs.length; i++) {
			File pkgFile = new File(dirs[i].getAbsolutePath() + s + "bluej.pkg");
			if (pkgFile.exists()){
				pkgFilesList.add(pkgFile);
			}
		}
		return listToFileArray(pkgFilesList);
	}
	
	private void updateGraphLayout(){
		File[] pkgArray = updatePkgFilesFromRepository();
		if (pkgArray.length < 1){
			return;
		}
		setupConnection();
		UpdateCommand command = new UpdateCommand();
		command.setFiles(pkgArray);
		command.setCleanCopy(true);
		command.setRecursive(true);
		command.setBuildDirectories(true);
		command.setPruneDirectories(false);
		SynchListener synchListener = new SynchListener();
		client.getEventManager().addCVSListener(synchListener);
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		System.out.println("Cvs command: " + command.getCVSCommand());
		try {
			client.executeCommand(command, globalOptions);
		} catch (CommandAbortedException e) {
			e.printStackTrace();
		} catch (CommandException e) {
			e.printStackTrace();
		} catch (AuthenticationException e) {
			e.printStackTrace();
		}
		synchListener.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(synchListener);
		disconnect();
		
	}
	
	/**
	 * Get an array of files containing all the bluej.pkg files in the repository
	 * @return array of files
	 */
	private File[] updatePkgFilesFromRepository() {
		List updateResults = getUpdateResults();
		List pkgFiles = new LinkedList();
		for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			if (updateResult.getFilename().endsWith("bluej.pkg")){
				pkgFiles.add(updateResultToFile(updateResult));
				//System.out.println("getPkgFilesInRepository:498 added " + updateResultToFile(updateResult).getAbsolutePath());
			}
		}
		File[] pkgArray = listToFileArray(pkgFiles);
		return pkgArray;
	}
	// util methods end

	// public methods begin
	
	

	public void printExperiments(Project project){
		System.out.println("====Files in project===========");
		File[] files = getFilesInProject(project, false);
		Set set = null;
		for(int i=0; i < files.length; i++ ){
			System.out.println(files[i].getName());
		}
		List updateResults = getUpdateResults();
		System.out.println("====Files in repository===========");
		for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			System.out.println(updateResult + " " + updateResultToFile(updateResult).getAbsolutePath());
		}
		try {
			set = client.getAllFiles(project.getProjectDir());
			//commitToRepository(getFilesInProject(project));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("====client.getAllFiles()===========");
		for (Iterator i = set.iterator(); i.hasNext();) {
			File file = (File) i.next();
			System.out.println("File: " + file.getName());
		}
		
		System.out.println("====FileInSandboxWhichNeedToBeAdded===========");
		files = getFilesInSandboxWhichNeedToBeAdded();
		for (int i = 0; i < files.length; i++) {
			System.out.println("File: " + files[i].getName());
		}
		/*
	    System.out.println("====Entries===========");
		try {
			for(Iterator i = client.getEntries(project.getProjectDir()) ; i.hasNext(); ){
				Entry entry = (Entry) i.next();
				System.out.println("Entry: " + entry.toString());
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		*/
	}
	
	/**
	 * Commits the files and directories in the project. If files or dirs need
	 * to be added first, they are added.
	 * The booelean pkg determins wether pkg files are included in the commit.
	 * One exception to this is newly created packages. They always have their
	 * pkg files committed. Otherwise bluej won't know the difference between
	 * simple directories and bluej packages.
	 * 
	 * @param project
	 * @param includePkgFiles if true, pkg files are included in the commit.
	 */
	public void commitAll(Project project, boolean includePkgFiles){
		// add and commit the folders
		File[] dirs = getDirsInProjectWhichNeedToBeAdded(project);
		addToRepository(dirs);
		commitToRepository(dirs);
		
		//add and commit the bluej.pkg files in dirs that was bluej packages
		File[] new_pkgFiles = find_pkgFiles(dirs);
		project.saveAllGraphLayout();
		addToRepository(new_pkgFiles);
		commitToRepository(new_pkgFiles);
		
		//add and commit the files
		addToRepository(getFilesInSandboxWhichNeedToBeAdded());
		commitToRepository(getFilesInProject(project, includePkgFiles));
	}
	
	/**
	 * Get all changes from repository except the pkg files that determine the
	 * layout of the graph.
	 * @param project
	 * @param includeGraphLayout should the update include the pkg files.
	 */
	public void updateAll(Project project, boolean includeGraphLayout){
		/*
		if (!hasBeenCheckedOut(project)){
			checkout();
		}
		else {
			updateFromRepository(project);
		}
		if (includeGraphLayout){
			updateGraphLayout();
		}
		*/
		updateGraphLayout();
		project.reReadAllGraphLayout();
	}
	
	public void shareProject(Project project){
		// TODO handle the existence of team.defs file
		importInRepository(project);
	}
	
	// public methods end
	
	
	
	private static class CodeFileFilter implements FilenameFilter {

		boolean includePkgFiles;
		
		public CodeFileFilter(){
			this(false);
		}
		
		public CodeFileFilter(boolean includePkgFiles){
			this.includePkgFiles = includePkgFiles;
		}
		/**
		 * Determins which files should be included
		 * @param dir the directory in which the file was found.
		 * @param name the name of the file.
		 */
		public boolean accept(File dir, String name) {
			boolean result = true;
			if (name.equals("CVS") || dir.getName().equals("CVS")){
				result = false;
			}
			if (name.equals("CVSROOT") || dir.getName().equalsIgnoreCase("CVSROOT")){
				result = false;
			}
			
			/* when a package is first created. pkg files should be
			 * added and committed. If we don't, BlueJ can't know which folders
			 * are packages
			 */ 
			if (!includePkgFiles && name.equals("bluej.pkg")){
				result = false;
			}
			if (name.equals("bluej.pkh")){
				result = false;
			}	
			if (name.equals("team.cfg")){
				result = false;
			}	
			if (getFileType(name).equals("class")){
				result = false;
			}
			if (getFileType(name).equals("ctxt")){
				result = false;
			}
			if (name.charAt(name.length() -1) == '~'){
				result = false;
			}
			if (name.charAt(name.length() -1) == '#'){
				result = false;
			}
			if (name.endsWith("#backup")){
				result = false;
			}
			if (result) {
				//System.out.println("Repository:509 accepted: " + name + " in " + dir.getAbsolutePath());
			}else{
				//System.out.println("Repository:509 rejected: " + name + " in " + dir.getAbsolutePath());
			}
			return result;
		}
		
		private String getFileType(String filename) {
			int lastDotIndex = filename.lastIndexOf('.');
			if (lastDotIndex > -1 && lastDotIndex < filename.length()){
				//System.err.println("at bluej.groupwork.Repository.getFileType(Repository.java:356)");
				return filename.substring(lastDotIndex + 1);
			}
			return "";
		}
	}
}

