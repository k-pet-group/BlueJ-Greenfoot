package bluej.groupwork;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.net.SocketFactory;

import org.netbeans.lib.cvsclient.CVSRoot;
import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.netbeans.lib.cvsclient.command.KeywordSubstitutionOptions;
import org.netbeans.lib.cvsclient.command.add.AddCommand;
import org.netbeans.lib.cvsclient.command.checkout.CheckoutCommand;
import org.netbeans.lib.cvsclient.command.commit.CommitCommand;
import org.netbeans.lib.cvsclient.command.importcmd.ImportCommand;
import org.netbeans.lib.cvsclient.command.log.LogCommand;
import org.netbeans.lib.cvsclient.command.remove.RemoveCommand;
import org.netbeans.lib.cvsclient.command.status.StatusCommand;
import org.netbeans.lib.cvsclient.command.status.StatusInformation;
import org.netbeans.lib.cvsclient.command.update.UpdateCommand;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.connection.PServerConnection;
import org.netbeans.lib.cvsclient.file.FileStatus;
import org.netbeans.modules.versioning.system.cvss.SSHConnection;

import bluej.utility.Debug;


/**
 * This class handles communication with the repository.
 *
 * @author fisker
 * @version $Id: Repository.java 4850 2007-03-19 04:34:02Z davmac $
 */
public class Repository
{
    /*
     * A little about CVS.
     * 
     * CVS "status" command refuses to give any information about files which
     * exist in the repository but not locally, *unless* you specify their name
     * and path as an argument (AND you don't also specify a directory argument).
     * So this means you need some way of finding the names first.
     * 
     * It turns out we can find out the names of files which only exist remotely
     * by using "cvs -n update". However, this won't recurse into directories which
     * only exist remotely, though it does print a warning for each one that it
     * comes across at the current level.
     * 
     * So, the overall solution is to perform "cvs -n update" recursively each
     * time we find out about a new directory. We can pretend (by using a
     * MildManneredAdminHandler instead of the StandardAdminHandler) that the
     * directory does exist locally (i.e. has been checked out) while we do this.
     *  
     */
    
    private boolean reconnectBetweenCommands = true;

    //CVS specific members
    private CVSRoot cvsroot;
    private GlobalOptions globalOptions;
    private boolean printCommand = true;
    private File projectPath;
    private BlueJAdminHandler adminHandler;

    // ** static declaration end

    /**
     * constructor
     */
    public Repository(File projectPath, String cvsrootString, BlueJAdminHandler adminHandler)
    {
        // this.project = project;
        this.projectPath = projectPath;
        globalOptions = new GlobalOptions();
        setCvsRoot(cvsrootString);

        // The client is created without a connection
        this.adminHandler = adminHandler;
        
        // System.setProperty("javacvs.multiple_commands_warning", "false");
    }

    // **** static declerations 

    /**
     * Convert a List of Files to an array of Files.
     *
     * @param fileList
     */
    private static File[] listToFileArray(Collection fileList)
    {
        File[] files = new File[fileList.size()];
        int j = 0;

        for (Iterator i = fileList.iterator(); i.hasNext();) {
            File file = (File) i.next();
            files[j++] = file;
        }

        return files;
    }

    // setup start

    /**
     * Set the repository root location (including server, protocol, username etc)
     * Used at setup and when the cvsroot changes due to user interaction.
     */
    private void setCvsRoot(String cvsrootString)
    {
        cvsroot = CVSRoot.parse(cvsrootString);
        globalOptions.setCVSRoot(cvsroot.toString());
    }

    /**
     * Create the connection, open it and associate it with the client.
     *
     * @throws AuthenticationException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws CommandAbortedException
     * @throws InvalidCvsRootException
     */
    private void setupConnection(Client client)
        throws CommandAbortedException, AuthenticationException, 
            InvalidCvsRootException
    {
        Connection connection = getConnection(cvsroot);

        
        if (connection != null) {
            connection.open();
            client.setConnection(connection);
        } else {
            Debug.message("Repository.setupConnection: connection is null");
        }
    }
    
