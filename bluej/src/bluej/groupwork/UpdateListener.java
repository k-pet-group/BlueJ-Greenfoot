package bluej.groupwork;

import java.io.File;

/**
 * An interface for listening to changes performed to local files
 * by an update from the repository.
 * 
 * @author Davin McCall
 * @version $Id: UpdateListener.java 5888 2008-09-22 10:24:40Z davmac $
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
     * A directory (and all files within) was removed.
     * The files within might not be individually reported.
     */
    public void dirRemoved(File f);
    
    /**
     * Conflicts must be handled.
     */
    public void handleConflicts(UpdateResults updateResults);
}
