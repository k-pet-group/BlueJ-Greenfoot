package bluej.debugger;

import java.util.Vector;

/**
 ** @version $Id: DebuggerThread.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** A class defining the debugger thread primitives needed by BlueJ
 ** May be implemented in the local VM or remotely (via sun.tools.debug)
 **/

public abstract class DebuggerThread
{
    public abstract String getName();
    public abstract String getStatus();
    public abstract void setHalted(boolean halted);
    public abstract boolean isHalted();
    public abstract String getClassSourceName(int frameNo);
    public abstract int getLineNumber(int frameNo);
    public abstract Vector getStack();
    public abstract Vector getInstanceVariables(int frameNo);
    public abstract Vector getLocalVariables(int frameNo);
    public abstract void stop();
    public abstract void step();
    public abstract void stepInto();
    public abstract void cont();
    public abstract void terminate();
}
