package bluej.utility;

import bluej.Config;
import bluej.pkgmgr.Package;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;


/**
 * A file utility for various file related actions. 
 *
 * @version $ $
 * @author Markus Ostman, but most of the methods are just copied across
 * from other parts of bluej.
 */

public class FileUtility
{
    private static PackageChooser pkgChooser = null;
    private static BluejFileChooser fileChooser = null;

    //========================= STATIC METHODS ============================

    /**
     *  Get a file name from the user, using a file selection dialogue.
     *  If cancelled or an invalid name was specified, return null.
     */
    public static String getFileName(Component parent, String title, 
                                     String buttonLabel)
    {
        JFileChooser newChooser = getFileChooser(false);
        newChooser.setDialogTitle(title);

        int result = newChooser.showDialog(parent, buttonLabel);

        if (result == JFileChooser.APPROVE_OPTION)
            return newChooser.getSelectedFile().getPath();
        else if (result == JFileChooser.CANCEL_OPTION)
            return null;
        else {
            DialogManager.showError(parent, "error-no-name");
            return null;
        }
    }

    /**
     * Return a BlueJ package chooser, i.e. a file chooser which
     * recognises BlueJ packages and treats them differently.
     */
    public static PackageChooser getPackageChooser() {
        if(pkgChooser == null)
            pkgChooser = new PackageChooser(
                           Config.getPropString("bluej.defaultProjectPath",
                                                "."));
        return pkgChooser;
    }

    /**
     * return a file chooser for choosing any directory (default behaviour)
     */
    public static JFileChooser getFileChooser(boolean directoryOnly)
    {
        if(fileChooser == null) {
            fileChooser = new BluejFileChooser(Config.getPropString("bluej.defaultProjectPath", "."), directoryOnly);
        }

        return fileChooser;
    }

    //==================== PRIVATE METHODS & CLASSES ========================

    /**
     * To serve our purposes we need to redefine some of the methods
     * in JFileChooser.
     *
     * Why is this Class static? Well since it is a nested class and 
     * it needs to be instantiated in a class (static) method, this 
     * seems to be the only way to do it. 
     * Normally an inner class is instantiated by an instance of the 
     * outer class but in this case it is the outer class itself that 
     * instantiate it.
     */
    private static class BluejFileChooser extends JFileChooser
    {
        /**
         * Create a new BluejFileChooser.
         * @param startDirectory Directory to start the package selection in.
         * @param directoryOnly  Should it display just directories
         **/
        public BluejFileChooser(String startDirectory, boolean directoryOnly)
        {
            super(startDirectory);
            setFileView(new PackageFileView());
            if (directoryOnly)
                setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            else
                setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }

        /**
         * A directory was double-clicked. If it is a BlueJ package maybe 
         * we want to treat it differently
         */
        public void setCurrentDirectory(File dir)    // redefined
        {
            //Here we could treat bluej package differently
            //At the moment nothing is done.
            if (Package.isBlueJPackage(dir)) {
                setSelectedFile(new File("")); 
                super.setCurrentDirectory(dir);
            }
            else{
                setSelectedFile(new File("")); //clear the textfield
                super.setCurrentDirectory(dir);
            }
        }
    }
        
        
}





