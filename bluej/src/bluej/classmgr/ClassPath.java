package bluej.classmgr;

import bluej.utility.Debug;
import bluej.Config;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.net.*;

import javax.swing.table.*;

/**
 * Class to maintain a list of ClassPathEntry's.
 *
 * @version $Id: ClassPath.java 194 1999-07-20 05:57:01Z ajp $
 * @author Andrew Patterson
 */
public class ClassPath
{
    /**
     * The actual list of class path entries
     */
	private ArrayList entries = new ArrayList();

    /**
     * Construct an empty ClassPath
     */
	public ClassPath()
	{
	}

    /**
     * Construct a ClassPath which is a copy of an existing ClassPath
     */
	public ClassPath(ClassPath classpath)
	{
		addClassPath(classpath);
	}

    /**
     * Construct a ClassPath from a delimitered String of entries
     *
     * @param   classpath   A ; or : seperated String with entries
     * @param   genericdescription  A String which can be used to
     *          generically describe these entries
     */
	public ClassPath(String classpath, String genericdescription)
	{
		addClassPath(classpath, genericdescription);
	}

    /**
     * Construct a ClassPath from a configuration file
     *
     * @param   inputstream A stream which represents the config file to read
     */
	public ClassPath(InputStream configstream)
	{
		addConfigFile(configstream);
	}

	/**
	 * Return the list of entries
	 */
	protected List getEntries()
	{
		return entries;
	}

	/**
	 * Remove elements from the classpath
     *
     * @param   classpath   A ; or : separated String of class path entries to
     *                      remove
	 */
	public void removeClassPath(String classpath)
	{
		try {
			StringTokenizer st = new StringTokenizer(classpath, Config.colonstring);

			while(st.hasMoreTokens()) {
				String entry = st.nextToken();

				entries.remove(entry);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * Remove all entries from the class path
     */
	public void removeAll()
	{
		entries.clear();
	}

    /**
     * Add a copy of an existing ClassPath
     *
     * @param   classpath   A ClassPath object to add a copy of
     */
	public void addClassPath(ClassPath classpath)
	{
		// make a copy of the entries.. don't just add the entries to the
		// new class path

		Iterator it = classpath.entries.iterator();

		while (it.hasNext()) {

			ClassPathEntry nextEntry = (ClassPathEntry)it.next();

			try {
				ClassPathEntry cpentry = (ClassPathEntry)nextEntry.clone();

				if(!entries.contains(cpentry))
					entries.add(cpentry);
			} catch(CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add from a classpath string all the libraries which it references
	 *
	 * @param	classpath	a string containing a sequence of filenames
	 *				separated by a path separator character
	 * @param	genericdescription	a string which will be used as the
	 *					description for all entries created for
	 *					this classpath
	 */
	public void addClassPath(String classpath, String genericdescription)
	{
		try {
			StringTokenizer st = new StringTokenizer(classpath, Config.colonstring);

			while(st.hasMoreTokens()) {
				String entry = st.nextToken();
				String name = (new File(entry)).getName();
				ClassPathEntry cpentry = new ClassPathEntry(entry, genericdescription + " - " + name);

				if(!entries.contains(cpentry))
					entries.add(cpentry);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read from an inputstream a set of configuration items which describe
	 * the set of class path entries
	 *
	 * @param	configstream	an inputstream which can be parsed as
	 *				properties
	 */
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

				if(!entries.contains(cpentry))
					entries.add(cpentry);

				resourceID++;
			}
		} catch (MissingResourceException mre) {
			// it is normal that this is exception is thrown, it just means we've come to the end of the file
		}
	}

	/**
	 * Save the current libraries back to a configuration file.
	 * Note that any comments originally appearing in this file will be removed.
	 * 
	 */
	public void putConfigFile(OutputStream configstream)
	{
		Properties config = new Properties();

		Iterator it = entries.iterator();
		int current = 1;

		while (it.hasNext()) {
			ClassPathEntry nextEntry = (ClassPathEntry)it.next();

			config.put("lib" + current + ".location", nextEntry.getPath());
			config.put("lib" + current + ".description", nextEntry.getDescription());

			current++;
		}

		try {
			config.store(configstream, "Class Libraries");
		} catch(IOException ioe) {
			Debug.message("Writing library configuration: " + ioe.getLocalizedMessage());
		}
	}

    /**
     * Return the class path entries as an array of URL's
     */
    public URL[] getURLs()
    {
		Iterator it = entries.iterator();
        URL u[] = new URL[entries.size()];
        int current = 0;

		while (it.hasNext()) {
			ClassPathEntry nextEntry = (ClassPathEntry)it.next();
	    
            try {
                u[current] = nextEntry.getURL();
                // Debug.message(u[current].toString());
            } catch(MalformedURLException mue) {
            
            }

            current++;
        }
        
        return u;
    }
    
	/**
	 * Find a file in the classpath
	 *
	 * @param	filename	a string which specifies a file to look
	 *				for throughout the class path
	 *          this filename is in native slash seperated form
	 *          ie foo/bar for UNIX and foo\bar for Windows
	 */
	public InputStream getFile(String filename) throws IOException
	{
		Iterator it = entries.iterator();

		while (it.hasNext()) {
			ClassPathEntry nextEntry = (ClassPathEntry)it.next();
	    
			// each entry can be either a jar/zip file or a directory
			// or neither in which case we ignore it

			if(nextEntry.isJar()) {
				InputStream ret = readJar(nextEntry.getFile(), filename);

				if (ret != null)
					return ret;
			} else if (nextEntry.isClassRoot()) {
				File fd = new File(nextEntry.getFile(), filename);

				if(fd.exists())
					return new FileInputStream(fd);
			}
		}
		return null;
	}

	/**
	 * Retrieve an entry out of a jar file
	 *
	 * @param	classjar	a file representing the jar to look in
	 * @param	filename	a string which specifies a file to look
	 *				for in the jar
	 */

	private InputStream readJar(File classjar, String filename) throws IOException
	{
		JarFile jarf = new JarFile(classjar);

		// filenames are passed into us in native slash separated form.
		// jar files require us to always use the forward slash when looking
		// for files so if we are on a system where / is not the actual
		// separator character we have to first fix the filename up

		if(File.separatorChar != '/')
			filename = filename.replace(File.separatorChar, '/');

		JarEntry entry = jarf.getJarEntry(filename);

		if(entry == null) {
			return null;
		}

		InputStream is = jarf.getInputStream(entry);

		return is;
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();

		Iterator it = entries.iterator();

		while (it.hasNext()) {
			ClassPathEntry nextEntry = (ClassPathEntry)it.next();

			buf.append(nextEntry.getPath());
			buf.append(Config.colon);
		}

		return buf.toString();
	}
}
