package bluej.utility.filefilter;

import java.io.FileFilter;
import java.io.File;

import bluej.pkgmgr.Package;

/**
* A FileFilter that only accepts BlueJ package directories.
* An instance of this class can be used as a parameter for
* the listFiles method of class File.
*
* @version $ $
* @author Axel Schmolitzky
* @see java.io.FileFilter
* @see java.io.File
*
*/
public class SubPackageFilter implements FileFilter {

    /**
     * This method only accepts directories.
     */
    public boolean accept(File pathname) {

        if (pathname.isDirectory() &&
                Package.isBlueJPackage(pathname))
                    return true;
        else
            return false;
    }
}
