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

/**
 ** @version $Id: ClassPathEntry.java 132 1999-06-16 04:44:24Z ajp $
 ** @author Andrew Patterson
 ** Class to maintain a single file location in a classpath
 **/
public class ClassPathEntry implements Cloneable
{
	private final static String unresolvable = Config.getString("classmgr.unresolvable");

	private File file;
	private String description;

	/**
	 * Create the classpath entry
	 */
	public ClassPathEntry(String location, String description) {

		this.file = new File(location);
		this.description = description;
	}

	public ClassPathEntry(String location)
	{
		this(location, "no description (" + location + ")");
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String d) {
		this.description = d;
	}

	public File getFile() {
		return file;
	}

	public String getPath() {
		return file.getPath();
	}

	public String getCanonicalPath() throws IOException {
		return file.getCanonicalPath();
	}

	public String getCanonicalPathNoException() {
		String path;
		try {
			path = file.getCanonicalPath();
		} catch (IOException ioe) {
			path = unresolvable + " (" + file.getPath() + ")";
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
