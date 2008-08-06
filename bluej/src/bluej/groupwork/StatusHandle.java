package bluej.groupwork;

import java.io.File;
import java.util.Set;

public interface StatusHandle
{
    /**
     * Commits the files and directories in the project. Some files can be forced,
     * which means that the existing local file (regardless of its revision) replaces
     * the repository version (with the specified version number), only failing if
     * an intermediate commit has occurred. 
     *
     * @param newFiles Files to be committed which are not presently in the repository
     *                 (text files only)
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
}
