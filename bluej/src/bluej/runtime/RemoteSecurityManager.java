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
 * @version $Id: RemoteSecurityManager.java 600 2000-06-28 07:21:39Z mik $
 */
public class RemoteSecurityManager extends SecurityManager
{
    private PrintStream oldErr = null;
    private ByteArrayOutputStream throwawayErr = null;

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
         * programmer wished by calling System.exit())
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

            oldErr = System.err;
            throwawayErr = new ByteArrayOutputStream();

            System.setErr(new PrintStream(throwawayErr));

            ExecServer.disposeWindowsLater(oldErr);

            // this exception will not ever be printed out
            throw new ExitException(Integer.toString(status));
        }
        else {
            throw new ExitException(Integer.toString(status));
        }
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

    ThreadGroup threadGroup;

    public void setThreadGroup(ThreadGroup threadGroup)
    {
        this.threadGroup = threadGroup;
    }

    public ThreadGroup getThreadGroup()
    {
        if(threadGroup != null)
            return threadGroup;
        else
            return Thread.currentThread().getThreadGroup();
    }
}
