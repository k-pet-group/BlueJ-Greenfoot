package bluej.debugger.suntools;

import bluej.debugger.*;

/**
 ** @version $Id: SunClassLoader.java 93 1999-05-28 00:54:37Z mik $
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
