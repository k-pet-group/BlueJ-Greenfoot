package bluej.debugger.jdi;

import bluej.debugger.*;

import com.sun.jdi.ClassLoaderReference;

/**
 * Holds a handle for a ClassLoader in a remote VM.
 *
 * @author  Michael Kolling
 * @version $Id: JdiClassLoader.java 329 2000-01-02 13:22:01Z ajp $
 */
public class JdiClassLoader extends DebuggerClassLoader
{
    String id;
    ClassLoaderReference loader;

    public JdiClassLoader(String id, ClassLoaderReference loader)
    {
        this.id = id;
        this.loader = loader;
    }

    public String getId()
    {
        return id;
    }

    public ClassLoaderReference getLoader()
    {
        return loader;
    }
}
