package bluej.classmgr;

import bluej.utility.Debug;
import bluej.Config;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.*;

import javax.swing.table.*;

/**
 ** @version $Id: ClassMgr.java 132 1999-06-16 04:44:24Z ajp $
 ** @author Andrew Patterson
 ** Class to maintain a global classpath environment.
 **/
public class ClassMgr
{
	static final String errorloadingconfig = Config.getString("classmgr.error.loadingconfig");
	static final String errormissingclasspath = Config.getString("classmgr.error.missingclasspath");
	static final String errormissingbootclasspath = Config.getString("classmgr.error.missingbootclasspath");

	static final String userlibs_file = Config.getPropString("classmgr.userconfig","userlibs.properties");
	static final String syslibs_file = Config.getPropString("classmgr.systemconfig","syslibs.properties");

	private static ClassMgr currentClassMgr = new ClassMgr();

	/**
	 * Returns the classmgr object associated with the current BlueJ.
	 * environment. Most of the methods of class <code>ClassMgr</code> are instance 
	 * methods and must be invoked with respect to the current classmgr object. 
	 * 
	 * @return  the <code>ClassMgr</code> object associated with the current
	 *          BlueJ environment.
	 */
	public static ClassMgr getClassMgr() { 
		return currentClassMgr;
	}

	/**
	 * Protected to allow access by the class manager dialog.
	 * These start off as empty classpath's. If the corresponding
	 * file does not exist and therefore throws an exception
	 * when we go to open it we will still end up with a valid
	 * classpath object (albeit empty)
	 */
	protected ClassPath systemLibraries = new ClassPath();
	protected ClassPath userLibraries = new ClassPath();
	protected ClassPath bootLibraries = new ClassPath();

	private Loader classloader = new Loader();

	/**
	 * Returns the class loader associated with this ClassMgr.
	 * This class loader should be used as the parent of all
	 * class loaders created within BlueJ.
	 * 
	 * @return  the <code>ClassLoader</code> associated with BlueJ's
	 *          current ClassMgr
	 */
	public ClassLoader getLoader() {
		return classloader;
	}

	/** Don't let anyone else instantiate this class */
	private ClassMgr() {

		try {
			userLibraries = new ClassPath(new FileInputStream(getUserConfigFile()));
		} catch (IOException ioe) {
			Debug.message(errorloadingconfig + "\n" + ioe.getLocalizedMessage());
		}

		try {
			systemLibraries = new ClassPath(new FileInputStream(getSystemConfigFile()));
		} catch (IOException ioe) {
			Debug.message(errorloadingconfig + "\n" + ioe.getLocalizedMessage());
		}

		String syscp = System.getProperty("sun.boot.class.path");
		String envcp = System.getProperty("java.class.path");

		if (syscp == null) {		// pre JDK1.2
			Debug.message(errormissingbootclasspath);
		} else {
			if (envcp == null) {	// no classpath
				Debug.message(errormissingclasspath);
			}
		}

		bootLibraries = new ClassPath(syscp, Config.getString("classmgr.bootclass"));

		/* The libraries which are in the java classpath environment variable should
		   only be the bluej libraries needed to run the program */

		if (envcp != null) {
			bootLibraries.addClassPath(envcp, Config.getString("classmgr.bluejclass"));
		}
	}

	/**
	 * Create the user configuration file name.
	 * 
	 * @return the user configuration file
	 */
	private File getUserConfigFile() {
		return new File(Config.getUserConfigDir(),userlibs_file);
	}
				       
	/**
	 * Create the system configuration file name.
	 * 
	 * @return the system configuration file
	 */
	private File getSystemConfigFile() {
		return new File(Config.getSystemConfigDir(),syslibs_file);
	}

	class Loader extends ClassLoader
	{
		/**
		 * Read in a class file from disk. Return a class object.
		 */
		protected Class findClass(String name)
		{
			// Debug.message("classmgrloader: finding ", name);

			byte[] bytes = loadClassData(name);
			if(bytes != null) {
				// Debug.message("classmgrloader: succeeded", name);
				return defineClass(name, bytes, 0, bytes.length);
			}
			else {
				// Debug.message("classmgrloader: failed", name);
				return null;
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
			byte[] classData = null;

			try {
				String filename = name.replace('.', Config.slash) + ".class";

				InputStream in = systemLibraries.getFile(filename);

				if(in == null)
					in = userLibraries.getFile(filename);

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
	}
}
