package bluej.pkgmgr;

import bluej.utility.Utility;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import bluej.utility.DialogManager;

/**
 ** @author Michael Kolling
 **
 ** Chooser for opening packages
 **/

class JBPackageChooser extends JFileChooser
{
    public JBPackageChooser(String startDirectory)
    {
	super(startDirectory);
    }

    /**
     *  "Open" has been selected. Check whether the selected directory
     *  is a BlueJ package. If so, let it be opened. Otherwise ask the user
     *  whether to import the java source files in the selected directory
     *  into a new package.
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
     *  A directory was opened. If this is a BlueJ package, consider this a
     *  package selection and accept this as "Open" action, otherwise just
     *  traverse into the directory.
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

