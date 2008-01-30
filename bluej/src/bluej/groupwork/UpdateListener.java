package bluej.groupwork;

import java.io.File;

/**
 * An interface for listening to changes performed to local files
 * by an update from the repository.
 * 
 * @author Davin McCall
 * @version $Id: UpdateListener.java 5510 2008-01-30 03:08:03Z davmac $
 */
public interface UpdateListener
{
    /**
     * A file was added locally.
     */
    public void fileAdded(File f);
    
    /**
     * A file was removed locally.
     */
    public void fileRemoved(File f);
    
    /**
     * A file was updated.
     */
    public void fileUpdated(File f);
    
    /**
     * Conflicts must be handled.
     */
    public void handleConflicts(UpdateResults updateResults);
}
