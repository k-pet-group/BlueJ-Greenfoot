package bluej.runtime;

import bluej.classmgr.*;

import java.io.*;
import java.net.*;

/**
 * Class to maintain a global classpath environment on the
 * remote virtual machine.
 *
 *  DefaultSystemLoader
 *       ^
 *       |
 *  BlueJLoader (changed with setLibraries(). If null, uses 
 *    ^  ^  ^    the DefaultSystemLoader)
 *    |  |  |
 *  ProjectClassLoaders
 *              (one for each project)
 *
 * @author  Andrew Patterson
 * @version $Id: RemoteClassMgr.java 2036 2003-06-16 07:08:51Z ajp $
 */
public class RemoteClassMgr
{
    /**
     * The loader which is made parent of all the ProjectClassLoaders
     * that we create. Until the first call to setLibraries(), this is
     * the default system class loader. After that is changed to a
     * BlueJLoader.
     *
     * Note, after a call to setLibraries(), existing ProjectClassLoaders
     * will still be using the old instance of bluejLoader. Only class
     * loaders created after the setLibraries() call will have the
     * changes. ProjectClassLoaders should be recreated on a compile
     * so this problem shouldn't be too much of a problem.
     */
    private ClassLoader bluejLoader = ClassLoader.getSystemClassLoader();

    /**
     * Return a non-project specific class loader
     */
    public ClassLoader getLoader()
    {
        return bluejLoader;
    }

    /**
     * Return a project specific class loader
     */
    public ClassLoader getLoader(String projectDirName)
    {
        return new ProjectClassLoader(new File(projectDirName),
                                      bluejLoader);
    }

    /**
     * Set the list of libraries that are in the remote virtual
     * machines class path
     */
    public void setLibraries(String libraries)
    {
        bluejLoader = new BlueJLoader(libraries);
    }

    class BlueJLoader extends URLClassLoader
    {
        BlueJLoader(String libraries)
        {
            super(getStringAsURLArray(libraries));
        }

		protected Class findClass(String name)
		throws ClassNotFoundException
		{
			System.out.println("BlueJ loader looking for " + name);
			Class cl = super.findClass(name);
			System.out.println("BlueJ loader classloader for it is" + cl.getClassLoader());
			return cl;
		}

    }

    private static URL[] getStringAsURLArray(String libraries)
    {
        ClassPath librariesClassPath = new ClassPath(libraries, "");

        URL librariesURLs[] = librariesClassPath.getURLs();

        return librariesURLs;
    }
}
