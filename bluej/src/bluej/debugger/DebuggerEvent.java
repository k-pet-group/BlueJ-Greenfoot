package bluej.debugger;

import java.util.EventObject;

/**
 */
public class DebuggerEvent extends EventObject
{
	public final static int DEBUGGER_STATE = 1;
	
	public final static int THREAD_HALT = 2;
	public final static int THREAD_BREAKPOINT = 3;
	public final static int THREAD_CONTINUE = 4;
	public final static int THREAD_SHOWSOURCE = 5;

	private int id;
	private DebuggerThread thr;

	public DebuggerEvent(Object source, int id)
	{
		super(source);

		this.id = id;
		this.thr = null;
	}

	public DebuggerEvent(Object source, int id, DebuggerThread thr)
	{
		super(source);

		this.id = id;
		this.thr = thr;
	}
	
	public int getID()
	{
		return id;
	}

	public DebuggerThread getThread()
	{
		return thr;
	}

}
