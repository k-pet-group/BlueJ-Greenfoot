package bluej.classmgr;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

import bluej.utility.Debug;
import bluej.Config;

/**
 * A class loader that will load classes from the current directory
 * and from jar files within a +libs directory.
 *
 * @author  Andrew Patterson
 * @version $Id: ProjectClassLoader.java 1226 2002-04-15 12:58:51Z mik $
 */
public class ProjectClassLoader extends URLClassLoader
{
    private String libsString = "+libs";

    public ProjectClassLoader(File projectDir)
    {
        this(projectDir, ClassLoader.getSystemClassLoader());
    }

    /**
     * Construct a class loader that load classes from the
     * directory projectDir using the parent class loader parent.
     */
    public ProjectClassLoader(File projectDir, ClassLoader parent)
    {
        super(getDirectoryAsURL(projectDir), parent);

        setAssertions(true);

        // the subdirectory of the project which can hold project specific
        // jars and zips
        File libsDirectory = new File(projectDir, libsString);

        // the list of jars and zips we find
        File libsJars[] = null;

        if (libsDirectory.isDirectory()) {
            libsJars = libsDirectory.listFiles(new JarFilter());
        }

        // if we found any jar files in the libs directory then add their
        // URLs
        if (libsJars != null) {
            for(int i=0; i<libsJars.length; i++) {
                try {
                    addURL(libsJars[i].toURL());
                }
                catch(MalformedURLException mue) { }
            }
        }
    }

    /**
     * Construct and return a ClassPath representing all the entries
     * managed by this class loader
     */
    public ClassPath getAsClassPath()
    {
        return new ClassPath(getURLs());
    }

    /**
     * Turns a directory File object into an array of length 1
     * containing a single URL. This is a helper function for
     * the constructor of this class.
     */
    private static URL[] getDirectoryAsURL(File projectDir)
    {
        if (!projectDir.isDirectory())
            throw new IllegalArgumentException("project directory was not a directory");

        // the project directory is always added as a URL
        try {
            URL urls[] = { projectDir.toURL() };
            return urls;
        }
        catch(MalformedURLException mue) { }

        URL blankUrls[] = { };

        return blankUrls; 
    }

    /**
     * Under a 1.4 or above VM, set default assert status in the project class loader
     * using reflection to find the relevant method.
     */
    private void setAssertions(boolean status)
    {
        try {
            Class cl = this.getClass();

            Class[] p = new Class[] { boolean.class }; 
 
            Method setAssertMethod = cl.getMethod("setDefaultAssertionStatus", p);

            Object objResult;
            Object[] arguments = new Object[] { new Boolean(status) };

            objResult = setAssertMethod.invoke(this, arguments);
        }
        // for all these errors no need to report anything.. just assume its because
        // assertions are not supported (ie 1.3)
        catch (NoSuchMethodException e) { }
        catch (IllegalAccessException e) { }
        catch (InvocationTargetException e) { }
    }
}

/**
 * A FileFilter that only accepts jar and zip files.
 */
class JarFilter implements FileFilter
{
    /**
     * This method only accepts files that are jar or zip files.
     */
    public boolean accept(File pathname)
    {
	String name = pathname.getName().toLowerCase();

	return pathname.isFile() &&
	    (name.endsWith(".zip") || name.endsWith(".jar"));
    }
}

