package bluej.utility;

import bluej.pkgmgr.Package;

import java.io.File;

/**
 * A file chooser for opening packages (with strict behaviour with
 * regards clicking on BlueJ packages).
 *
 * Behaves the same as a PackageChooser but with the added restriction
 * that only BlueJ package directories are an acceptable selection
 * and that double clicking on a BlueJ package will open it not
 * traverse into it.
 *
 * @author Michael Kolling
 * @author Axel Schmolitzky
 * @author Markus Ostman
 * @version $Id: PackageChooserStrict.java 4602 2006-09-07 04:31:33Z davmac $
 */
class PackageChooserStrict extends PackageChooser
{
    /**
     * Create a new strict PackageChooser.
     *
     * @param startDirectory the directory to start the package selection in.
     */
    public PackageChooserStrict(File startDirectory)
    {
        super(startDirectory, false, false);
    }

    /**
     *  Selection approved by button-click. Check whether the selected
     *  directory is a BlueJ package. If so, let it be opened.
     */
    public void approveSelection()   // redefined
    {
    	if (Package.isBlueJPackage(getSelectedFile())) {
    	    approved();
        }
        else {
            super.setCurrentDirectory(getSelectedFile());
        }
    }
}
