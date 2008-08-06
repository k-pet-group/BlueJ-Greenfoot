package bluej.groupwork.cvsnb;

import java.io.File;
import java.util.Set;

import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.UpdateListener;

/**
 * Implementation of StatusHandle for CVS.
 * 
 * @author Davin McCall
 */
public class CvsStatusHandle implements StatusHandle
{
    private CvsRepository repository;
    
    public CvsStatusHandle(CvsRepository repository)
    {
        this.repository = repository;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#commitAll(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.lang.String)
     */
    public TeamworkCommand commitAll(Set<File> newFiles,
            Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files,
            Set<TeamStatusInfo> forceFiles, String commitComment)
    {
        return new CvsCommitCommand(repository, newFiles, binaryNewFiles, deletedFiles,
                files, forceFiles, commitComment);
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#updateTo(bluej.groupwork.UpdateListener, java.util.Set, java.util.Set)
     */
    public TeamworkCommand updateTo(UpdateListener listener, Set<File> files,
            Set<File> forceFiles)
    {
        return repository.updateFiles(listener, files, forceFiles);
    }

}
