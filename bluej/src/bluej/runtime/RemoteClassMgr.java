package bluej.runtime;

import bluej.utility.Debug;
import bluej.Config;
import bluej.classmgr.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.table.*;

/**
 ** @version $Id: RemoteClassMgr.java 160 1999-07-06 14:39:53Z ajp $
 ** @author Andrew Patterson
 ** Class to maintain a global classpath environment.
 **/
public class RemoteClassMgr
{
	/**
	 */
	private ClassPath otherLibraries = new ClassPath();

	public RemoteClassMgr() {

	}

	public ClassLoader getLoader(String classpathstr)
	{
		return new ClassPathLoader(new ClassPath(classpathstr,""));
	}

	public ClassLoader getLoader()
	{
		return new ClassPathLoader(null);
	}

	public void setLibraries(String libraries)
	{
		otherLibraries = new ClassPath(libraries, "");
	}

	class ClassPathLoader extends ClassLoader
	{
		ClassPath classpath;

		ClassPathLoader(ClassPath classpath)
		{
			this.classpath = classpath;
		}

		/**
		 * Read in a class file from disk. Return a class object.
		 */
		protected Class findClass(String name) throws ClassNotFoundException
		{
			// Debug.message("remoteclasspathloader: finding " + name);

			byte[] bytes = loadClassData(name);
			if(bytes != null) {
				// Debug.message("remoteclasspathloader: succeeded " + name);
				return defineClass(name, bytes, 0, bytes.length);
			}
			else {
				// Debug.message("remoteclasspathloader: failed " + name);
				throw new ClassNotFoundException("RemoteClassPathLoader");
			}
		}

		/**
		 * Read in a class file from disk. Return the class code as a byte
		 * array. The JDK class loader delegation model means that we are
		 * only ever asked to look up a class if the parent system loader
		 * has failed. Therefore we need only look in our userLibraries and
		 * systemLibraries. The bootLibraries will have been searched by
		 * the system loader.
		 */
		protected byte[] loadClassData(String name)
		{
			ByteArrayOutputStream classdata = new ByteArrayOutputStream();

			try {
				String filename = name.replace('.', Config.slash) + ".class";

				InputStream in = null;

				if(classpath != null)
					in = classpath.getFile(filename);

				if(in == null)
					in = otherLibraries.getFile(filename);

				if(in != null) {
					BufferedInputStream bufin = new BufferedInputStream(in);
					int b;
					while ((b = bufin.read()) != -1) {
						classdata.write(b);
					}
					// Debug.message("remoteclasspathloader: " + classdata.size() + " bytes");
				}

			} catch(Exception e) {
				Debug.reportError("cannot load class " + name + ": " + e);
				e.printStackTrace();
				return null;
			}
			if (classdata.size() == 0)
				return null;
			else
				return classdata.toByteArray();
		}
	}
}

