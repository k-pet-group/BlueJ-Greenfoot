package bluej.debugger;

import java.util.EventObject;

/**
 */
public class DebuggerEvent extends EventObject
{
	public final static int DEBUGGER_STATECHANGED = 1;
	public final static int DEBUGGER_REMOVESTEPMARKS = 2;
	
	public final static int THREAD_HALT = 3;
	public final static int THREAD_BREAKPOINT = 4;
	public final static int THREAD_CONTINUE = 5;
	public final static int THREAD_SHOWSOURCE = 6;

	private int id;
	private DebuggerThread thr;
	private int oldState, newState;

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

	public DebuggerEvent(Object source, int id, int oldState, int newState)
	{
		this(source, id);

		this.oldState = oldState;
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

	public int getOldState()
	{
		return oldState;
	}

	public int getNewState()
	{
		return newState;
	}
}
