package bluej.debugger;

import java.util.Vector;

/**
 ** A class defining the debugger thread primitives needed by BlueJ
 ** May be implemented in the local VM or remotely.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: DebuggerThread.java 124 1999-06-14 07:26:17Z mik $
 **/

public abstract class DebuggerThread
{
    public abstract String getName();
    public abstract void setParam(Object param);
    public abstract Object getParam();
    public abstract String getStatus();
    public abstract String getClassSourceName(int frameNo);
    public abstract int getLineNumber(int frameNo);
    public abstract Vector getStack();
    public abstract Vector getLocalVariables(int frameNo);
    public abstract DebuggerObject getCurrentObject(int frameNo);
    public abstract void setSelectedFrame(int frame);
    public abstract int getSelectedFrame();
    public abstract void stop();
    public abstract void step();
    public abstract void stepInto();
    public abstract void cont();
    public abstract void terminate();
}
