package bluej.debugger.suntools;

import bluej.debugger.*;

/**
 ** @version $Id: SunClassLoader.java 86 1999-05-18 02:49:53Z mik $
 ** @author Michael Cahill
 ** Holds a handle for a ClassLoader in a remote runtime.
 **/

public class SunClassLoader extends DebuggerClassLoader
{
	String id;
	
	public SunClassLoader(String id)
	{
		this.id = id;
	}
	
	public String getId()
	{
		return id;
	}
}
