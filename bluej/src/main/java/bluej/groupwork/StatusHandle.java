/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2016,2017  Michael Kolling and John Rosenberg
 
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

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.util.Set;

@OnThread(Tag.FXPlatform)
public interface StatusHandle
{
    /**
     * Commits the files and directories in the project. Some files can be forced,
     * which means that the existing local file (regardless of its revision) replaces
     * the repository version (with the specified version number), only failing if
     * an intermediate commit has occurred. 
     *
     * @param newFiles Files to be committed which are not presently in the repository
     *                 (text files only). If the version management system versions
     *                 directories, this should also contain directories, and must be
     *                 an ordered set such that any directories precede files which
     *                 they contain.
     * @param binaryNewFiles Files to be committed which are not presently in the
     *                       repository and which are to be treated as binary
     * @param deletedFiles Files which have been deleted locally but which exist
     *                     in the latest version in the repository 
     * @param files  All files to be committed (including all in newFiles, binaryNewFiles,
     *               and deletedFiles, as well as any other files to be committed)
     * @param forceFiles  Those files for which the commit should be forced, overriding
     *               the existing file in the repository.
     *               specified. (The commit can still fail if the file is committed
     * @param commitComment  The comment for this commit
     */
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles,
            Set<File> deletedFiles, Set<File> files, Set<TeamStatusInfo> forceFiles,
            String commitComment);

    /**
     * After a status command, get a command which can be used to
     * update the working copy to the same revision(s) as was 
     * shown in the status.
     * 
     * For CVS, this doesn't work exactly - it just does an update to
     * latest revision, which might have changed since the status
     * was performed.
     */
    public TeamworkCommand updateTo(UpdateListener listener, Set<File> files, Set<File> forceFiles);
    
    /**
     * Gets the repository.  Used for data collection.
     * @return
     */
    public Repository getRepository();
    
    /**
     * Push changes into remote repository. Shall not be used on Subversion or cvs.
     * @param filesToPush set of files to be pushed into the remote repository
     * @return TeamworkCommand if a VCS, null otherwise.
     */
    public default TeamworkCommand pushAll(Set<File> filesToPush){
        return null;
    }
    
    /**
     * checks if, usually after a merge commit, the current repository state must
     * be pushed into the remote repository. This must be implemented by DCVS 
     * systems such as GIT.
     * @return 
     */
    @OnThread(Tag.Worker)
    public default boolean pushNeeded()
    {
        return true;
    }
    
    /**
     * checks if, the current repository state must be pulled from the remote 
     * repository. This must be implemented by DCVS 
     * systems such as GIT.
     * @return 
     */
    @OnThread(Tag.Worker)
    public default boolean pullNeeded()
    {
        return false;
    }
}
