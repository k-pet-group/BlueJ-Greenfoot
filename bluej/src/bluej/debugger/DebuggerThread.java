package bluej.debugger;

import java.util.List;

/**
 * A class defining the debugger thread primitives needed by BlueJ.
 *
 * Objects of this class can only be constructed 
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: DebuggerThread.java 2033 2003-06-12 06:51:21Z ajp $
 */

public abstract class DebuggerThread
{
    public abstract String getName();

    public abstract String getStatus();
    
    public abstract boolean isSuspended();
	public abstract boolean isAtBreakpoint();

    public abstract String getClass(int frameNo);
    public abstract String getClassSourceName(int frameNo);
    public abstract int getLineNumber(int frameNo);
    public abstract boolean isKnownSystemThread();

    public abstract List getStack();
    public abstract List getLocalVariables(int frameNo);
    public abstract boolean varIsObject(int frameNo, int index);
    public abstract DebuggerObject getStackObject(int frameNo, int index);
    public abstract DebuggerObject getCurrentObject(int frameNo);
    public abstract DebuggerClass getCurrentClass(int frameNo);

    public abstract void setSelectedFrame(int frame);
    public abstract int getSelectedFrame();

	public abstract void halt();
	public abstract void cont();
	
    public abstract void step();
    public abstract void stepInto();
}
