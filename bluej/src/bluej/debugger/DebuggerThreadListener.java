package bluej.debugger;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 15/05/2017.
 */
@OnThread(Tag.Any)
public interface DebuggerThreadListener
{
    /**
     * Called when a thread has changed state.
     * @param thread The thread whose state has changed
     * @param shouldDisplay If true, change the debugger to display this thread.
     *                      If false, leave display as-is.
     */
    void threadStateChanged(DebuggerThread thread, boolean shouldDisplay);

    /**
     * Called after a VM reset, when the list of threads should be cleared.
     */
    void clearThreads();

    void addThread(DebuggerThread thread);

    void removeThread(DebuggerThread thread);
}
