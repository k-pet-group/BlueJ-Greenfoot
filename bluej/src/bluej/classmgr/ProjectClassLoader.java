package bluej.classmgr;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A class loader that will load classes from the current directory
 * and from jar files within a +libs directory.
 *
 * @author  Andrew Patterson
 * @version $Id: ProjectClassLoader.java 3103 2004-11-18 04:59:24Z davmac $
 */
public class ProjectClassLoader extends URLClassLoader
{
    public static final String projectLibDirName = "+libs";
    private File[] libsJars;
    
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

        setDefaultAssertionStatus(true);

        // the subdirectory of the project which can hold project specific
        // jars and zips
        File libsDirectory = new File(projectDir, projectLibDirName);

        // the list of jars and zips we find

        if (libsDirectory.isDirectory()) {
            libsJars = libsDirectory.listFiles(new JarFilter());
        }
        if(libsJars == null)
            libsJars = new File[0];

        // if we found any jar files in the libs directory then add their
        // URLs
        for(int i=0; i<libsJars.length; i++) {
            try {
                addURL(libsJars[i].toURI().toURL());
            }
            catch(MalformedURLException mue) { }
        }
    }

	/* Use me for debugger class loading problems
	protected Class findClass(String name)
	throws ClassNotFoundException
	{
		System.out.println(this + " findClass(" + name + ")");
		Class cl = super.findClass(name);
		System.out.println(this + " result in a class with classloader " + cl.getClassLoader());
		
		return cl;
	} */
	
    /**
     * Construct and return a ClassPath representing all the entries
     * managed by this class loader
     */
    public ClassPath getAsClassPath()
    {
        return new ClassPath(getURLs());
    }

    /**
     * Return the jar files found in the '+libs' directory within the project directory.
     * 
     * @return An array of jar files, may be empty, but will not be null.
     */
    public File[] getProjectLibs()
    {
        return libsJars;
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
            URL urls[] = { projectDir.toURI().toURL() };
            return urls;
        }
        catch(MalformedURLException mue) { }

        URL blankUrls[] = { };

        return blankUrls; 
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

