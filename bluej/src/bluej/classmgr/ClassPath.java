package bluej.classmgr;

import bluej.utility.Debug;
import bluej.Config;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.table.*;

/**
 ** @version $Id: ClassPath.java 105 1999-06-03 02:14:25Z ajp $
 ** @author Andrew Patterson
 ** Class to maintain a list of ClassPathEntry's.
 **/
public class ClassPath
{
	public ArrayList entries;

	public ClassPath()
	{
	}

	public ClassPath(String classpath, String genericdescription)
	{
		addClassPath(classpath, genericdescription);
	}

	public ClassPath(InputStream configstream)
	{
		addConfigFile(configstream);
	}

		/* now check whether there are any entries in the environment
		   variable CLASSPATH which we have not yet seen. We will add
		   them to the user's libraries so that they will be saved out
		   for next time */
		   
/*		Config.getString("classmgr.missingsysconfdialog.text")

ArrayList cpLibraries = new ArrayList();
		fillTableFromClasspath(System.getProperty("java.class.path"),
					cpLibraries, "from class path");

		Iterator cpLibrariesIterator = cpLibraries.listIterator();
 */
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

	/**
	 * Add from a classpath string all the libraries which it references
	 *
	 */
	public void addClassPath(String classpath, String genericdescription)
	{
		try {
			StringTokenizer st = new StringTokenizer(classpath, Config.colonstring);

			while(st.hasMoreTokens()) {
				String entry = st.nextToken();
				ClassPathEntry cpentry = new ClassPathEntry(entry, genericdescription);

				entries.add(cpentry);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
/*		try {
			config.load(new FileInputStream(configfile));
		} catch (IOException ioe) {
			Debug.message(notfoundmessage);
			return false;
		}
	*/
	}

	/**
	 * Read from an inputstream a set of configuration items which describe
	 * the set of class path entries
	 *
	 * @param
	 **/
	private void addConfigFile(InputStream configstream)
	{
		Properties config = new Properties();

		try {
			config.load(configstream);
		} catch (IOException ioe) {
			Debug.message("Reading library configuration: " + ioe.getLocalizedMessage());
			return;
		}

		int resourceID = 1;
		try {
			String location, description;

			while (true) {
				location = config.getProperty("lib" + resourceID + ".location");
				description = config.getProperty("lib" + resourceID + ".description");

				if (description == null || location == null)
					break;

				ClassPathEntry cpentry = new ClassPathEntry(location, description);
				entries.add(cpentry);
				resourceID++;
			}
		} catch (MissingResourceException mre) {
			// it is normal that this is exception is thrown, it just means we've come to the end of the file
		}
	}

	/**
	 * Save the current user configured libraries back to the user configuration file.
	 * Note that any comments originally appearing in this file will be removed.
	 * 
	 * @return true if the configuration could be saved.
	 */
	public void putConfigFile(OutputStream configstream)
	{
		Properties config = new Properties();

		Iterator it = entries.listIterator();
		int current = 1;

		while (it.hasNext()) {
			ClassPathEntry nextEntry = (ClassPathEntry)it.next();

			config.put("lib" + current + ".location", nextEntry.getPath());
			config.put("lib" + current + ".description", nextEntry.getDescription());

			current++;
	    }
	}
		
//	try {
//	    userConfig.save(new FileOutputStream(Config.getUserConfigDir() + File.separator + Config.getString("browser.librarychooser.config.user")), "");
//	} catch (IOException ioe) {
//	    ioe.printStackTrace();
//	}
}
