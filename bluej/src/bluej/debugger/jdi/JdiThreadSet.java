package bluej.debugger.jdi;

import java.util.*;

import bluej.utility.Debug;

import com.sun.jdi.ThreadReference;

/**
 * A wrapper around a TreeSet that helps us
 * store JdiThreads.
 * 
 * @author  Michael Kolling
 * @version $Id: JdiThreadSet.java 2129 2003-07-23 06:25:49Z ajp $
 */
public class JdiThreadSet extends HashSet
{
	/**
	 * Construct an empty thread set.
	 * 
	 */
    public JdiThreadSet()
    {
        super();
    }

	/**
	 * Find the thread in the set representing the thread reference specified.
	 */
    public JdiThread find(ThreadReference thread)
	{
        for(Iterator it=iterator(); it.hasNext(); ) {
            JdiThread currentThread = (JdiThread)it.next();
            if(currentThread.getRemoteThread().equals(thread)) {
                return currentThread;
            }
        }
        Debug.reportError("Encountered thread not in ThreadSet!");
        return null;
	}

	/**
	 * Remove the given thread from the set.
	 */
    public void removeThread(ThreadReference thread)
	{
        for(Iterator it=iterator(); it.hasNext(); ) {
            if(((JdiThread)it.next()).getRemoteThread().equals(thread)) {
                it.remove();
                return;
            }
        }
        Debug.reportError("Unknown thread died!");
	}
}
