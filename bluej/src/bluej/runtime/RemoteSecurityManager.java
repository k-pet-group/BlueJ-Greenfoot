package bluej.runtime;

import java.security.Permission;
import java.awt.*;

// NOTE: CURRENTLY UNUSED. PENDING FOR REMOVAL

/**
 * A SecurityManager for the BlueJ runtime
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: RemoteSecurityManager.java 2253 2003-11-04 13:49:11Z mik $
 */
public class RemoteSecurityManager extends SecurityManager
{
    /**
     * All user threads will be created into this group
     */
    ThreadGroup threadGroup = null;

    /**
     * This method returns the thread group that any new
     * threads should be constructed in. We rig it so that
     * if threadGroup is null, a new ThreadGroup is created and
     * this will contain all user threads. Then when we simulate
     * System.exit(), we can dispose all these threads (remembering
     * to set threadGroup back to null to reset everything).
     * Currently not implemented - we leave all user created threads
     * alone when simulating System.exit()
     */
    public ThreadGroup getThreadGroup()
    {
        if(threadGroup == null) {
            threadGroup = new ThreadGroup(super.getThreadGroup(), "BlueJ user threads");
        }
        return threadGroup;
    }

    /**
     * The only thing BlueJ applications are currently not allowed to
     * do is exit normally. We handle this by signalling the exit as
     * a special exception which we catch later in in the debugger.
     *
     * @param status   the exit status.
     */
    public void checkExit(int status)
    {
    }

    public void checkMemberAccess(Class clazz,
                                    int which)
    {
    }

    /**
     * With the exception of checkExit(int) we want to
     * behave just as if there were no SecurityManager
     * installed, so we override to do nothing.
     *
     * @param perm The permission object to ignore.
     */
    public void checkPermission(Permission perm)
    {
    }

    /**
     * With the exception of checkExit(int) we want to
     * behave just as if there were no SecurityManager
     * installed, so we override to do nothing.
     *
     * @param perm The permission object to ignore.
     * @param context The context object to ignore.
     */
    public void checkPermission(Permission perm, Object context)
    {
    }
}
