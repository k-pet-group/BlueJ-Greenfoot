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
 ** @version $Id: ClassMgr.java 105 1999-06-03 02:14:25Z ajp $
 ** @author Andrew Patterson
 ** Class to maintain a global classpath environment.
 **/
public class ClassMgr
{
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
    
	public ArrayList systemLibraries = new ArrayList();
	public ArrayList bootLibraries = new ArrayList();
	public ClassPath userLibraries;

	public ArrayList allLibraries = new ArrayList();

	/** Don't let anyone else instantiate this class */
	private ClassMgr() {

		try {
			userLibraries = new ClassPath(new FileInputStream(getUserConfigFilename()));
		} catch (IOException ioe) {
			Debug.message("config file not found");
		}

		fillTableFromClasspath(System.getProperty("sun.boot.class.path"),
					bootLibraries, "from boot path");
		fillTableFromConfigFile(getSystemConfigFilename() , systemLibraries);
//		fillTableFromConfigFile(getUserConfigFilename() , userLibraries);

//		allLibraries.addAll(userLibraries);
		allLibraries.addAll(systemLibraries);
		allLibraries.addAll(bootLibraries);

//		System.out.println(allLibraries.toString());

		/* now check whether there are any entries in the environment
		   variable CLASSPATH which we have not yet seen. We will add
		   them to the user's libraries so that they will be saved out
		   for next time */
		   
		ArrayList cpLibraries = new ArrayList();
		fillTableFromClasspath(System.getProperty("java.class.path"),
					cpLibraries, "from class path");

		Iterator cpLibrariesIterator = cpLibraries.listIterator();

/*       if (syscp == null) {
            cp = System.getProperty("java.class.path");
            if (cp == null)
                cp = ".";
        } else {
            String envcp = System.getProperty("env.class.path");
            if (envcp == null)
                envcp = ".";
            cp = syscp + File.pathSeparator + envcp;
		while (cpLibrariesIterator.hasNext()) {
			String nextEntry = (String)cpLibrariesIterator.next();

			if (!allLibraries.contains(nextEntry)) {
				userLibraries.add(nextEntry,cpLibraries.get(nextEntry));
				allLibraries.put(nextEntry,cpLibraries.get(nextEntry));

				System.out.println("Entry " + nextEntry + " in class path variable is not list anyhwer");
			} 
		} */
	}

	/**
	 * Read from a classpath string all the libraries which it references
	 *
	 * @return true if the file loaded successfully, false otherwise
	 */
	private void fillTableFromClasspath(String classpath, ArrayList table,
						String genericdescription)
	{
		try {
			StringTokenizer st = new StringTokenizer(classpath, Config.colonstring);

			while(st.hasMoreTokens()) {
				String entry = st.nextToken();
				ClassPathEntry cpentry = new ClassPathEntry(entry, genericdescription);

				table.add(cpentry);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private boolean fillTableFromConfigFile(String configfile, ArrayList table)
	{
		Properties config = new Properties();

		try {
			config.load(new FileInputStream(configfile));
		} catch (IOException ioe) {
			Debug.message(Config.getString("browser.librarychooser.missingsysconfdialog.text"));
			return false;
		}
	
		int resourceID = 1;
		try {
			String alias, location;

			while (true) {
				alias = config.getProperty("lib" + resourceID + ".alias");
				location = config.getProperty("lib" + resourceID + ".location");

				if (alias == null || location == null)
					break;

				ClassPathEntry cpentry = new ClassPathEntry(location, alias);
				table.add(cpentry);
				resourceID++;
			}
		} catch (MissingResourceException mre) {
			// it is normal that this is exception is thrown, it just means we've come to the end of the file
		}

		return true;
	}

	/**
	 * Create the user configuration file name.
	 * 
	 * @return the name of the user configuration file
	 */
	private static String getUserConfigFilename() {
		return Config.getUserConfigDir() + File.separator +
			Config.getString("browser.librarychooser.config.user");
	}
				       
	/**
	 * Create the system configuration file name.
	 * 
	 * @return the name of the system configuration file
	 */
	private static String getSystemConfigFilename() {
		return Config.getSystemConfigDir() + File.separator +
			 Config.syslibs_file;
	}

/*	public Vector getBootLibraries() 
	{

	}

	public Vector getSystemLibraries()
	{


	}
*/
	/**
	 * Save the current user configured libraries back to the user configuration file.
	 * Note that any comments originally appearing in this file will be removed.
	 * 
	 * @return true if the configuration could be saved.
	 */
/*	public boolean saveUserConfigFile() {
		Properties userConfig = new Properties();
		
		Enumeration librariesEnum = userLibraries.keys();

		int current = 1;
		while (librariesEnum.hasMoreElements()) {
			String nextAlias = libraries.nextElement().toString();
	    if (libraryAliases.get(nextAlias) instanceof UserLibraryNode) {
		userConfig.put("lib" + current + ".alias", nextAlias);
		userConfig.put("lib" + current + ".location", ((UserLibraryNode)libraryAliases.get(nextAlias)).getInternalName());
		current++;
	    }
	}
		
	try {
	    userConfig.save(new FileOutputStream(Config.getUserConfigDir() + File.separator + Config.getString("browser.librarychooser.config.user")), "");
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
		
	return true;
    } */
}
