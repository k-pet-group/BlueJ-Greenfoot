package bluej.utility;

import bluej.Config;
import bluej.pkgmgr.Package;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.File;

/**
 * A FileView subclass that enables BlueJ packages to be displayed with a
 * distinct icon in a FileChooser.
 *
 * @author Michael Kolling
 * @see FileUtility
 * @version $Id: PackageFileView.java 2704 2004-07-01 09:24:22Z polle $
 */
public class PackageFileView extends FileView
{
    static final Icon packageIcon = Config.getImageAsIcon("image.filechooser.packageIcon");

    /**
     * The name of the file.  Do nothing special here. Let the system file
     * view handle this. (All methods that return null get then handled by
     * the system.)
     */
    public String getName(File f)
    {
        return null;
    }

    /**
     * A human readable description of the file.
     */
    public String getDescription(File f)
    {
        return null;
    }

    /**
     * A human readable description of the type of the file.
     */
    public String getTypeDescription(File f)
    {
        return null;
    }

    /**
     * Here we return proper BlueJ package icons for BlueJ packages.
     * Everything else gets handled by the system (by returning null).
     */
    public Icon getIcon(File f)
    {
        if(Package.isBlueJPackage(f))
            return packageIcon;
        else
            return null;
    }
}
