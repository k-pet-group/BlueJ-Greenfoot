package bluej.runtime;

import java.io.*;
import java.net.InetAddress;
import java.security.Permission;
import java.awt.*;

/**
 * A SecurityManager for the BlueJ runtime
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: RemoteSecurityManager.java 1582 2002-12-13 06:32:37Z ajp $
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
        /**
         * Many AWT programs react to a window close event by
         * calling System.exit(). Unfortunately it is the AWT
         * event thread which is executing at the time and so
         * we cannot allow that thread to die as it is impossible
         * to restart without restarting the VM. Here we check if
         * we are running as the AWT thread and if so, set an
         * event so that later on all the top level windows
         * will be disposed (we assume this is the behaviour the
         * programmer wished by calling System.exit()). We also
         * will break the main user thread started in the
         * virtual machine (to simulate quitting the application).
         */

        if (EventQueue.isDispatchThread())
        {
            // no matter what, we have to throw an exception and
            // no matter what that exception will be printed to
            // System.err by the AWT thread.. we play some
            // funny business to hide the message

            // there are probably numerous race conditions etc
            // hidden in here but this is the best solution we've
            // come up with so far

            // signal local VM that we are simulating an exit
            ExecServer.exitMarker();

            // hide the exception print out which the AWT event
            // thread handler will print
            ExecServer.supressOutput();

            Toolkit.getDefaultToolkit().getSystemEventQueue().
                invokeLater(new Runnable() {
                        public void run() {
                            ExecServer.restoreOutput();
                            ExecServer.disposeWindows();
                        }
                    });

            // this exception will not ever be printed out
            throw new ExitException(Integer.toString(status));
        }
        else {
            // this exception will be displayed to the user
            throw new ExitException(Integer.toString(status));
        }
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
