package bluej.utility;

import bluej.pkgmgr.Package;

import java.io.File;

/**
 * A file chooser for opening packages (with strict behaviour with
 * regards clicking on BlueJ packages).
 *
 * <p>Behaves the same as a PackageChooser but with the added restriction
 * that only BlueJ package directories, and archives, are an acceptable
 * selection. Double clicking on a BlueJ package will open it rather
 * than traverse into it.
 *
 * @author Michael Kolling
 * @author Axel Schmolitzky
 * @author Markus Ostman
 * @version $Id: PackageChooserStrict.java 5592 2008-02-25 05:08:42Z davmac $
 */
public class PackageChooserStrict extends PackageChooser
{
    /**
     * Create a new strict PackageChooser.
     *
     * @param startDirectory the directory to start the package selection in.
     */
    public PackageChooserStrict(File startDirectory)
    {
        super(startDirectory, false, true);
    }

    /**
     *  Selection approved by button-click. Check whether the selected
     *  directory is a BlueJ package. If so, let it be opened.
     */
    public void approveSelection()   // redefined
    {
        File selectedFile = getSelectedFile();
        if (selectedFile.isFile()) {
            // it must be an archive (jar or zip)
            approved();
        }
        else if (Package.isBlueJPackage(getSelectedFile())) {
    	    approved();
        }
        else {
            super.setCurrentDirectory(getSelectedFile());
        }
    }
}
