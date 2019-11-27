package bluej.debugger;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 15/05/2017.
 */
public interface DebuggerThreadListener
{
    /**
     * Called when a thread has changed state.
     * @param thread The thread whose state has changed
     * @param shouldDisplay If true, change the debugger to display this thread.
     *                      If false, leave display as-is.
     */
    @OnThread(Tag.VMEventHandler)
    void threadStateChanged(DebuggerThread thread, boolean shouldDisplay);

    /**
     * Called after a VM reset, when the list of threads should be cleared.
     */
    @OnThread(Tag.VMEventHandler)
    void clearThreads();

    @OnThread(Tag.VMEventHandler)
    void addThread(DebuggerThread thread);

    @OnThread(Tag.VMEventHandler)
    void removeThread(DebuggerThread thread);
}
