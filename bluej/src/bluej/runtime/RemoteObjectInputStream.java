package bluej.runtime;

import java.io.*;

/**
 *
 * @author  Andrew Patterson
 * @version $Id: RemoteObjectInputStream.java 1819 2003-04-10 13:47:50Z fisker $
 */
public class RemoteObjectInputStream extends ObjectInputStream
{
    ClassLoader loader;

    RemoteObjectInputStream(InputStream in, ClassLoader loader)
        throws IOException
    {
        this.loader = loader;
    }

    protected Class resolveClass(ObjectStreamClass v)
        throws IOException, ClassNotFoundException
    {
        System.out.println("resolving " + v.getName());

        return loader.loadClass(v.getName());
    }

}
