package bluej.runtime;

import java.io.*;
import java.net.InetAddress;
import java.security.Permission;
import java.awt.*;

/**
 *
 * @author  Andrew Patterson
 * @version $Id: RemoteObjectInputStream.java 331 2000-01-02 13:27:10Z ajp $
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
