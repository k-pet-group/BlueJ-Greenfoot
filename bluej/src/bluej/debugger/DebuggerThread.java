package bluej.debugger;

import java.util.Vector;

/**
 ** A class defining the debugger thread primitives needed by BlueJ
 ** May be implemented in the local VM or remotely.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: DebuggerThread.java 111 1999-06-04 06:16:57Z mik $
 **/

public abstract class DebuggerThread
{
    public abstract String getName();
    public abstract Object getParam();
    public abstract String getStatus();
    public abstract void setHalted(boolean halted);
    public abstract boolean isHalted();
    public abstract String getClassSourceName(int frameNo);
    public abstract int getLineNumber(int frameNo);
    public abstract Vector getStack();
    public abstract Vector getLocalVariables(int frameNo);
    public abstract DebuggerObject getCurrentObject(int frameNo);
    public abstract void stop();
    public abstract void step();
    public abstract void stepInto();
    public abstract void cont();
    public abstract void terminate();
}
