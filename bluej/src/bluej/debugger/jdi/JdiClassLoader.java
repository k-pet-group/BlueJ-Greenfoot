package bluej.debugger.jdi;

import bluej.debugger.*;

import com.sun.jdi.ClassLoaderReference;

/**
 * Holds a handle for a ClassLoader in a remote VM.
 *
 * @author  Michael Kolling
 * @version $Id: JdiClassLoader.java 1991 2003-05-28 08:53:06Z ajp $
 */
public class JdiClassLoader extends DebuggerClassLoader
{
	ClassLoaderReference loader;
	
    public JdiClassLoader(ClassLoaderReference clr)
    {
    	loader = clr;
    }

    public String getId()
    {
        return Long.toString(loader.uniqueID());
    }
}
