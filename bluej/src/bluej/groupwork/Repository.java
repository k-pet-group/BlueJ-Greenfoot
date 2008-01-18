package bluej.groupwork;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A version control repository, which comprises a remote repository together with a
 * local copy.
 * 
 * @author Davin McCall
 */
public interface Repository
{
    /**
     * Checkout project from repostitory to local project.
     */
    public TeamworkCommand checkout(File projectPath);

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
     */
    public TeamworkCommand commitAll(Set newFiles, Set binaryNewFiles,
            Set deletedFiles, Set files, String commitComment);
    
    /**
     * Update local files with changes from the repository
     * 
     * @param listener  A listener to be notified of files being added/updated/removed
     *                  during the update, and of conflicts.
     * @param theFiles     The files to be updated (excluding forceFiles)
     * @param forceFiles   Files to be updated "forcefully", i.e. get a clean copy of the
     *                     file from the repository rather than attempting a merge
     */
    public TeamworkCommand updateFiles(UpdateListener listener, Set theFiles, Set forceFiles);
    
    /**
     * Put the project in the repository
     */
    public TeamworkCommand shareProject();

    /**
     * Get status of all the given files.
     * Returns a List of TeamStatusInfo.
     *
     * @param listener  A listener to be notified of the status of each requested file
     * @param files  The files whose status to retrieve
     * @param includeRemote  Whether to include remote files (files which do not exist
     *                       locally, but which do exist in the repository), regardless of
     *                       whether they are listed in the files argument.
     */
    public TeamworkCommand getStatus(StatusListener listener, Set files, boolean includeRemote);
    
    /**
     * Get a list of modules in the repository. The module names (String) are added
     * to the supplied list before the command terminates.
     */
    public TeamworkCommand getModules(List modules);
    
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
     */
    public TeamworkCommand getLogHistory(LogHistoryListener listener);
    
    /**
     * Prepare for the deletion of a directory. For CVS, this involves moving
     * the metadata elsewhere.
     */
    public void prepareDeleteDir(File dir);
    
    /**
     * Prepare a newly created directory for version control.
     */
    public void prepareCreateDir(File dir);
    
    /**
     * Get a filter which can filter out directories/files that comprise metadata
     * or other housekeeping information in the working copy
     */
    public FileFilter getMetadataFilter();
}
