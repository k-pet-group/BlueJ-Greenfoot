package bluej.groupwork;

import java.io.File;

/**
 * An interface for listening to changes performed to local files
 * by an update from the repository.
 * 
 * @author Davin McCall
 * @version $Id: UpdateListener.java 4916 2007-04-12 03:57:23Z davmac $
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
    public void handleConflicts(UpdateResults updateServerResponse);
}
