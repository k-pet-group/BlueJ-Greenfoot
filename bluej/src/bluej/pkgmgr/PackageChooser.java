package bluej.pkgmgr;

import bluej.utility.Utility;
import bluej.Config;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import bluej.utility.DialogManager;

/**
 * A file chooser for opening packages. Extends the behaviour of JFileChooser
 * in the following ways: <BR><BR>
 * Only directories (either BlueJ packages or plain ones) are displayed. <BR>
 * BlueJ packages are displayed with a different icon. <BR>
 * A double-click on a BlueJ package returns it rather than showing it's
 * content. <BR>
 * If a non-BlueJ directory is selected by button-click the user is asked  
 * whether this directory should be imported. <BR>
 *
 * @version $ $
 * @author Michael Kolling
 * @author Axel Schmolitzky
 */

class PackageChooser extends JFileChooser
{
    /**
     * Create a new PackageChooser.
     * @param startDirectory the directory to start the package selection in.
     **/
    public PackageChooser(String startDirectory)
    {
	super(startDirectory);
        setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        setFileView(new PackageFileView());
        setDialogTitle(Config.getString("pkgmgr.openPkg.title"));
        setApproveButtonText(Config.getString("pkgmgr.openPkg.buttonLabel"));
    }

    /**
     *  Selection approved by button-click. Check whether the selected 
     *  directory is a BlueJ package. If so, let it be opened. Otherwise ask 
     *  the user whether to import the java source files in the selected 
     *  directory into a new package.
     */
    public void approveSelection() {   // redefined
	if (Package.isBlueJPackage(getSelectedFile()))
	    super.approveSelection();
	else {
	    int answer = DialogManager.askQuestion(this,
                                                   "really-import-package");
	    if (answer == 0)  // OK
		super.approveSelection();
	}
    }

    /**
     *  A directory was double-clicked. If this is a BlueJ package, consider 
     *  this a package selection and accept it as the "Open" action, otherwise
     *  just traverse into the directory.
     */
    public void setCurrentDirectory(File dir) {    // redefined
	if (Package.isBlueJPackage(dir)) {
	    setSelectedFile(dir);
	    super.approveSelection();
	}
	else
	    super.setCurrentDirectory(dir);
    }
}

