package bluej.classmgr;

import bluej.utility.Debug;
import bluej.Config;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.net.*;

/**
 ** @version $Id: ClassPathEntry.java 189 1999-07-17 02:35:32Z ajp $
 ** @author Andrew Patterson
 ** Class to maintain a single file location in a classpath
 **/
public class ClassPathEntry implements Cloneable
{
	private File file;
	private String description;

	/**
	 * Create the classpath entry
	 */
	public ClassPathEntry(String location, String description)
	{
		// we take the decision that all ClassPathEntries should
		// be absolute (the behaviour of BlueJ should not change
		// dependant upon what directory the user was in when they
		// launched it). There may be a good reason why relative
		// ClassPathEntries are needed.. if so this code will have
		// to be rethought
		this.file = new File(location).getAbsoluteFile();
		this.description = description;
	}

	public ClassPathEntry(String location)
	{
		this(location, "no description (" + location + ")");
	}

	public String getDescription() {
		return description;
	}
	protected void setDescription(String d) {
		this.description = d;
	}

	public File getFile() {
		return file;
	}

	/* Note that this path is always absolute because of our constructor
	 */
	public String getPath() {
		return file.getPath();
	}

	public String getCanonicalPath() throws IOException {
		return file.getCanonicalPath();
	}

    public URL getURL() throws MalformedURLException {
        return file.toURL();   
    }
	/* Note that the Config.getString in this method was changed from a
	 * static class string to a local variable because we need to instantiate
	 * ClassPathEntries on the remote VM, and it has no access to the Config
	 * files and was therefore generating error message when the class was
	 * loaded (even though we don't use getCanonicalPathNoException() on
	 * the remote VM).
	 */
	public String getCanonicalPathNoException() {
		String path;
		try {
			path = file.getCanonicalPath();
		} catch (IOException ioe) {
			path = Config.getString("classmgr.unresolvable") + " (" + file.getPath() + ")";
		}
		return path;
	}

	public boolean isJar() {
		String name = file.getPath();

		return file.isFile() && 
			(name.endsWith(".zip") || name.endsWith(".jar"));
	}

	public boolean isClassRoot() {
		return file.isDirectory();
	}

	public String toString() {
		return getPath();
	}

	/* we want to appear the same as another ClassPathEntry if our
	   location is identical - we ignore the description */

	public boolean equals(Object o)
	{
		return this.file.equals(((ClassPathEntry)o).file);
	}

	public int hashCode()
	{
		return file.hashCode();
	}

	protected Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
}
