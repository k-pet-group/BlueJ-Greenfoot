package bluej.groupwork;

import java.util.List;
import java.util.Set;

/**
 * Represents a set of update results from an update, and provides a method to decide
 * conflict resolution in the case of binary file conflicts.
 * 
 * @author Davin McCall
 */
public interface UpdateResults
{
    /**
     * Get a list of File objects that have a conflict after update. 
     */
    public List getConflicts();
    
    /**
     * Get the set of files which had binary conflicts. These are files which
     * have been modified both locally and in the repository. A decision needs to
     * be made about which version (local or repository) is to be retained; use
     * the overrideFiles() method to finalise this decision.
     */
    public Set getBinaryConflicts();
    
    /**
     * Once the initial update has finished and the binary conflicts are known,
     * this method must be called to select whether to keep the local or use the
     * remote version of the conflicting files.
     *  
     * @param files  A set of files to fetch from the repository, overwriting the
     *               local version. (For any file not in the set, the local version
     *               is retained). 
     */
    public void overrideFiles(Set files);

}
