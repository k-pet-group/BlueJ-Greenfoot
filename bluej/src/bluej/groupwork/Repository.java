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
import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.netbeans.lib.cvsclient.command.add.AddCommand;
import org.netbeans.lib.cvsclient.command.checkout.CheckoutCommand;
import org.netbeans.lib.cvsclient.command.commit.CommitCommand;
import org.netbeans.lib.cvsclient.command.importcmd.ImportCommand;
import org.netbeans.lib.cvsclient.command.remove.RemoveCommand;
import org.netbeans.lib.cvsclient.command.update.UpdateCommand;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.connection.ConnectionFactory;
import org.netbeans.lib.cvsclient.connection.StandardScrambler;
import org.netbeans.lib.cvsclient.event.MessageEvent;
import org.netbeans.lib.cvsclient.event.TerminationEvent;


import bluej.pkgmgr.Package;
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
	private String password;
	
	private boolean reconnectBetweenCommands = true;
	
	//CVS specific members
	private CVSRoot cvsroot; 
	private GlobalOptions globalOptions;
	private Client client;
	private Project project;
	private Connection connection;
	
	private boolean printCommand = true;
	
	// **** static declerations 
	
	/**
	 * Create an instance of a Repository associated with the project.
	 * The Repository class will try to read the setup file team.defs from the
	 * top level directory in the project.
	 * @param the project to which the Repository will be associated. 
	 */
	public static Repository getInstance(Project project) {
		if (project == null){
			throw new IllegalArgumentException("Project must not be null");
		}
		if (!isGroupProject(project)){
			return null;
		}
		Repository repository = new Repository(project);
		repository.readSetupFile();
		repository.setupClient();
		//System.setProperty("javacvs.multiple_commands_warning", "false");
		return repository;
	}
	
	/**
	 * Determine if project is a team project. 
	 * The method will look for the existence of the team configuration file
	 * team.defs
	 * @param project the project to investigate
	 * @return true if the project is a team project
	 */
	private static boolean isGroupProject(Project project){
		String cfgFilePath = project.getProjectDir() + "/team.defs";
		File cfgFile = new File(cfgFilePath);
		return cfgFile.isFile();
	}
	
	/**
	 * Determine if the project has been checked out.
	 * The method will look for the existence of a CVS directory in the top 
	 * level project directory
	 * @param project
	 * @return true if the project has been checked out.
	 */
	private static boolean hasBeenCheckedOut(Project project){
		File cvsDir = new File(project.getProjectDir() + "/CVS");
		return cvsDir.isDirectory();
	}
	
	/**
	 * Get an array of Files that resides in the project folders.
	 * @param project the project
	 * @return List of Files 
	 */
	private static List getFilesInProject(Project project, boolean includePkgFiles){
		File projectDir = project.getProjectDir();
		List files = new LinkedList();
		traverseDirsForFiles(files, projectDir, includePkgFiles);
		return files;
	}
	
	/**
	 * Get an array of Files containing the directories that exist in the 
	 * project which has not been added to CVS.
	 * @param project
	 * @return array of Files
	 */
	private static List getDirsInProjectWhichNeedToBeAdded(Project project) {
		File projectDir = project.getProjectDir();
		List files = new LinkedList();
		traverseDirsForDirs(files, projectDir);
		return files;
	}
	
	/**
	 * Traverse the directory tree starting in dir an add all the encountered 
	 * files to the List allFiles. The parameter includePkgFiles determine 
	 * whether bluej.pgk files should be added to allFiles as well.
	 * @param allFiles a List to which the method will add the files it meets.
	 * @param dir the directory the search starts from
	 * @param includePkgFiles if true, bluej.pkg files are included as well.
	 */
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
	 * Traverses a directory tree and records the directories it encounters.
	 * Directories named 'CVSROOT' or 'CVS' are ignored along with directories
	 * already in CVS.
	 * @param allDirs a List to which the method will add the dirs it meets.
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
	
	/**
	 * Determine if a directory is in CVS. The method looks for a subfolder
	 * named 'CVS'
	 * @param dir the directory
	 * @return true if directory is in CVS.
	 */
	private static boolean isInCVS(File dir){
		char s = File.separatorChar;
		File cvsDir = new File(dir.getAbsolutePath() + s + "CVS");
		return cvsDir.exists();
	}
	
	/**
	 * Convert a List of Files to an array of Files.
	 * @param fileList
	 */
	private static File[] listToFileArray(List fileList) {
		File[] files = new File[fileList.size()];
		int j = 0;
		for (Iterator i = fileList.iterator(); i.hasNext(); ) {
			File file = (File) i.next();
			files[j++] = file;
		}
		return files;	
	}
	
	// ** static declaration end
	
	/**
	 * private constructor
	 */
	private Repository(Project project){
		this.project = project;
		globalOptions = new GlobalOptions();
		globalOptions.setUseGzip(false);
	}
	
	// setup start
	
	/**
	 * Create the connection, open it and associate it with the client.
	 * @throws AuthenticationException
	 * @throws CommandAbortedException
	 * @throws AuthenticationException
	 * @throws CommandAbortedException
	 */
	private void setupConnection() throws CommandAbortedException, AuthenticationException {
		connection = ConnectionFactory.getConnection(cvsroot);     
		connection.open();
		client.setConnection(connection);
	}
	
	/**
	 * Create and setup the client. 
	 *
	 */
	private void setupClient(){
		//the client is created without a connection
		client = new Client(null, new StandardAdminHandler());
	    client.setLocalPath(project.getProjectDir().getParentFile().getAbsolutePath());
	    //client.getEventManager().addCVSListener(new BasicListener());
	    client.dontUseGzipFileHandler();
	}

	/**
	 * If the attribute 'reconnectBetweenCommands' is true the connection is 
	 * closed.
	 */
	private void disconnect(){
		if (reconnectBetweenCommands){
			try {
				client.getConnection().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Read the team setup file in the top level folder of the project and
	 * configure the cvsroot and set the globalOptions
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void readSetupFile(){
		File cfgFile = new File(project.getProjectDir() + "/team.defs");
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(cfgFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		protocol = properties.getProperty("protocol");
		server = properties.getProperty("server");
		repository = properties.getProperty("groupname");
		module = project.getProjectName();
		user = properties.getProperty("user");
		password = properties.getProperty("password");
		
		//setup cvsroot and global options
		String cvsRoot = ":" + protocol+":"+user+":"+password+"@"+server+":/CVSNT/"+repository;
		cvsroot = CVSRoot.parse(cvsRoot);
		globalOptions.setCVSRoot(cvsroot.toString());
	}

	// setup end
	
	// basic cvs commands begin
	
	/**
	 * Checkout project from repostitory to local project. 
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	public void checkout() throws CommandAbortedException, CommandException, AuthenticationException{
		setupConnection();
		CheckoutCommand checkoutCommand = new CheckoutCommand(true, module);
		checkoutCommand.setRecursive(true);
		checkoutCommand.setPruneDirectories(false);
		BasicServerResponse basicServerResponse = new BasicServerResponse();
		client.getEventManager().addCVSListener(basicServerResponse);
		
		client.executeCommand(checkoutCommand, globalOptions);
		
		basicServerResponse.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(basicServerResponse);
		disconnect();
		project.reloadAll();
	}
	
	/**
	 * Add an array of Files to the repository. 
	 * @param files the files to add
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private void addToRepository(List files) throws CommandAbortedException, CommandException, AuthenticationException{
		if (files.size() < 1){
			return;
		}
		setupConnection();
		AddCommand addCommand = new AddCommand();
		addCommand.setFiles(listToFileArray(files));
		BasicServerResponse basicServerResponse = new BasicServerResponse();
		client.getEventManager().addCVSListener(basicServerResponse);
		printCommand(addCommand);
		
		client.executeCommand(addCommand, globalOptions);

		basicServerResponse.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(basicServerResponse);
		disconnect();
	}
	
	/**
	 * Commit an array of Files to the repository
	 * @param files the files to commit
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private BasicServerResponse commitToRepository(List files) throws CommandAbortedException, CommandException, AuthenticationException{
		if (files.size() < 1){
			return new BasicServerResponse();
		}
		setupConnection();
		CommitCommand commitCommand = new CommitCommand();
		commitCommand.setMessage("");
		commitCommand.setFiles(listToFileArray(files));
		BasicServerResponse basicServerResponse = new BasicServerResponse();
		client.getEventManager().addCVSListener(basicServerResponse);
		printCommand(commitCommand);
		
		client.executeCommand(commitCommand, globalOptions);
		
		basicServerResponse.waitForExecutionToFinish();
		if (basicServerResponse.isError()){
			System.out.println("basicServerResponse.getMessage(): " +basicServerResponse.getMessage());
		}
		client.getEventManager().removeCVSListener(basicServerResponse);
		disconnect();
		return basicServerResponse;
	}
	
	/**
	 * Update all files from repository except bluej.pkg files
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private UpdateServerResponse updateFromRepository() throws CommandAbortedException, CommandException, AuthenticationException{
		setupConnection();
		UpdateCommand command = new UpdateCommand();
		command.setCleanCopy(false);
		command.setRecursive(true);
		command.setBuildDirectories(true);
		command.setPruneDirectories(false);
		command.setCVSCommand('I', Package.pkgfileName);// ignore bluej.pkg files
		UpdateServerResponse updateServerResponse = new UpdateServerResponse();
		client.getEventManager().addCVSListener(updateServerResponse);
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		printCommand(command);
		
		client.executeCommand(command, globalOptions);
		
		updateServerResponse.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(updateServerResponse);
		disconnect();
		return updateServerResponse;
	}
	
	/**
	 * Import a project into the repository. The project will be put in the
	 * repository in a module named after the project.
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private void importInRepository() throws CommandAbortedException, CommandException, AuthenticationException{
		setupConnection();
		
		ImportCommand importCommand = new ImportCommand();
		//importCommand.addWrapper(localPath + "/TestProject/simplePackage/SimpleClass.java", KeywordSubstitutionOptions.DEFAULT);
		//importCommand.addWrapper(localPath + "/TestProject/simplePackage/added.txt", KeywordSubstitutionOptions.DEFAULT);
		importCommand.setModule(project.getProjectName());
		importCommand.setReleaseTag("init");
		importCommand.setLogMessage("logMessage");
		importCommand.setVendorTag("vendor");
		BasicServerResponse synchListener = new BasicServerResponse();
		client.getEventManager().addCVSListener(synchListener);
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		printCommand(importCommand);
		
		client.executeCommand(importCommand, globalOptions);
		
		synchListener.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(synchListener);
		disconnect();
	}
	
	/**
	 * Remove the files in an array of Files from the repository.
	 * @param files the files to remove.
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private void removeFromRepository(List files) throws CommandAbortedException, CommandException, AuthenticationException{
		if (files.size() < 1){
			return;
		}
		setupConnection();
		RemoveCommand removeCommand = new RemoveCommand ();
		removeCommand.setFiles(listToFileArray(files));
		BasicServerResponse basicServerResponse = new BasicServerResponse();
		client.getEventManager().addCVSListener(basicServerResponse);
		printCommand(removeCommand);
		
		client.executeCommand(removeCommand, globalOptions);
		
		basicServerResponse.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(basicServerResponse);
		disconnect();
	}
	
	// basic cvs commands end
	
	// util methods begin
	
	/**
	 * Get a UpdateServerResponse that would result from an update. No changes
	 * are made to the repository or the local copy.
	 * @return List of UpdateResults
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private UpdateServerResponse getUpdateListener() throws CommandAbortedException, CommandException, AuthenticationException{
		setupConnection();
		UpdateCommand command = new UpdateCommand();
		UpdateServerResponse updateServerResponse = new UpdateServerResponse();
		GlobalOptions globalOptions = new GlobalOptions();
		
		command.setRecursive(true);
		command.setBuildDirectories(true);
		command.setPruneDirectories(false);
		command.setCleanCopy(false);
		globalOptions.setCVSRoot(cvsroot.toString());
		globalOptions.setCVSCommand('n',"");//Don't change any files
		globalOptions.setCVSCommand('q',"");// be quiet
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		client.getEventManager().addCVSListener(updateServerResponse);
		
		client.executeCommand(command, globalOptions);
		
		client.getEventManager().removeCVSListener(updateServerResponse);
		disconnect();
		return updateServerResponse;
	}
	
	/**
	 * Get an array of files in the sandbox that needs to be added to repository
	 * @return array of files
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private List getFilesInSandboxWhichNeedToBeAdded() throws CommandAbortedException, CommandException, AuthenticationException{
		List updateResults = getUpdateListener().getUnknown();
		FilenameFilter filter = new CodeFileFilter();
		List files = new LinkedList();
		for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			File file = updateResultToFile(updateResult);
			if (filter.accept(new File(file.getParent()), file.getName())){
				files.add(file);
			}
		}
		return files;
	}
	
	/**
	 * Get an array of files that has been deleted from the sandbox but not from
	 * the repository.
	 * @return List of Files
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private List getRemovedFilesInSandbox() throws CommandAbortedException, CommandException, AuthenticationException{
		List updateResults = getUpdateListener().getUpdated();
		List files = new LinkedList();
		for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			File file = updateResultToFile(updateResult);
			if (!file.exists()) {
				files.add(file);
			}
		}
		return files;
	}
	
	/**
	 * Convert an UpdateResult to a File
	 * @param updateResult
	 * @return a File
	 */
	private File updateResultToFile(UpdateResult updateResult){
		String str;
		char s = File.separatorChar;
		str = project.getProjectDir().getAbsolutePath() + s + updateResult.getFilename();
		File file = new File(str);
		return file;
	}
	
	/**
	 * Look through the List of dirs and return a List of bluej.pkg files found
	 * @param dirs List of dirs that the method will look through
	 * @return a List of the found bluej.pkg files
	 */
	private List findPkgFiles(List dirs){
		File[] pkgFiles;
		List pkgFilesList = new LinkedList();
		char s = File.separatorChar;
		for (Iterator i = dirs.iterator(); i.hasNext(); ) {
			File pkgFile = new File(((File)i.next()).getAbsolutePath() + s + Package.pkgfileName);
			if (pkgFile.exists()){
				pkgFilesList.add(pkgFile);
			}
		}
		return pkgFilesList;
	}
	
	/**
	 * Update the layout of the graphs from the ones in the 
	 * repository. 
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 *
	 */
	public void updateGraphLayout() throws CommandAbortedException, CommandException, AuthenticationException{
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
		BasicServerResponse basicServerResponse = new BasicServerResponse();
		client.getEventManager().addCVSListener(basicServerResponse);
		client.setLocalPath(project.getProjectDir().getAbsolutePath());
		//System.out.println("Cvs command: " + command.getCVSCommand());
		
		client.executeCommand(command, globalOptions);
		
		basicServerResponse.waitForExecutionToFinish();
		client.getEventManager().removeCVSListener(basicServerResponse);
		disconnect();
		
	}
	
	/**
	 * Get an array of files containing all the bluej.pkg files in the repository
	 * @return array of files
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	private File[] updatePkgFilesFromRepository() throws CommandAbortedException, CommandException, AuthenticationException {
		List updateResults = getUpdateListener().getUpdateResults();
		List pkgFiles = new LinkedList();
		for (Iterator i = updateResults.iterator(); i.hasNext();) {
			UpdateResult updateResult = (UpdateResult) i.next();
			if (updateResult.getFilename().endsWith(Package.pkgfileName)){
				pkgFiles.add(updateResultToFile(updateResult));
				//System.out.println("getPkgFilesInRepository:498 added " + updateResultToFile(updateResult).getAbsolutePath());
			}
		}
		File[] pkgArray = listToFileArray(pkgFiles);
		return pkgArray;
	}
	
	/**
	 * if the attribute 'printCommand' the command is printed as it would look
	 * on the command line
	 * @param command the command to print
	 */
	private void printCommand(Command command){
		if (printCommand){
			System.out.println(command.getCVSCommand());
		}
	}
	// util methods end

	// public methods begin
	
	

	public void printExperiments(Project project){
		System.out.println("====Files in project===========");
		List files = getFilesInProject(project, false);
		Set set = null;
		/*for(int i=0; i < files.size(); i++ ){
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
	 * The booelean includePkgFiles determins whether pkg files are included in 
	 * the commit. One exception to this is newly created packages. They always 
	 * have their pkg files committed. Otherwise bluej won't know the difference
	 * between simple directories and bluej packages.
	 * 
	 * @param project
	 * @param includePkgFiles if true, pkg files are included in the commit.
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 */
	public BasicServerResponse commitAll(boolean includePkgFiles) throws CommandAbortedException, CommandException, AuthenticationException{
		BasicServerResponse basicServerResponse;
		// add and commit new directories
		List dirs = getDirsInProjectWhichNeedToBeAdded(project);
		addToRepository(dirs);
		commitToRepository(dirs);// this commit needs to be here. find_pkgfiles
								// depend on directories to be in the repositroy
		
		//add and commit the bluej.pkg files in new dirs that were bluej packages
		List addPkgFiles = findPkgFiles(dirs);
		project.saveAllGraphLayout(); // make BlueJ save the pkg files
		addToRepository(addPkgFiles);
		commitToRepository(addPkgFiles);
		
		//add new files
		addToRepository(getFilesInSandboxWhichNeedToBeAdded());
		List addFiles = getFilesInProject(project, includePkgFiles);
		
		//remove files from repository that has been removed from sandbox
		List removeFiles = getRemovedFilesInSandbox();
		removeFromRepository(removeFiles);
		
		//commit the files that needed to be added or removed
		removeFiles.addAll(addFiles);
		basicServerResponse = commitToRepository(removeFiles);
		return basicServerResponse;
	}
	
	/**
	 * Get all changes from repository except the pkg files that determine the
	 * layout of the graph.
	 * @param includeGraphLayout should the update include the pkg files.
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 * @return UpdateServerResponse if an update was performed
	 */
	public UpdateServerResponse updateAll(boolean includeGraphLayout) throws CommandAbortedException, CommandException, AuthenticationException{
		UpdateServerResponse updateServerResponse;
		if (!hasBeenCheckedOut(project)){
			checkout();
			updateServerResponse = new UpdateServerResponse();// empty UpdateServerResponse
		}
		else {
			updateServerResponse = updateFromRepository();
		}
		if (includeGraphLayout){
			updateGraphLayout();
			project.reReadAllGraphLayout();
		}
		project.reloadAll();
		return updateServerResponse;
	}
	
	/**
	 * Put the project in the repository
	 * @throws AuthenticationException
	 * @throws CommandException
	 * @throws CommandAbortedException
	 *
	 */
	public void shareProject() throws CommandAbortedException, CommandException, AuthenticationException{
		// TODO handle the existence of team.defs file
		importInRepository();
	}
	
	// public methods end
	
	
	/**
	 * A FilenameFilter that filters out files that reside in a directory named
	 * 'CVS' or 'CVSROOT'
	 * It also filters out files  
	 *
	 */
	private static class CodeFileFilter implements FilenameFilter {

		boolean includePkgFiles;
		
		/**
		 * Construct a filter that doesn't accept pkg files
		 *
		 */
		public CodeFileFilter(){
			this(false);
		}
		
		/**
		 * Construct a filter.
		 * @param includePkgFiles if true, pkg files are accepted
		 */
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
			if (!includePkgFiles && name.equals(Package.pkgfileName)){
				result = false;
			}
			if (name.equals(Package.pkgfileBackup)){
				result = false;
			}	
			if (name.equals("team.defs")){
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
			if (name.startsWith(".#")){
				return false;
			}
			if (result) {
				//System.out.println("Repository:509 accepted: " + name + " in " + dir.getAbsolutePath());
			}else{
				//System.out.println("Repository:509 rejected: " + name + " in " + dir.getAbsolutePath());
			}
			return result;
		}
		
		/**
		 * Get the type of a file
		 * @param filename the name of the file
		 * @return a string with the type of the file.
		 */
		private String getFileType(String filename) {
			int lastDotIndex = filename.lastIndexOf('.');
			if (lastDotIndex > -1 && lastDotIndex < filename.length()){
				return filename.substring(lastDotIndex + 1);
			}
			return "";
		}
	}
}

