package bluej.utility;

import javax.swing.*;
import java.io.File;

/**
 * A modified JFileChooser. Modifications are made for
 * displaying BlueJ packages with a specific icon and to clear the selection
 * field after traversing into a directory.
 *
 * @author Michael Kolling
 * @version $Id: BlueJFileChooser.java 1819 2003-04-10 13:47:50Z fisker $
 */
class BlueJFileChooser extends JFileChooser
{
    /**
     * Create a new BlueJFileChooser.
     *
     * @param   startDirectory  directory to start the package selection in.
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
        
        //commented out post 1.1.6 to fix null pointer issue with J2SDK 1.4
        //setSelectedFile(null);              //clear the textfield
        super.setCurrentDirectory(dir);
    }
}
