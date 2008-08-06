package bluej.groupwork.svn;

import java.io.File;
import java.util.Set;

import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.UpdateListener;

/**
 * Implementation of StatusHandle for Subversion.
 * 
 * @author davmac
 */
public class SvnStatusHandle implements StatusHandle
{
    private SvnRepository repository;
    private long version;
    
    public SvnStatusHandle(SvnRepository repository, long version)
    {
        this.repository = repository;
        this.version = version;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#commitAll(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.lang.String)
     */
    public TeamworkCommand commitAll(Set<File> newFiles,
            Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files,
            Set<TeamStatusInfo> forceFiles, String commitComment)
    {
        // TODO Complete implementation
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.StatusHandle#updateTo(bluej.groupwork.UpdateListener, java.util.Set, java.util.Set)
     */
    public TeamworkCommand updateTo(UpdateListener listener, Set<File> files,
            Set<File> forceFiles)
    {
        return new SvnUpdateToCommand(repository, listener,
                version, files, forceFiles);
    }

}
