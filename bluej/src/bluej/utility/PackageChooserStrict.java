package bluej.utility;

import bluej.Config;
import bluej.pkgmgr.Package;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.*;
import java.io.File;
import bluej.utility.DialogManager;
import bluej.utility.filefilter.*;

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
 * @version $Id: PackageChooserStrict.java 1418 2002-10-18 09:38:56Z mik $
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
        super(startDirectory, false);
    }

    /**
     *  Selection approved by button-click. Check whether the selected
     *  directory is a BlueJ package. If so, let it be opened.
     */
    public void approveSelection()   // redefined
    {
    	if (Package.isBlueJPackage(getSelectedFile()))
    	    super.approveSelection();
    }

    /**
     *  A directory was double-clicked. If this is a BlueJ package, consider
     *  this a package selection and accept it as the "Open" action, otherwise
     *  just traverse into the directory.
     */
    public void setCurrentDirectory(File dir)   // redefined
    {
        if (Package.isBlueJPackage(dir)) {
            setSelectedFile(dir);
            super.approveSelection();
        }
        else{
            setSelectedFile(null);
            super.setCurrentDirectory(dir);
        }
    }
}
