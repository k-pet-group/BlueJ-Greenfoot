package bluej.utility.filefilter;

import java.io.*;

import bluej.pkgmgr.Package;

/**
 * A FileFilter that only accepts BlueJ package directories.
 * An instance of this class can be used as a parameter for
 * the listFiles method of class File.
 *
 * @author  Axel Schmolitzky
 * @version $Id: SubPackageFilter.java 1700 2003-03-13 03:34:20Z ajp $
 */
public class SubPackageFilter implements FileFilter
{
    /**
     * This method only accepts directories.
     */
    public boolean accept(File pathname)
    {
        return (pathname.isDirectory() &&
                 Package.isBlueJPackage(pathname));
    }
}
