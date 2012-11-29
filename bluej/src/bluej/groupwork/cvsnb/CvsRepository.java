/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.groupwork.cvsnb;

import java.io.File;
import java.io.FileFilter;
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
import org.netbeans.lib.cvsclient.command.update.UpdateCommand;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.connection.PServerConnection;

import bluej.groupwork.*;
import bluej.utility.Debug;
import bluej.utility.filefilter.DirectoryFilter;


/**
 * This class handles communication with the repository.
 *
 * @author fisker
 */
public class CvsRepository implements Repository
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
    private String protocol; // Only for data collection
    private GlobalOptions globalOptions;
    private boolean printCommand = true;
    private File projectPath;
    private BlueJAdminHandler adminHandler;

    // ** static declaration end

    /**
     * constructor
     */
    public CvsRepository(File projectPath, String protocol, String cvsrootString, BlueJAdminHandler adminHandler)
    {
        // this.project = project;
        this.protocol = protocol;
        this.projectPath = projectPath;
        globalOptions = new GlobalOptions();
        setCvsRoot(cvsrootString);

        // The client is created without a connection
        this.adminHandler = adminHandler;
        
        // System.setProperty("javacvs.multiple_commands_warning", "false");
    }

    // **** static declarations 

    /**
     * Convert a List of Files to an array of Files.
     *
     * @param fileList
     */
    static File[] listToFileArray(Collection<File> fileList)
    {
        File[] files = new File[fileList.size()];
        int j = 0;

        for (Iterator<File> i = fileList.iterator(); i.hasNext();) {
            File file = (File) i.next();
            files[j++] = file;
        }

        return files;
    }

    // setup start

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#setPassword(bluej.groupwork.TeamSettings)
     */
    public void setPassword(TeamSettings newSettings)
    {
    try {
    setCvsRoot(CvsProvider.makeCvsRoot(newSettings));
    }
    catch (UnsupportedSettingException e) {
    Debug.reportError("CvsRepository.setPassword error", e);
    }
    }
    
    /**
     * Get the project path for this repository.
     */
    public File getProjectPath()
    {
        return projectPath;
    }
    
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
     * Get a client object which can be used to execute commands.
     * <p>
     * It's necessary to get a fresh client for each command because once
     * the client is put in the "aborted" state there's no way to undo
     * that effect.
     * 
     * @return A client (without connection established)
     */
    BlueJCvsClient getClient()
    {
        BlueJCvsClient client = new BlueJCvsClient(null, adminHandler);
        client.dontUseGzipFileHandler();
        return client;
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
    void setupConnection(Client client)
        throws CommandAbortedException, AuthenticationException 
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

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#versionsDirectories()
     */
    public boolean versionsDirectories()
    {
        return false;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#checkout(java.io.File)
     */
    public TeamworkCommand checkout(File projectPath)
    {
        return new CvsCheckoutCommand(this, projectPath);
    }
    
    /**
     * Checkout project from repostitory to local project.
     *
     * @throws CommandException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public synchronized BasicServerResponse doCheckout(Client client, File projectPath)
        throws AuthenticationException, CommandAbortedException, CommandException
    {
        // Client client = getClient();
        // setupConnection();
        setupConnection(client);

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
    BasicServerResponse addToRepository(Client client, File [] files, boolean binary)
        throws CommandException, CommandAbortedException, 
            AuthenticationException
    {
        BasicServerResponse basicServerResponse = new BasicServerResponse();
        
        // If there's nothing to add, return immediately
        if (files.length < 1) {
            basicServerResponse.commandTerminated(null);
            return basicServerResponse;
        }

        setupConnection(client);
        // setupConnection();
        // Client client = getClient();

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
    BasicServerResponse commitToRepository(Client client, Collection<File> files, String comment)
        throws CommandException, CommandAbortedException, 
            AuthenticationException
    {
        BasicServerResponse basicServerResponse = new BasicServerResponse();
        
        if (files.size() < 1) {
            basicServerResponse.commandTerminated(null);
            return basicServerResponse;
        }

        setupConnection(client);
        // setupConnection();
        // Client client = getClient();

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
    BasicServerResponse importInRepository(Client client)
        throws CommandException, CommandAbortedException, 
            AuthenticationException
    {
        // setupConnection();
        // Client client = getClient();
        setupConnection(client);

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
    BasicServerResponse removeFromRepository(Client client, Collection<File> files)
        throws CommandException, CommandAbortedException, 
            AuthenticationException
    {
        BasicServerResponse basicServerResponse = new BasicServerResponse();
        
        if (files.size() < 1) {
            basicServerResponse.commandTerminated(null);
            return basicServerResponse;
        }

        setupConnection(client);
        // setupConnection();
        // Client client = getClient();

        RemoveCommand removeCommand = new RemoveCommand();
        removeCommand.setFiles(listToFileArray(files));

        client.getEventManager().addCVSListener(basicServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());
        
        printCommand(removeCommand, client);
        try {
            adminHandler.setMildManneredMode(true);
            client.executeCommand(removeCommand, globalOptions);
            basicServerResponse.waitForExecutionToFinish();
        }
        finally {
            client.getEventManager().removeCVSListener(basicServerResponse);
            disconnect(client);
            adminHandler.setMildManneredMode(false);
        }
        
        return basicServerResponse;
    }

    // basic cvs commands end
    // util methods begin
    
    /**
     * Get the response of a "dummy run" update command. The response contains a
     * list of files, which need to be updated or are locally modified etc.
     */
    private UpdateServerResponse getUpdateServerResponse(Client client, String path)
        throws CommandException, CommandAbortedException, 
            AuthenticationException
    {
        // setupConnection();
        // Client client = getClient();
        setupConnection(client);

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
    boolean isDirectoryUnderCVS(File dir)
    {
        String apath = adminHandler.getMetaDataPath(dir.getAbsolutePath());
        dir = new File(apath);
        return (new File(dir, "CVS").isDirectory());
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#commitAll(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.lang.String)
     */
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles, Set<File> deletedFiles,
            Set<File> files, String commitComment)
    {
        return new CvsCommitAllCommand(this, newFiles, binaryNewFiles, deletedFiles, files,
                commitComment);
    }

    /**
     * Get a command to update the given sets of files.
     * 
     * @param listener  The listener to receive notification of update progress, conflicts
     * @param theFiles   The set of files to update normally
     * @param forceFiles The set of files to update forcefully (to a clean version from
     *                   the repository)
     * @return
     */
    public TeamworkCommand updateFiles(UpdateListener listener, Set<File> theFiles,
            Set<File> forceFiles)
    {
        return new CvsUpdateCommand(this, listener, theFiles, forceFiles);
    }
    
   /**
    * Update the listed files from the repository
    *
    * @param client  The client to use to perform the update with
    * @param listener  The listener to receive notifications of updated files
    * @param theFiles  The set of files to update
    * @param force  Whether to do a forced "clean copy" update (override
    *           local changes)
    *
    * @return UpdateServerResponse with information about the update
    *
    * @throws CommandAbortedException
    * @throws CommandException
    * @throws AuthenticationException
    * @throws InvalidCvsRootException
    */
    synchronized UpdateServerResponse doUpdateFiles(BlueJCvsClient client,
            UpdateListener listener, Set<File> theFiles, boolean force)
        throws CommandAbortedException, CommandException, 
            AuthenticationException
    {
        UpdateCommand command = new UpdateCommand();
        command.setFiles(listToFileArray(theFiles));
        
        command.setCleanCopy(force);
        command.setRecursive(false);
        command.setBuildDirectories(true);
        command.setPruneDirectories(true);

        UpdateServerResponse updateServerResponse = new UpdateServerResponse(listener,
                client);
        client.getEventManager().addCVSListener(updateServerResponse);
        client.setLocalPath(projectPath.getAbsolutePath());
        
        printCommand(command, client);
        setupConnection(client);

        try {
            adminHandler.setMildManneredMode(true);
            client.executeCommand(command, globalOptions);
            updateServerResponse.waitForExecutionToFinish();
        }
        finally {
            // restore previous excludes setting
            client.getEventManager().removeCVSListener(updateServerResponse);
            disconnect(client);
            adminHandler.setMildManneredMode(false);
        }

        updateServerResponse.setConflictMap(client.getConflictFiles());
        return updateServerResponse;
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#shareProject()
     */
    public TeamworkCommand shareProject()
    {
        return new CvsShareProjectCmd(this);
    }

    /**
     * See if we can connect to the server specified by the parameter
     * cvsRootStr
     *
     * @param cvsrootStr
     *
     * @return true if the connection could be made
     */
    public static TeamworkCommandResult validateConnection(String cvsrootStr)
    {
        TeamworkCommandResult status = null;
        Connection connection = null;

        try {
            CVSRoot cvsroot = CVSRoot.parse(cvsrootStr);
            connection = getConnection(cvsroot);

            if (connection != null) {
                connection.verify();
                status = new TeamworkCommandResult();
            }
        } catch (AuthenticationException e) {
            // problem verifying connection
            status = new TeamworkCommandError(e.getMessage(), e.getLocalizedMessage());
        } catch (IllegalArgumentException iae) {
            // problem parsing CVSRoot
            status = new TeamworkCommandError(iae.getMessage(), iae.getLocalizedMessage());
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

            GSSHConnection sshConnection = new GSSHConnection(socketFactory,
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
    public synchronized List<File> getRemoteFiles(Client client, Set<File> remoteDirs) throws
        AuthenticationException, CommandException
    {
        List<File> files = new LinkedList<File>();
        adminHandler.setMildManneredMode(true);
        try {
            getRemoteFiles(client, files, projectPath, remoteDirs, false);
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
    public synchronized Set<File> getRemoteDirs(Client client)
        throws AuthenticationException, CommandException
    {
        adminHandler.setMildManneredMode(true);
        Set<File> remoteDirs = new HashSet<File>();
        try {
            getRemoteFiles(client, null, projectPath, remoteDirs, true);
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
     * @param client the client for processing the commands
     * @param files  the list to store the discovered files in (may be null)
     * @param path  the path to look at
     * @param remoteDirs  a set to store all encountered remote directories in
     * @param localDirs  whether to only look at directories which also exist locally
     * 
     * @throws InvalidCvsRootException
     * @throws AuthenticationException
     * @throws CommandException
     */
    private void getRemoteFiles(Client client, List<File> files, File path, Set<File> remoteDirs,
            boolean localDirs)
        throws AuthenticationException, CommandAbortedException, CommandException
    {        
        UpdateServerResponse updateResponse = getUpdateServerResponse(client,
                path.getAbsolutePath());
        List<CvsUpdateResult> updated = updateResponse.getUpdated();
        Iterator<CvsUpdateResult> i = updated.iterator();
        while (i.hasNext()) {
            CvsUpdateResult ur = i.next();
            File f = new File(path, ur.getFilename());
            remoteDirs.add(f.getParentFile());
            if (files != null && ! f.exists()) {
                files.add(f);
            }
        }
        
        // Now recurse into any new directories which were discovered.
        List<String> newDirs = updateResponse.getNewDirectoryNames();
        for (String newDirName : newDirs) {
            File localPath = new File(path, newDirName);
            if (! localDirs || localPath.isDirectory()) {
                remoteDirs.add(localPath);
                getRemoteFiles(client, files, localPath, remoteDirs, localDirs);
            }
        }
    }
    
    /**
     * Get the status of the given set of files 
     * @param listener
     * @param files
     * @param includeRemote
     * @return
     */
    public TeamworkCommand getStatus(StatusListener listener, Set<File> files, boolean includeRemote)
    {
        return new CvsStatusCommand(this, listener, files, includeRemote);
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#getStatus(bluej.groupwork.StatusListener, java.io.FileFilter, boolean)
     */
    public TeamworkCommand getStatus(StatusListener listener, FileFilter filter, boolean includeRemote)
    {
        Set<File> files = new HashSet<File>();
        traverseDirsForFiles(files, projectPath, filter);
        return getStatus(listener, files, includeRemote);
    }

    /**
     * Traverse the directory tree starting in dir and add all the encountered 
     * files to the Set allFiles. 
     */
    private void traverseDirsForFiles(Set<File> allFiles, File dir, FileFilter filter)
    {
        if (! filter.accept(dir)) {
            return;
        }
        if (dir.isFile()) {
            allFiles.add(dir);
            return;
        }
        
        File[] files = dir.listFiles(filter);
        if (files==null){
            return;
        }

        try {
            getLocallyDeletedFiles(allFiles, dir);
        }
        catch (IOException ioe) {
            Debug.message("CVS error determining locally deleted files: " + ioe.getLocalizedMessage());
            // TODO: should probably propagate and cause the command to fail.
        }
        for(int i=0; i< files.length; i++ ){
            if (files[i].isFile()) {
                allFiles.add(files[i]);
            } else {
                traverseDirsForFiles(allFiles, files[i], filter);
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
    public synchronized StatusServerResponse getStatus(Client client, Set<File> files, Set<File> remoteDirs)
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        // setupConnection();
        // Client client = getClient();
        setupConnection(client);
        
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
            statusServerResponse.waitForExecutionToFinish();
        }
        finally {
            adminHandler.setMildManneredMode(false);
            client.getEventManager().removeCVSListener(statusServerResponse);
            disconnect(client);
        }

        return statusServerResponse;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#getModules(java.util.List)
     */
    public TeamworkCommand getModules(List<String> modules)
    {
        return new CvsModulesCommand(this, modules);
    }
    
    /**
     * Get a list of modules in the repository.
     * 
     * @throws InvalidCvsRootException
     * @throws AuthenticationException
     * @throws CommandAbortedException
     * @throws CommandException
     */
    public synchronized UpdateServerResponse doGetModules(Client client, List<String> modules) throws
            AuthenticationException, CommandAbortedException, CommandException
    {
        // Client client = getClient();
        setupConnection(client);
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
        
        List<String> projects = updateServerResponse.getNewDirectoryNames();
        for (Iterator<String> i = projects.iterator(); i.hasNext(); ) {
            String projectName = i.next();
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
    public void getLocallyDeletedFiles(Set<File> set, File dir) throws IOException
    {
        Iterator<Entry> i = adminHandler.getEntries(dir);
        while (i.hasNext()) {
            Entry entry = i.next();
            File file = new File(dir, entry.getName());
            if (! file.exists() && ! entry.isDirectory()) {
                set.add(new File(dir, entry.getName()));
            }
        }
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#getLogHistory(bluej.groupwork.LogHistoryListener)
     */
    public TeamworkCommand getLogHistory(LogHistoryListener listener)
    {
        return new CvsLogCommand(this, listener);
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
    public synchronized LogServerResponse doGetLogHistory(Client client)
        throws AuthenticationException, CommandAbortedException, CommandException
    {
        // Client client = getClient();
        setupConnection(client);

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
    
    /**
     * Get the repository root path (the directory path on the server to the
     * repository).
     */
    String getRepositoryRoot()
    {
        return cvsroot.getRepository();
    }
    
    /*
     * (non-Javadoc)
     * @see bluej.groupwork.Repository#prepareDeleteDir(java.io.File)
     */
    public boolean prepareDeleteDir(File dir)
    {
        /* Move the CVS metadata into a new location */
        adminHandler.prepareDeleteDir(dir);
        return true;
    }
    
    /**
     * Prepare a newly created directory for version control.
     */
    public void prepareCreateDir(File dir)
    {
        adminHandler.prepareCreateDir(dir);
    }
    
    /*
     * (non-Javadoc)
     * @see bluej.groupwork.Repository#getMetadataFilter()
     */
    public FileFilter getMetadataFilter()
    {
        return new FileFilter() {
            public boolean accept(File pathname)
            {
                return ! pathname.getName().equals("CVS"); 
            }
        };
    }
    
    /*
     * (non-Javadoc)
     * @see bluej.groupwork.Repository#getAllLocallyDeletedFiles()
     */
    public void getAllLocallyDeletedFiles(Set<File> files)
    {
        LinkedList<File> stack = new LinkedList<File>();
        stack.add(projectPath);
        Set<File> tempSet = new HashSet<File>();
        FileFilter reposFilter = getMetadataFilter();

        while (! stack.isEmpty()) {
            File dir = (File) stack.remove(0);
            File [] subDirs = dir.listFiles(new DirectoryFilter());
            for (int i = 0; i < subDirs.length; i++) {
                if (reposFilter.accept(subDirs[i])) {
                    stack.add(subDirs[i]);
                }
            }
            try {
                getLocallyDeletedFiles(files, dir);
            }
            catch (IOException ioe) { }
        }

        File delDir = new File(projectPath, "CVS");
        delDir = new File(delDir, "deleted");
        if (delDir.exists()) {
            stack.add(delDir);
        }
        
        while (! stack.isEmpty()) {
            File dir = (File) stack.remove(0);
            File [] subDirs = dir.listFiles(new DirectoryFilter());
            for (int i = 0; i < subDirs.length; i++) {
                if (reposFilter.accept(subDirs[i])) {
                    stack.add(subDirs[i]);
                }
            }
            try {
                getLocallyDeletedFiles(tempSet, dir);
                for (Iterator<File> i = tempSet.iterator(); i.hasNext(); ) {
                    File file = (File) i.next();
                    // map the file back to the real directory
                    String fileStr = file.getPath();
                    fileStr = fileStr.substring(delDir.getPath().length());
                    file = new File(projectPath, fileStr);
                    files.add(file);
                }
                tempSet.clear();
            }
            catch (IOException ioe) { }
        }
    }
    
    /**
     * Set the revision of a versioned file to the given revision, without altering
     * the file contents. (This is a way to "update" but keep the current file
     * contents. The server doesn't need to be contacted).
     */
    public synchronized void setFileVersion(File file, String revision) throws IOException
    {
        Entry cvsEntry = adminHandler.getEntry(file);
        if (cvsEntry != null) {
            cvsEntry.setRevision(revision);
        }
        else {
            cvsEntry = new Entry();
            cvsEntry.setName(file.getName());
            cvsEntry.setRevision(revision);
        }
        adminHandler.setEntry(file, cvsEntry);
    }

    @Override
    public String getVCSType()
    {
        return "CVS";
    }

    @Override
    public String getVCSProtocol()
    {
        return protocol;
    }

}
