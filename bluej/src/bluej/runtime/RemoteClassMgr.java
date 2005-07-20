package bluej.runtime;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import bluej.classmgr.ClassPath;
import bluej.classmgr.ProjectClassLoader;

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
 * @version $Id: RemoteClassMgr.java 3471 2005-07-20 05:47:21Z davmac $
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
     * Return a project specific class loader
     */
    public ClassLoader getLoader(String projectDirName)
    {
        return new ProjectClassLoader(new File(projectDirName),
                                      bluejLoader);
    }
    
    /**
     * Return a class loader which uses the specified classpath
     * (a '\n' seperated list of URLs).
     */
    public void setClassPath(String urls)
    {
        ArrayList urlsList = new ArrayList();
        int index = 0;
        int nindex = urls.indexOf('\n');
        while (nindex != -1) {
            try {
                urlsList.add(new URL(urls.substring(index, nindex)));
            }
            catch (MalformedURLException mfue) {}
            index = nindex;
            nindex = urls.indexOf(nindex, '\n');
        }
            
        bluejLoader =  new URLClassLoader((URL []) urlsList.toArray(new URL[urlsList.size()]));
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
			//System.out.println("BlueJ loader looking for " + name);
			Class cl = super.findClass(name);
			//System.out.println("BlueJ loader classloader for it is" + cl.getClassLoader());
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
