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
 * @version $Id: ClassPathEntry.java 1067 2002-01-08 05:49:39Z ajp $
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
     * Gets the description for this class path entry.
     *
     * @returns a string describing the contents of this classpath entry
     */
    public String getDescription()
    {
        if (description == null)
            return Config.getString("classmgr.error.nodescription") +
                    " (" + file.getPath() + ")";
        else
            return description;
    }

    /**
     * Set the description for this class path entry.
     *
     * @param description a short description of the classes represented
     *                    by this classpath entry
     */
    protected void setDescription(String d)
    {
        this.description = d;
    }

    /**
     * Gets the File for this class path entry.
     *
     * @returns a File identifying the file or directory that this class
     *          path entry represents
     */
    public File getFile()
    {
        return file;
    }

    /**
     * Gets the path for this class path entry.
     *
     * @returns a File identifying the file or directory that this class
     * @note    this path is always absolute because of our constructor
     */
    public String getPath()
    {
        return file.getPath();
    }

    /**
     * Gets the canonical path for this entry, or a String describing an error
     * if the canonical path could not be found.
     *
     * @returns a String representing the canonical path of the file or
     *          directory that this class path entry represents
     * @note    the Config.getString in this method was changed from a
     *          static class string to a local variable because we need to instantiate
     *          ClassPathEntries on the remote VM, and it has no access to the Config
     *          files and was therefore generating error message when the class was
     *          loaded (even though we don't use getCanonicalPathNoException() on
     *          the remote VM).
     */
    public String getCanonicalPathNoException()
    {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException ioe) {
            path = Config.getString("classmgr.error.unresolvable") +
                    " (" + file.getPath() + ")";
        }
        return path;
    }

    /**
     * Gets a URL representing this class path entry.
     *
     * @returns a URL representing this classpath entry
     */
    public URL getURL() throws MalformedURLException
    {
        return file.toURL();
    }

    /**
     * Determine if this class path entry represents a valid entry
     * on the current VM (ie file/dir exists and is readable)
     */
    public boolean isValid()
    {
        /* If its a directory then it exists and we wont try to
           work out any more about it.. its valid as far as we are
           concerned */
        if (file.isDirectory())
            return true;

        /* If it satisfies our conditions for a Jar file we still may
           not be able to read it, so use that as a test for validity */
        if (isJar())
            return file.canRead();

        /* we don't know what it is.. mark it as invalid */
        return false;
    }

    /**
     * Determine if this class path entry represents a Jar file.
     *
     * @returns a boolean indicating if this entry is a jar or
     *          zip file.
     */
    public boolean isJar()
    {
        String name = file.getName().toLowerCase();

        return file.isFile() &&
            (name.endsWith(".zip") || name.endsWith(".jar"));
    }

    /**
     * Determine if this class path entry represents the root of
     * a class directory.
     *
     * @returns a boolean indicating if this entry is a class
     *          directory.
     */
    public boolean isClassRoot()
    {
        return file.isDirectory();
    }

    public String toString()
    {
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
