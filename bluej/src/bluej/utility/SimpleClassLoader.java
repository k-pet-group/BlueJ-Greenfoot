package bluej.utility;

import bluej.Config;
import bluej.utility.Debug;

import java.io.InputStream;
import java.io.IOException;

/**
 ** @version $Id: SimpleClassLoader.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** A simple ClassLoader for BlueJ
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

		byte[] bytes = findClass(name);
		if(bytes != null)
			return defineClass(name, bytes, 0, bytes.length);
		else
		{
			Class ret = findSystemClass(name);
			// Debug.message("Returning system class " + ret + " for class " + name);
			return ret;
		}
	}

	protected byte[] findClass(String name)
		throws ClassNotFoundException
	{
		byte[] classData = null;

		try {
			String filename = name.replace('.', Config.slash) + ".class";
			InputStream in = searcher.getFile(filename);
			if(in != null)
			{
				byte[] buffer = new byte[in.available()];
				in.read(buffer);
				classData = buffer;
				in.close();
			}
		} catch(Exception e) {
			// ignore it
			e.printStackTrace();
		}
			
		return classData;
	}
	
	public boolean hasClass(String classname)
	{
		return (findLoadedClass(classname) != null);
	}
}
