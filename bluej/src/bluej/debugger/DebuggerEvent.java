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
	private int newState;

	public DebuggerEvent(Object source, int id)
	{
		super(source);

		this.id = id;
	}

	public DebuggerEvent(Object source, int id, DebuggerThread thr)
	{
		this(source, id);

		this.thr = thr;
	}

	public DebuggerEvent(Object source, int id, int newState)
	{
		this(source, id);

		this.newState = newState;
	}
	
	public int getID()
	{
		return id;
	}

	public DebuggerThread getThread()
	{
		return thr;
	}

	public int getNewState()
	{
		return newState;
	}
}
