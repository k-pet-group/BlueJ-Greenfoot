package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;

/**
 ** @author Michael Kolling
 **
 ** Chooser for opening packages
 **/

public class JBPackageChooser extends JFileChooser
{
    public JBPackageChooser(String startDirectory)
    {
	super(startDirectory);
    }

    /**
     *  "Open" has been selected. Check whether the selection is really a BlueJ 
     *  package. If so, go ahead, open it, otherwise (it was just a normal directory)
     *  change to that directory instead.
     */
    public void approveSelection() {   // redefined
	if (isJBPackage(getSelectedFile()))
	    super.approveSelection();
	else
	    super.setCurrentDirectory(getSelectedFile());
    }

    /**
     *  A directory was opened. If this is a BlueJ package, consider this a
     *  package selection and accept this as "Open" action, otherwise just
     *  traverse into the directory.
     */
    public void setCurrentDirectory(File dir) {    // redefined
	if (isJBPackage(dir)) {
	    setSelectedFile(dir);
	    super.approveSelection();
	}
	else
	    super.setCurrentDirectory(dir);
    }

    /**
     *  return true if the directory is a BlueJ package
     */
    private boolean isJBPackage(File directory) {
	if (directory == null)
	    return false;

  	File packageFile = new File(directory, Package.pkgfileName);
  	return (packageFile.exists());
    }
}

