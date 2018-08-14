/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2016,2018  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork;

import java.io.File;
import java.io.FileFilter;
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
     * Set the password used to access the repository
     */
    public void setPassword(TeamSettings newSettings);
    
    /**
     * Returns true if this repository versions directories (subversion),
     * false otherwise (CVS).
     */
    public boolean versionsDirectories();
    
    /**
     * Checkout project from repostitory to local project.
     */
    public TeamworkCommand checkout(File projectPath);

    /**
     * Commits the files and directories in the project.
     *
     * @param newFiles Files to be committed which are not presently in the repository
     *                 (text files only). If the version control system versions directories,
     *                 this must be an ordered set where directories precede the files they
     *                 contain.
     * @param binaryNewFiles Files to be committed which are not presently in the
     *                       repository and which are to be treated as binary
     * @param deletedFiles Files which have been deleted locally but which exist
     *                     in the latest version in the repository 
     * @param files  All files to be committed (including all in newFiles, binaryNewFiles,
     *               and deletedFiles, as well as any other files to be committed)
     * @param commitComment  The comment for this commit
     */
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles,
            Set<File> deletedFiles, Set<File> files, String commitComment);
    
    /**
     * Put the project in the repository. This should create an empty project in
     * the repository, and set the local project up as a working copy (with
     * uncommitted files).
     */
    public TeamworkCommand shareProject();

    /**
     * Push project changes to the upstream server.
     * This is used *only* by distributed version control.
     */
    public TeamworkCommand pushChanges();
    
    /**
     * Get status of all the given files.
     *
     * @param listener  A listener to be notified of the status of each requested file.
     *                For version management systems which version directories, the status
     *                of directories will be reported before files they contain.
     * @param filter  A file filter to determine which files and directories to include
     *                in the returned statuses
     * @param includeRemote 
     *                Whether to include remote files (files which do not exist locally, but which
     *                do exist in the repository), regardless of whether they are listed in the
     *                files argument.
     */
    public TeamworkCommand getStatus(StatusListener listener, FileFilter filter, boolean includeRemote);
    
    /**
     * Get a list of modules in the repository. The module names (String) are added
     * to the supplied list before the command terminates.
     */
    public TeamworkCommand getModules(List<String> modules);
    
    /**
     * Get the history of the repository - all commits, including file, date,
     * revision, user, and comment.
     */
    public TeamworkCommand getLogHistory(LogHistoryListener listener);
    
    /**
     * Prepare for the deletion of a directory. For CVS, this involves moving
     * the metadata elsewhere. Returns true if the directory should be physically
     * removed, or false otherwise, in which case all files and sub-directories
     * within are assumed to have been handled.
     * 
     * <p>Also, calling this may result in the directory
     * being removed.
     */
    public boolean prepareDeleteDir(File dir);
    
    /**
     * Prepare a newly created directory for version control.
     */
    public void prepareCreateDir(File dir);
    
    /**
     * Get a filter which can filter out directories/files that comprise metadata
     * or other housekeeping information in the working copy
     */
    public FileFilter getMetadataFilter();
    
    /**
     * Get all the locally deleted files in the repository. The files are put
     * into the supplied set.
     * 
     * <p>Calling this method Does not result in communication with the repository
     * server.
     */
    public void getAllLocallyDeletedFiles(Set<File> files);
    
    /**
     * Gets the version control type, for data collection purposes
     */
    public String getVCSType();
    
    /**
     * Gets the version control protocol, for data collection purposes
     */
    public String getVCSProtocol();
}
