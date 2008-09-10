package bluej.pkgmgr;

import java.io.File;

import bluej.Config;

/**
 * Factory for creating package files.
 * 
 * @author Poul Henriksen
 */
public class PackageFileFactory
{

    /**
     * Get a packagefile for the given directory. This will be either a
     * Greenfoot or BlueJ package file, depending on whether we are using this
     * from Greenfoot or BlueJ.
     * 
     * @param dir
     * @return
     */
    public static PackageFile getPackageFile(File dir)
    {
        if (Config.isGreenfoot()) {
            return new GreenfootProjectFile(dir);
        }
        else {
            return new BlueJPackageFile(dir);
        }
    }
}
