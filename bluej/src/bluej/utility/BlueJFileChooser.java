package bluej.utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;

import bluej.pkgmgr.Package;

/**
 * BlueJFileChooser - a modified JFileChooser. Modifications are made for
 * displaying BlueJ packages with a specific icon and to clear the selection
 * field after traversing into a directory.
 *
 * @author Michael Kolling
 * @version 1.0
 */
class BlueJFileChooser extends JFileChooser
{
    /**
     * Create a new BlueJFileChooser.
     * @param   startDirectory  Directory to start the package selection in.
     */
    public BlueJFileChooser(String startDirectory)
    {
        super(startDirectory);
        setFileView(new PackageFileView());
    }

    /**
     * A directory was double-clicked. If it is a BlueJ package maybe
     * we want to treat it differently
     */
    public void setCurrentDirectory(File dir)    // redefined
    {
        //Here we could treat bluej package differently
        //At the moment nothing is done.
        //if (Package.isBlueJPackage(dir)) { ...

        setSelectedFile(new File("")); //clear the textfield
        super.setCurrentDirectory(dir);
    }
}
