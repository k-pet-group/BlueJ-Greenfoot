package bluej.utility;

import bluej.Config;
import bluej.utility.Debug;

import java.io.InputStream;
import java.io.IOException;

/**
 ** A simple ClassLoader for BlueJ
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: SimpleClassLoader.java 72 1999-05-11 05:01:43Z mik $
 **/
public class SimpleClassLoader extends ClassLoader
{
    private ClasspathSearcher searcher;
	
    public SimpleClassLoader(ClasspathSearcher searcher)
    {
	this.searcher = searcher;
    }
	
    public SimpleClassLoader(String path)
    {
	this(new ClasspathSearcher(path));
    }
	
    public SimpleClassLoader()
    {
	this(new ClasspathSearcher());
    }
	
    public void addSearchPath(String path)
    {
	searcher.addClasspath(path);
    }
	
    public void removeSearchPath(String path)
    {
	searcher.removeClasspath(path);
    }
	
    protected Class loadClass(String name, boolean resolve)
	throws ClassNotFoundException
    {
	Class c = loadClass(name);
		
	if(resolve)
	    resolveClass(c);

	return c;
    }
	
    public synchronized Class loadClass(String name)
	throws ClassNotFoundException
    {
	Class preloaded = findLoadedClass(name);
	if(preloaded != null)
	    return preloaded;

	Class cl = findClass(name);

	if(cl != null)
	    return cl;
	else
	    return findSystemClass(name);
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
	byte[] classData = null;

	try {
	    String filename = name.replace('.', Config.slash) + ".class";
	    InputStream in = searcher.getFile(filename);
	    if(in != null) {
		byte[] buffer = new byte[in.available()];
		in.read(buffer);
		classData = buffer;
		in.close();
	    }
	} catch(Exception e) {
	    Debug.reportError("cannot load class " + name + ": " + e);
	    e.printStackTrace();
	}
	return classData;
    }
	
    public boolean hasClass(String classname)
    {
	return (findLoadedClass(classname) != null);
    }
}
