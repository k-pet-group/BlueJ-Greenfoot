package bluej.runtime;

import bluej.Config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Hashtable;

/**
 ** A ClassLoader for the BlueJ runtime. One of these class loaders is
 ** created for every package, with the package path as the classpath.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **/
public class BlueJClassLoader extends ClassLoader
{
	String classdir;

    /**
     * Create a class loader for the directory specified (the directory
     * is usually the package directory).
     */
    public BlueJClassLoader(String classdir)
    {
	this.classdir = classdir;
    }
	
    /**
     * 
     * 
     */
    public Class loadClass(String name, boolean resolve)
	 throws ClassNotFoundException
    {
	Class cl = loadClass(name);
	if(resolve)
	    resolveClass(cl);
	return cl;
    }

    /**
     * Load a class. Check whether we have it pre-loaded; if not,
     * load it from disk.
     */
    public synchronized Class loadClass(String name)
	 throws ClassNotFoundException
    {
	Class preloaded = findLoadedClass(name);
	if(preloaded != null)
	    return preloaded;

	Class cl = findClass(name);

	if (cl != null)
	    return cl;
	else
	    return findSystemClass(name);
	
	// throw new ClassNotFoundException(name);
    }
 	
    /**
     * Read in a class file from disk. Return a class object.
     */
    protected Class findClass(String name)
    {
	byte[] bytes = loadClassData(name);
	if(bytes != null)
	    return defineClass(name, bytes, 0, bytes.length);
	else
	    return null;
    }

    /**
     * Read in a class file from disk. Return the class code as a byte
     * array.
     */
    protected byte[] loadClassData(String name)
    {
	String filename = classdir + File.separator + name.replace('.', Config.slash) + ".class";
	byte[] classData = null;
	
	try {
	    FileInputStream in = new FileInputStream(filename);
	    classData = new byte[in.available()];
	    in.read(classData);
	    in.close();
	} catch(Exception e) {
	    // ignore it
	}

	return classData;
    }

}
