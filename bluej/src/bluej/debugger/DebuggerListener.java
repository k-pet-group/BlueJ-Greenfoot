package bluej.debugger;

import java.util.EventListener;

/**
 * The listener for Debugger events.
 *
 * @author  Andrew Patterson
 * @version $Id: DebuggerListener.java 2033 2003-06-12 06:51:21Z ajp $
 */
public interface DebuggerListener extends EventListener
{
    void debuggerEvent(DebuggerEvent e);
}
