package bluej.utility.filefilter;

import java.io.*;

/**
 * A FileFilter that only accepts directories.
 * An instance of this class can be used as a parameter for
 * the listFiles method of class File.
 *
 * @author Axel Schmolitzky
 * @version $Id: DirectoryFilter.java 1700 2003-03-13 03:34:20Z ajp $
 */
public class DirectoryFilter implements FileFilter
{
    /**
     * This method only accepts directories.
     */
    public boolean accept(File pathname)
    {
        return pathname.isDirectory();
    }
}
