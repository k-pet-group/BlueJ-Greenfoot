package bluej.utility.filefilter;

import java.io.FileFilter;
import java.io.File;

/**
* A FileFilter that only accepts directories.
* An instance of this class can be used as a parameter for
* the listFiles method of class File.
*
* @version $ $
* @author Axel Schmolitzky
* @see java.io.FileFilter
* @see java.io.File
*
*/
public class DirectoryFilter implements FileFilter {

    /**
     * This method only accepts directories.
     */
    public boolean accept(File pathname) {

        return pathname.isDirectory();

    }
}
