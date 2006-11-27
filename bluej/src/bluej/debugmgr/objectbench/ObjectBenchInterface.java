package bluej.debugmgr.objectbench;

/**
 * An interface for listenening for object selection events.
 * 
 * @author Davin McCall
 * @version $Id: ObjectBenchInterface.java 4708 2006-11-27 00:47:57Z bquig $
 */
public interface ObjectBenchInterface
{
    /**
     * Add a listener for events on this object bench.
     * @param l  The listener to add
     */
    public void addObjectBenchListener(ObjectBenchListener l);
    
    /**
     * Remove a listener so that it no longer receives events.
     * @param l  The listener to remove
     */
    public void removeObjectBenchListener(ObjectBenchListener l);
    
    /**
     * Check whether the bench contains an object with name 'name'.
     *
     * @param name  The name to check for.
     */
    public boolean hasObject(String name);
}
