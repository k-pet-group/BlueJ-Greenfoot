/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2016  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.classmgr;

import java.io.File;
import java.io.IOException;
import java.net.*;

import bluej.Config;
import bluej.utility.Debug;

/**
 * Class to maintain a single file/directory location in a classpath
 *
 * @author  Andrew Patterson
 * @version $Id: ClassPathEntry.java 16031 2016-06-14 13:32:45Z nccb $
 */
public class ClassPathEntry
{
    private static final String statusGood = Config.getString("classmgr.statusgood");
    private static final String statusBad = Config.getString("classmgr.statusbad");
    private static final String statusNew = Config.getString("classmgr.statusnew");

    /**
     * Hold the class path entry location.
     */
    private final File file;

    /**
     * Hold the class path entry description.
     */
    private final String description;

    /**
     * Flag to mark entries added after system start (unloaded).
     */
    private final boolean justAdded;
    
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
        this(location, description, false);
    }


    /**
     * Holds a file/directory location in a classpath entry along with a description.
     *
     * @param file the file holding one path entry.
     * @param description a short description of the classes represented by this classpath entry
     */
    public ClassPathEntry(File file, String description)
    {
        this.file = file;
        this.description = description;
        this.justAdded = false;
    }

    public ClassPathEntry(String location, String description, boolean isNew)
    {
        // we take the decision that all ClassPathEntries should
        // be absolute (the behaviour of BlueJ should not change
        // dependant upon what directory the user was in when they
        // launched it). There may be a good reason why relative
        // ClassPathEntries are needed.. if so this code will have
        // to be rethought
        this.file = new File(location).getAbsoluteFile();
        this.description = description;
        justAdded = isNew;
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
    public URL safeGetURL()
    {
        try
        {
            return file.toURI().toURL();
        }
        catch (MalformedURLException e)
        {
            Debug.reportError("Bad class path entry: " + file, e);
            return null;
        }
    }

    /**
     * Determine if this class path entry has been added after
     * BlueJ was started (and thus isn't loaded).
     */
    public boolean isNew()
    {
        return justAdded;
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
     * Return the current status as a string (Loaded/Not Loaded/Error).
     */
    public String getStatusString()
    {
        if (!isValid())
            return statusBad;
        else if(isNew())
            return statusNew;
        else
            return statusGood;
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

    public String toString()
    {
        return getPath();
    }

    /* we want to appear the same as another ClassPathEntry if our
       location is identical - we ignore the description */

    public boolean equals(Object o)
    {
        if (o == null) {
            return false;
        }
        return this.file.equals(((ClassPathEntry)o).file);
    }

    public int hashCode()
    {
        return file.hashCode();
    }
}
