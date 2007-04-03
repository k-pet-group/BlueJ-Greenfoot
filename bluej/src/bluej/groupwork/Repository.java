package bluej.groupwork;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

public interface Repository
{
    /**
     * Checkout project from repostitory to local project.
     *
     * @throws CommandException
     * @throws CommandAbortedException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public BasicServerResponse checkout(File projectPath)
        throws CommandException, CommandAbortedException, 
        AuthenticationException, InvalidCvsRootException;

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
    public BasicServerResponse commitAll(Set newFiles, Set binaryNewFiles,
            Set deletedFiles, Set files, String commitComment)
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException;
    
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
    public UpdateServerResponse updateAll(UpdateListener listener)
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException;
    
    /**
     * Put the project in the repository and make local copy a sandbox
     *
     * @throws CommandAbortedException
     * @throws CommandException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    public BasicServerResponse shareProject()
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException;

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
    public List getRemoteFiles(Set remoteDirs) throws InvalidCvsRootException,
        AuthenticationException, CommandException;
    
    /**
     * Find the remote directories which also exist locally, but are not
     * locally under version control.
     */
    public Set getRemoteDirs() throws InvalidCvsRootException,
        AuthenticationException, CommandException;
    
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
    public List getStatus(Set files, Set remoteDirs)
        throws CommandAbortedException, CommandException, 
            AuthenticationException, InvalidCvsRootException;
    
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
    public UpdateServerResponse getModules(List modules) throws InvalidCvsRootException, AuthenticationException,
            CommandAbortedException, CommandException;
    
    /**
     * Get the locally deleted files (files which are under version control,
     * and which existed locally, but which have been deleted locally).
     * 
     * <p>Does not communicate with server.
     * 
     * @param set  The set to store the locally deleted files in
     * @param dir  The directory to look for deleted files in (non-recursively)
     * @throws IOException
     */
    public void getLocallyDeletedFiles(Set set, File dir) throws IOException;
    
    /**
     * Get the history of the repository - all commits, including file, date,
     * revision, user, and comment.
     * 
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     * @throws CommandAbortedException
     * @throws CommandException
     */
    public LogServerResponse getLogHistory()
        throws AuthenticationException, InvalidCvsRootException, CommandAbortedException,
        CommandException;
}