    /**
     * Get a client object, along with a connection, which can be used
     * to execute commands.
     * 
     * It's necessary to get a fresh client for each command because once
     * the client is put in the "aborted" state there's no way to undo
     * that effect.
     * 
     * @return A client
     * 
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    private BlueJCvsClient getClient() throws CommandAbortedException, AuthenticationException,
        InvalidCvsRootException
    {
        BlueJCvsClient client = new BlueJCvsClient(null, adminHandler);
        client.dontUseGzipFileHandler();
        setupConnection(client);
        return client;
    }

    /**
     * If the attribute 'reconnectBetweenCommands' is true the connection is
     * closed.
     */
    private void disconnect(Client client)
    {
        if (reconnectBetweenCommands) {
            try {
                client.getConnection().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checkout project from repostitory to local project.
     *
     * @throws CommandException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public synchronized BasicServerResponse checkout(File projectPath)
        throws CommandException, CommandAbortedException, 
            AuthenticationException, InvalidCvsRootException
    {
        Client client = getClient();
        // setupConnection();

        CheckoutCommand checkoutCommand = new CheckoutCommand(true,
                projectPath.getName());
        checkoutCommand.setRecursive(true);
        checkoutCommand.setPruneDirectories(false);

        BasicServerResponse basicServerResponse = new BasicServerResponse();
        client.getEventManager().addCVSListener(basicServerResponse);
        client.setLocalPath(projectPath.getParent());
        
        printCommand(checkoutCommand, client);
        try {
            client.executeCommand(checkoutCommand, globalOptions);
            basicServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(basicServerResponse);
            disconnect(client);
        }

        return basicServerResponse;
    }

    /**
     * Add an array of Files/directories to the repository. Parent directories
     * must be specified before the sub-directories/files they contain.
     *
     * @param files the files to add
     * @param binary true if the added files should be treated as binary
     *
     * @throws CommandException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    private BasicServerResponse addToRepository(File [] files, boolean binary)
        throws CommandException, CommandAbortedException, 
            AuthenticationException, InvalidCvsRootException
    {
        BasicServerResponse basicServerResponse = new BasicServerResponse();
        
        // If there's nothing to add, return immediately
        if (files.length < 1) {
            basicServerResponse.commandTerminated(null);
            return basicServerResponse;
        }

        // setupConnection();
        Client client = getClient();

        AddCommand addCommand = new AddCommand();
        addCommand.setFiles(files);
        
        KeywordSubstitutionOptions kso;
        if (binary) {
            kso = KeywordSubstitutionOptions.BINARY;
        }
        else {
            kso = KeywordSubstitutionOptions.DEFAULT;
        }
        addCommand.setKeywordSubst(kso);

        client.getEventManager().addCVSListener(basicServerResponse);
        client.setLocalPath(projectPath.toString());

        printCommand(addCommand, client);
        try {
            client.executeCommand(addCommand, globalOptions);
            basicServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(basicServerResponse);
            disconnect(client);
        }
        
        return basicServerResponse;
    }

    /**
     * Commit an array of Files to the repository
     *
     * @param files the files to commit
     *
     * @throws CommandException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    private BasicServerResponse commitToRepository(Collection files, String comment)
        throws CommandException, CommandAbortedException, 
            AuthenticationException, InvalidCvsRootException
    {
        BasicServerResponse basicServerResponse = new BasicServerResponse();
        
        if (files.size() < 1) {
            basicServerResponse.commandTerminated(null);
            return basicServerResponse;
        }

        // setupConnection();
        Client client = getClient();

        CommitCommand commitCommand = new CommitCommand();
        commitCommand.setMessage(comment);
        commitCommand.setFiles(listToFileArray(files));

        client.getEventManager().addCVSListener(basicServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());

        printCommand(commitCommand, client);
        try {
            client.executeCommand(commitCommand, globalOptions);
            basicServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(basicServerResponse);
            disconnect(client);
        }

        return basicServerResponse;
    }

    /**
     * Import a project into the repository. The project will be put in the
     * repository in a module named after the project.
     *
     * @throws CommandException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    private BasicServerResponse importInRepository()
        throws CommandException, CommandAbortedException, 
            AuthenticationException, InvalidCvsRootException
    {
        // setupConnection();
        Client client = getClient();

        ImportCommand importCommand = new ImportCommand();

        //importCommand.addWrapper(localPath + "/TestProject/simplePackage/SimpleClass.java", KeywordSubstitutionOptions.DEFAULT);
        //importCommand.addWrapper(localPath + "/TestProject/simplePackage/added.txt", KeywordSubstitutionOptions.DEFAULT);
        importCommand.setModule(projectPath.getName());
        importCommand.setReleaseTag("init");
        importCommand.setLogMessage("logMessage");
        importCommand.setVendorTag("vendor");
        
        // Ignore all files during checkout. Then we can just commit them in-place.
        importCommand.addIgnoredFile("*");

        BasicServerResponse basicServerResponse = new BasicServerResponse();
        client.getEventManager().addCVSListener(basicServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());
        printCommand(importCommand, client);

        try {
            client.executeCommand(importCommand, globalOptions);
            basicServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(basicServerResponse);
            disconnect(client);
        }

        return basicServerResponse;
    }

    /**
     * Remove the files in an array of Files from the repository.
     *
     * @param files the files to remove.
     *
     * @throws CommandException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    private BasicServerResponse removeFromRepository(Collection files)
        throws CommandException, CommandAbortedException, 
            AuthenticationException, InvalidCvsRootException
    {
        BasicServerResponse basicServerResponse = new BasicServerResponse();
        
        if (files.size() < 1) {
            basicServerResponse.commandTerminated(null);
            return basicServerResponse;
        }

        // setupConnection();
        Client client = getClient();

        RemoveCommand removeCommand = new RemoveCommand();
        removeCommand.setFiles(listToFileArray(files));

        client.getEventManager().addCVSListener(basicServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());
        
        printCommand(removeCommand, client);
        try {
            client.executeCommand(removeCommand, globalOptions);
            basicServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(basicServerResponse);
            disconnect(client);
        }
        
        return basicServerResponse;
    }

    // basic cvs commands end
    // util methods begin
    
    /**
     * Get the response of a "dummy run" update command. The response contains a
     * list of files, which need to be updated or are locally modified etc.
     */
    private UpdateServerResponse getUpdateServerResponse(String path)
        throws CommandException, CommandAbortedException, 
            AuthenticationException, InvalidCvsRootException
    {
        //setupConnection();
        Client client = getClient();

        UpdateCommand updateCommand = new UpdateCommand();
        UpdateServerResponse updateServerResponse = new UpdateServerResponse(null, null);
        GlobalOptions globalOptions = new GlobalOptions();

        updateCommand.setRecursive(true);
        updateCommand.setBuildDirectories(true);
        updateCommand.setPruneDirectories(true);
        updateCommand.setCleanCopy(false);
        globalOptions.setCVSRoot(cvsroot.toString());

        globalOptions.setDoNoChanges(true); // -n
        globalOptions.setModeratelyQuiet(true); // -q
        client.setLocalPath(path);
        client.getEventManager().addCVSListener(updateServerResponse);

        //System.out.println("dir: " + client.getLocalPath());
        //System.out.println("globalOptions: " + globalOptions.getCVSCommand());
        printCommand(updateCommand, client);

        //Debug.message("Update command = " + updateCommand.getCVSCommand());
        try {
            client.executeCommand(updateCommand, globalOptions);
            updateServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(updateServerResponse);
            disconnect(client);
        }

        return updateServerResponse;
    }
    
    /**
     * if the attribute 'printCommand' the command is printed as it would look
     * on the command line
     *
     * @param command the command to print
     */
    private void printCommand(Command command, Client client)
    {
        if (printCommand) {
            System.out.println("cvsCommand: " + command.getCVSCommand() +
                " localpath: " + client.getLocalPath());
        }
    }

    /**
     * Check whether a directory is under CVS control.
     */
    private boolean isDirectoryUnderCVS(File dir)
    {
        String apath = adminHandler.getMetaDataPath(dir.getAbsolutePath());
        dir = new File(apath);
        return (new File(dir, "CVS").isDirectory());
    }
    
    /**
     * Commits the files and directories in the project. If files or dirs need
     * to be added first, they are added. The booelean includePkgFiles
     * determins whether pkg files are included in  the commit. One exception
     * to this is newly created packages. They always  have their pkg files
     * committed. Otherwise bluej won't know the difference between simple
     * directories and bluej packages.
     *
     * @param newFiles Files to be committed which are not presently in the repository
     *                 (text files only)
     * @param binaryNewFiles Files to be committed which are not presently in the
     *                       repository and which are to be treated as binary
     * @param deletedFiles Files which have been deleted locally but which exist
     *                     in the latest version in the repository 
     * @param files  All files to be committed (including all in newFiles, binaryNewFiles,
     *               and deletedFiles, as well as any other files to be committed)
     * @param commitComment  The comment for this commit
     *
     * @throws CommandAbortedException
     * @throws CommandException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public synchronized BasicServerResponse commitAll(Set newFiles, Set binaryNewFiles,
            Set deletedFiles, Set files, String commitComment)
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException
    {
        // First we need to do "cvs add" to put files and directories
        // under version control if they are not already. Start by building
        // a list of directories to add.
        
        // Note, we need to use a LinkedHashSet to preserve order.
        Set dirs = new LinkedHashSet();
        LinkedList stack = new LinkedList();
        for (Iterator i = newFiles.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            
            File parent = file.getParentFile();
            while (! isDirectoryUnderCVS(parent) && ! dirs.contains(parent)) {
                stack.addLast(parent);
                if (parent.equals(projectPath)) {
                    break;
                }
                parent = parent.getParentFile();
            }
            while (! stack.isEmpty()) {
                dirs.add(stack.removeLast());
            }
        }
        
        // The list of directories must include those containing binary files
        for (Iterator i = binaryNewFiles.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            
            File parent = file.getParentFile();
            while (! isDirectoryUnderCVS(parent) && ! dirs.contains(parent)) {
                stack.addLast(parent);
                if (parent.equals(projectPath)) {
                    break;
                }
                parent = parent.getParentFile();
            }
            while (! stack.isEmpty()) {
                dirs.add(stack.removeLast());
            }
        }
        
        // we also add the files which need to be added
        dirs.addAll(newFiles);
        
        // "cvs remove" files which need to be removed
        BasicServerResponse basicServerResponse;
        try {
            adminHandler.setMildManneredMode(true);
            basicServerResponse = removeFromRepository(deletedFiles);
            if (basicServerResponse.isError()) {
                return basicServerResponse;
            }
        }
        finally {
            adminHandler.setMildManneredMode(false);
        }
        
        // "cvs add" new directories and text files
        basicServerResponse = addToRepository(listToFileArray(dirs), false);
        if (basicServerResponse.isError()) {
            return basicServerResponse;
        }
        
        // add the binary files
        basicServerResponse = addToRepository(listToFileArray(binaryNewFiles), true);
        if (basicServerResponse.isError()) {
            return basicServerResponse;
        }
        
        // Now perform the commit.
        basicServerResponse = commitToRepository(files, commitComment);
        return basicServerResponse;
    }

    /**
     * Get all changes from repository except the pkg files that determine the
     * layout of the graph.
     *
     * @param includeGraphLayout should the update include the pkg files.
     *
     * @return UpdateServerResponse if an update was performed
     *
     * @throws CommandAbortedException
     * @throws CommandException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public synchronized UpdateServerResponse updateAll(UpdateListener listener)
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException
    {
        BlueJCvsClient client = getClient();

        UpdateCommand command = new UpdateCommand();
        command.setCleanCopy(false);
        command.setRecursive(true);
        command.setBuildDirectories(true);
        command.setPruneDirectories(true);

        UpdateServerResponse updateServerResponse = new UpdateServerResponse(listener,
                client);
        client.getEventManager().addCVSListener(updateServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());
        printCommand(command, client);

        try {
            client.executeCommand(command, globalOptions);
            updateServerResponse.waitForExecutionToFinish();
        }
        finally {
            // restore previous excludes setting
            client.getEventManager().removeCVSListener(updateServerResponse);
            disconnect(client);
        }

        updateServerResponse.setConflictMap(client.getConflictFiles());
        return updateServerResponse;
    }

    /**
     * Put the project in the repository and make local copy a sandbox
     *
     * @throws CommandAbortedException
     * @throws CommandException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public synchronized BasicServerResponse shareProject()
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException
    {
        BasicServerResponse importResponse;
        BasicServerResponse checkoutResponse;
        importResponse = importInRepository();

        if (importResponse.isError()) {
            return importResponse;
        } else {
            checkoutResponse = checkout(projectPath);
        }

        return checkoutResponse;
    }

    /**
     * See if we can connect to the server specified by the parameter
     * cvsRootStr
     *
     * @param cvsrootStr
     *
     * @return true if the connection could be made
     */
    public static boolean validateConnection(String cvsrootStr)
    {
        boolean status = false;
        Connection connection = null;

        try {
            CVSRoot cvsroot = CVSRoot.parse(cvsrootStr);
            connection = getConnection(cvsroot);

            if (connection != null) {
                connection.verify();
                status = true;
            }
        } catch (AuthenticationException e) {
            // problem verifying connection
            status = false;
        } catch (IllegalArgumentException iae) {
            // problem parsing CVSRoot
            status = false;
        }

        return status;
    }

    /**
     * Get the applicable Connection type for the given CVSRoot object.
     * At present this can only be either pserver or ext using internal ssh.
     * @return a Connection object for the cvsroot or null if invalid
     */
    private static Connection getConnection(CVSRoot cvsRoot)
    {
        SocketFactory socketFactory = SocketFactory.getDefault();

        if (CVSRoot.METHOD_EXT.equals(cvsRoot.getMethod())) {
            // set port to 22 unless it is already set to something other than 0
            int port = 22;

            if (cvsRoot.getPort() != 0) {
                port = cvsRoot.getPort();
            }

            SSHConnection sshConnection = new SSHConnection(socketFactory,
                    cvsRoot.getHostName(), port, cvsRoot.getUserName(),
                    cvsRoot.getPassword());
            sshConnection.setRepository(cvsRoot.getRepository());

            return sshConnection;
        } else if (CVSRoot.METHOD_PSERVER.equals(cvsRoot.getMethod())) {
            PServerConnection pConnection = new PServerConnection(cvsRoot, socketFactory);

            //pServerConnection.setRepository(cvsRoot.getRepository());
            return pConnection;
        }

        return null;
    }

    /**
     * Get a list of files which are in the repository, but which are
     * not in the local project. This includes both files which have been
     * locally deleted, and files which have been added to the repository
     * from another location.
     * 
     * @param remoteDirs  This set will have all remote directories which
     *                    are found added to it.
     * 
     * @throws InvalidCvsRootException
     * @throws AuthenticationException
     * @throws CommandException
     */
    public synchronized List getRemoteFiles(Set remoteDirs) throws InvalidCvsRootException,
        AuthenticationException, CommandException
    {
        List files = new LinkedList();
        adminHandler.setMildManneredMode(true);
        try {
            getRemoteFiles(files, projectPath, remoteDirs, false);
        }
        finally {
            adminHandler.setMildManneredMode(false);
        }
        return files;
    }
    
    /**
     * Find the remote directories which also exist locally, but are not
     * locally under version control.
     */
    public synchronized Set getRemoteDirs() throws InvalidCvsRootException,
        AuthenticationException, CommandException
    {
        adminHandler.setMildManneredMode(true);
        Set remoteDirs = new HashSet();
        try {
            getRemoteFiles(null, projectPath, remoteDirs, true);
        }
        finally {
            adminHandler.setMildManneredMode(false);
        }
        return remoteDirs; 
    }
    
    /**
     * Get a list of files which are in the repository, but which are
     * not in the local project. This includes both files which have been
     * locally deleted, and files which have been added to the repository
     * from another location.
     * 
     * Throws CommandAbortedException if command aborted.
     * 
     * @param files  the list to store the discovered files in (may be null)
     * @param path  the path to look at
     * @param remoteDirs  a set to store all encountered remote directories in
     * @param localDirs  whether to only look at directories which also exist locally
     * 
     * @throws InvalidCvsRootException
     * @throws AuthenticationException
     * @throws CommandException
     */
    private void getRemoteFiles(List files, File path, Set remoteDirs, boolean localDirs)
        throws InvalidCvsRootException, AuthenticationException, CommandException
    {        
        UpdateServerResponse updateResponse = getUpdateServerResponse(path.getAbsolutePath());
        List updated = updateResponse.getUpdated();
        Iterator i = updated.iterator();
        while (i.hasNext()) {
            UpdateResult ur = (UpdateResult) i.next();
            File f = new File(path, ur.getFilename());
            remoteDirs.add(f.getParentFile());
            if (files != null && ! f.exists()) {
                files.add(f);
            }
        }
        
        // Now recurse into any new directories which were discovered.
        List newDirs = updateResponse.getNewDirectoryNames();
        i = newDirs.iterator();
        while (i.hasNext()) {
            String newDirName = i.next().toString();
            File localPath = new File(path, newDirName);
            if (! localDirs || localPath.isDirectory()) {
                remoteDirs.add(localPath);
                getRemoteFiles(files, localPath, remoteDirs, localDirs);
            }
        }
    }
    
    
    /**
     * Get status of all the given files.
     * Returns a List of TeamStatusInfo.
     * 
     * @param files  The files whose status to retrieve
     * @param remoteDirs  These are the directories which we know are in the
     *                    repository. Any file in the files list which does not
     *                    exist locally but for which the containing directory is
     *                    in the repository,  should have that directory listed here.
     * 
     * @throws CommandAbortedException
     * @throws CommandException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public synchronized List getStatus(Set files, Set remoteDirs)
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException
    {
        // setupConnection();
        Client client = getClient();
        List returnInfo = new LinkedList();

        // First, it's necessary to filter out files which are in
        // directories not in the repository. Otherwise the
        // CVS status command barfs when it hits such a file.
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            File parent = file.getParentFile();
            if (! remoteDirs.contains(parent) && ! isDirectoryUnderCVS(parent)) {
                i.remove();
                // All such files have status NEEDSADD.
                TeamStatusInfo teamInfo = new TeamStatusInfo(file,
                        "",
                        null,
                        TeamStatusInfo.STATUS_NEEDSADD);
                returnInfo.add(teamInfo);
            }
        }
        
        // Now we can use the cvs "status" command to get status on the
        // remaining files.
        
        StatusCommand statusCommand = new StatusCommand();
        statusCommand.setFiles(listToFileArray(files));

        StatusServerResponse statusServerResponse = new StatusServerResponse();
        client.getEventManager().addCVSListener(statusServerResponse);

        client.setLocalPath(projectPath.getAbsolutePath());
        printCommand(statusCommand, client);
        adminHandler.setMildManneredMode(true);
        
        try {
            client.executeCommand(statusCommand, globalOptions);
        }
        finally {
            adminHandler.setMildManneredMode(false);
            
            statusServerResponse.waitForExecutionToFinish();
            client.getEventManager().removeCVSListener(statusServerResponse);
            disconnect(client);
        }

        List statusInfo = statusServerResponse.getStatusInformation();
        for (Iterator i = statusInfo.iterator(); i.hasNext(); ) {
            StatusInformation sinfo = (StatusInformation) i.next();
            
            int status;
            FileStatus fstatus = sinfo.getStatus();
            String workingRev = sinfo.getWorkingRevision();
            if (workingRev == null || workingRev.startsWith("No entry")) {
                workingRev = "";
            }
            
            if (fstatus == FileStatus.NEEDS_CHECKOUT) {
                if (workingRev.length() > 0) {
                    status = TeamStatusInfo.STATUS_DELETED;
                }
                else {
                    status = TeamStatusInfo.STATUS_NEEDSCHECKOUT;
                }
            }
            else if (fstatus == FileStatus.NEEDS_PATCH) {
                status = TeamStatusInfo.STATUS_NEEDSUPDATE;
            }
            else if (fstatus == FileStatus.NEEDS_MERGE) {
                status = TeamStatusInfo.STATUS_NEEDSMERGE;
            }
            else if (fstatus == FileStatus.MODIFIED) {
                status = TeamStatusInfo.STATUS_NEEDSCOMMIT;
            }
            else if (fstatus == FileStatus.UNKNOWN) {
                // present locally, not present in repository
                status = TeamStatusInfo.STATUS_NEEDSADD;
            }
            else if (fstatus == FileStatus.UP_TO_DATE) {
                status = TeamStatusInfo.STATUS_UPTODATE;
            }
            else if (fstatus == FileStatus.INVALID) {
                status = TeamStatusInfo.STATUS_REMOVED;
            }
            else if (fstatus == FileStatus.UNRESOLVED_CONFLICT) {
                // This seems to indicate that there's been a local modification,
                // but the file has been removed in the repository
                status = TeamStatusInfo.STATUS_UNRESOLVED;
            }
            else if (fstatus == FileStatus.HAS_CONFLICTS) {
                // The local file still has conflicts in it from the last update.
                // The file needs to modified before this status will change.
                status = TeamStatusInfo.STATUS_HASCONFLICTS;
            }
            else if (fstatus == FileStatus.REMOVED) {
                // "cvs remove" command has been run for this file. This
                // shouldn't really happen, because we only do that just
                // before a commit.
                status = TeamStatusInfo.STATUS_NEEDSCOMMIT;
            }
            else {
                status = TeamStatusInfo.STATUS_WEIRD;
            }
            
            // There's a bug in the netbeans CVS library which can cause files
            // with the same base name (eg. multiple "bluej.pkg" files) to sometimes
            // get mixed up. However the repository file name will always
            // be correct, so we'll use that instead.
            String reposName = sinfo.getRepositoryFileName();
            if (reposName.endsWith(",v")) {
                reposName = reposName.substring(0, reposName.length() - 2);
            }
            String reposRoot = cvsroot.getRepository();
            if (! reposRoot.endsWith("/")) {
                reposRoot += "/";
            }
            reposRoot += projectPath.getName() + "/";
            String fname = reposName.substring(reposRoot.length());
            File file = new File(projectPath, fname);
            
            files.remove(file);
            TeamStatusInfo teamInfo = new TeamStatusInfo(file,
                    workingRev,
                    sinfo.getRepositoryRevision(),
                    status);
            returnInfo.add(teamInfo);
        }
        
        // Now we may have some local files left which cvs hasn't given any
        // status for...
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            TeamStatusInfo teamInfo = new TeamStatusInfo(file,
                    "",
                    null,
                    TeamStatusInfo.STATUS_REMOVED);
            returnInfo.add(teamInfo);
        }
        
        return returnInfo;
    }
    
    /**
     * Get a list of modules in the repository. The module list is returned as
     * an UpdateServerResponse - use getNewDirectories() to get the list of module
     * names.
     * 
     * @throws InvalidCvsRootException
     * @throws AuthenticationException
     * @throws CommandAbortedException
     * @throws CommandException
     */
    public synchronized UpdateServerResponse getModules(List modules) throws InvalidCvsRootException, AuthenticationException,
            CommandAbortedException, CommandException
    {
        Client client = getClient();
        client.setAdminHandler(new EmptyAdminHandler());

        CheckoutCommand checkoutCommand = new CheckoutCommand(true, ".");
        checkoutCommand.setRecursive(true);
        checkoutCommand.setPruneDirectories(false);
        globalOptions.setDoNoChanges(true);

        UpdateServerResponse updateServerResponse = new UpdateServerResponse(null, null);
        client.getEventManager().addCVSListener(updateServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());
        printCommand(checkoutCommand, client);
        
        try {
            client.executeCommand(checkoutCommand, globalOptions);
            updateServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(updateServerResponse);
            disconnect(client);
            client.setAdminHandler(adminHandler);
            globalOptions.setDoNoChanges(false);
        }
        
        List projects = updateServerResponse.getNewDirectoryNames();
        for (Iterator i = projects.iterator(); i.hasNext(); ) {
            String projectName = i.next().toString();
            if (! projectName.equals("CVSROOT")) {
                modules.add(projectName);
            }
        }
        
        return updateServerResponse;
    }
    
    /**
     * Get the locally deleted files (files which are under version control,
     * and which existed locally, but which have been deleted locally).
     * 
     * @param set  The set to store the locally deleted files in
     * @param dir  The directory to look for deleted files in (non-recursively)
     * @throws IOException
     */
    public void getLocallyDeletedFiles(Set set, File dir) throws IOException
    {
        Iterator i = adminHandler.getEntries(dir);
        while (i.hasNext()) {
            Entry entry = (Entry) i.next();
            File file = new File(dir, entry.getName());
            if (! file.exists() && ! entry.isDirectory()) {
                set.add(new File(dir, entry.getName()));
            }
        }
    }
    
    /**
     * Get the history of the repository - all commits, including file, date,
     * revision, user, and comment.
     * 
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     * @throws CommandAbortedException
     * @throws CommandException
     */
    public synchronized LogServerResponse getLogHistory()
        throws AuthenticationException, InvalidCvsRootException, CommandAbortedException,
        CommandException
    {
        Client client = getClient();

        LogCommand logCommand = new LogCommand();
        
        LogServerResponse logServerResponse = new LogServerResponse();
        client.getEventManager().addCVSListener(logServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());
        
        printCommand(logCommand, client);
        try {
            client.executeCommand(logCommand, globalOptions);
            logServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(logServerResponse);
            disconnect(client);
        }

        return logServerResponse;
    }
}
