package bluej.classmgr;

import bluej.utility.Debug;
import bluej.Config;
import bluej.prefmgr.PrefMgrDialog;

import java.io.*;
import java.util.*;
import java.net.*;


/**
 ** @version $Id: ClassMgr.java 278 1999-11-16 00:58:12Z ajp $
 ** @author Andrew Patterson
 **
 ** Class to maintain a global class loading environment.
 **
 ** We aim to construct a class hierarchy like this
 **
 **  DefaultSystemLoader
 **       ^
 **       |
 **  BlueJLoader (retrieve with ClassMgr.getBlueJLoader()
 **    ^  ^  ^     or use ClassMgr.loadClass() to use directly)
 **    |  |  |
 **  ClassPathLoaders
 **              (one for each package, retrieve using getLoader(dir)
 **               and supply the directory the package lives in)
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
	 * environment. Some of the methods of class <code>ClassMgr</code> are instance 
	 * methods and must be invoked with respect to the current classmgr object. 
	 * 
	 * @return  the <code>ClassMgr</code> object associated with the current
	 *          BlueJ environment.
	 */
	public static ClassMgr getClassMgr() { 
		return currentClassMgr;
	}

	/**
	 * Returns a ClassLoader which can load classes from a particular class
	 * directory (while delegating other class loading to the default BlueJ
	 * class loader).
	 */
	public static ClassLoader getLoader(String classdir) {
		return new ClassPathLoader(new ClassPath(classdir, "Package " + classdir),
						getBlueJLoader());
	}

	/**
	 * Convenience static method to easily allow classes to be loaded into the default
	 * BlueJ class loader.
	 */
	public static Class loadBlueJClass(String classname) throws ClassNotFoundException {
		return getBlueJLoader().loadClass(classname);
	}
	/**
	 * Returns the class loader associated with the ClassMgr.
	 * This class loader is used as the parent of all
	 * class loaders created within BlueJ.
	 * 
	 * @return  the <code>ClassLoader</code> associated with BlueJ's
	 *          current ClassMgr
	 */
	public static ClassLoader getBlueJLoader() {
		return getClassMgr().classloader;
	}


	private BlueJLoader classloader = new BlueJLoader();

	public Iterator getAllClassPathEntries() {
		List fullList = new LinkedList();

		fullList.addAll(systemLibraries.getEntries());
		fullList.addAll(userLibraries.getEntries());
		fullList.addAll(bootLibraries.getEntries());

		return fullList.iterator();
	}

	public String getAllClassPath() {
		ClassPath all = new ClassPath();

		all.addClassPath(systemLibraries);
		all.addClassPath(userLibraries);
		all.addClassPath(bootLibraries);

		return all.toString();
	}

	public Iterator getNonBootClassPathEntries() {
		List fullList = new LinkedList();
		fullList.addAll(systemLibraries.getEntries());
		fullList.addAll(userLibraries.getEntries());

		return fullList.iterator();
	}

	public String getNonBootClassPath() {
		ClassPath all = new ClassPath();
		all.addClassPath(systemLibraries);
		all.addClassPath(userLibraries);
		return all.toString();
	}

	/**
	 * Protected to allow access by the class manager panel.
	 * These start off as empty classpath's. If the corresponding
	 * file does not exist and therefore throws an exception
	 * when we go to open it we will still end up with a valid
	 * classpath object (albeit empty)
	 */
	protected ClassPath systemLibraries = new ClassPath();
	protected ClassPath userLibraries = new ClassPath();
	protected ClassPath bootLibraries = new ClassPath();

	/**
	 * Create the user configuration file name.
	 * 
	 * @return the user configuration file
	 */
	protected File getUserConfigFile() {
		return new File(Config.getUserConfigDir(),userlibs_file);
	}
				       
	/**
	 * Create the system configuration file name.
	 * 
	 * @return the system configuration file
	 */
	protected File getSystemConfigFile() {
		return new File(Config.getSystemConfigDir(),syslibs_file);
	}

	/** Don't let anyone else instantiate this class */
	private ClassMgr() {

		try {
			userLibraries = new ClassPath(new FileInputStream(getUserConfigFile()));
    	} catch (IOException ioe) {
			// Debug.message(errorloadingconfig + "\n" + ioe.getLocalizedMessage());
		}

		try {
			systemLibraries = new ClassPath(new FileInputStream(getSystemConfigFile()));
		} catch (IOException ioe) {
			// Debug.message(errorloadingconfig + "\n" + ioe.getLocalizedMessage());
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

        /* we should add here the boot libraries which are in the JDK extension
           directory */

		/* The libraries which are in the java classpath environment variable should
		   only be the bluej libraries needed to run the program */

		if (envcp != null) {
			bootLibraries.addClassPath(envcp, Config.getString("classmgr.bluejclass"));
		}
	}

	class BlueJLoader extends ClassLoader
	{
		/**
		 * Read in a class file from disk. Return a class object.
		 */
		protected Class findClass(String name) throws ClassNotFoundException
		{
			// Debug.message("classmgrloader: finding " + name);

			byte[] bytes = loadClassData(name);
			if(bytes != null) {
				// Debug.message("classmgrloader: succeeded " + name);
				return defineClass(name, bytes, 0, bytes.length);
			}
			else {
				// Debug.message("classmgrloader: failed " + name);
				throw new ClassNotFoundException("BlueJLoader");
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

				InputStream in = systemLibraries.getFile(filename);

				if(in == null)
					in = userLibraries.getFile(filename);

				if(in != null) {
					BufferedInputStream bufin = new BufferedInputStream(in);
					int b;
					while ((b = bufin.read()) != -1) {
						classdata.write(b);
					}
					// Debug.message("classmgrloader: " + classdata.size() + " bytes");
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

class ClassPathLoader extends URLClassLoader
{
	ClassPathLoader(ClassPath classpath, ClassLoader parent)
	{
		super(classpath.getURLs(), parent);
	}
}
