package bluej.debugger.jdi;

import bluej.debugger.*;

/**
 ** Holds a handle for a ClassLoader in a remote runtime.
 **
 ** @author Michael Kolling
 **/

public class JdiClassLoader extends DebuggerClassLoader
{
	String id;
	
	public JdiClassLoader(String id)
	{
		this.id = id;
	}
	
	public String getId()
	{
		return id;
	}
}
