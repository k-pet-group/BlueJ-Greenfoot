package bluej.classmgr;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.net.*;

import bluej.utility.Debug;
import bluej.Config;

/**
 * Class to maintain a single file/directory location in a classpath
 *
 * @author  Andrew Patterson
 * @version $Id: ClassPathEntry.java 416 2000-03-14 03:03:13Z ajp $
 */
public class ClassPathEntry implements Cloneable
{
    /**
     * Hold the class path entry location.
     */
    private File file;
    /**
     * Hold the class path entry description.
     */
    private String description;

    /**
     * Holds a file/directory location in a classpath entry along with a
     * description.
     *
     * @param location  the directory path or filename or a jar/zip file
     * @param description a short description of the classes represented
     *                    by this classpath entry
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

    /**
     * Holds a file/directory location in a classpath entry without
     * a description.
     *
     * @param location  the directory path or filename or a jar/zip file
     */
/*    public ClassPathEntry(String location)
    {
        this(location, null);
    } */

    /**
     * Gets the description for this entry.
     *
     * @returns a string describing the contents of this classpath entry
     */
    public String getDescription()
    {
        if (description == null)
            return "no description (" + file.getPath() + ")";
        else
            return description;
    }

    protected void setDescription(String d) {
        this.description = d;
    }

    public File getFile() {
        return file;
    }

    /**
     * Note that this path is always absolute because of our constructor
     */
    public String getPath() {
        return file.getPath();
    }

    public String getCanonicalPath() throws IOException {
        return file.getCanonicalPath();
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

    public URL getURL() throws MalformedURLException {
        return file.toURL();
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
