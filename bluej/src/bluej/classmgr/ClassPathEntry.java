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
 ** @version $Id: ClassPathEntry.java 105 1999-06-03 02:14:25Z ajp $
 ** @author Andrew Patterson
 ** Class to maintain a single file location in a classpath
 **/
public class ClassPathEntry
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
}
