package bluej.debugger;

/**
 ** @version $Id: SunClassLoader.java 36 1999-04-27 04:04:54Z mik $
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
