package greenfoot.util;

import bluej.Config;
import java.awt.Component;
import java.io.File;


import javax.swing.JFileChooser;

import bluej.prefmgr.PrefMgr;
import bluej.utility.PackageChooserStrict;

/**
 * Class that holds different file choosers that can be used to select files or directories. 
 * 
 * @author Poul Henriksen
 *
 */
public class FileChoosers
{
    private static JFileChooser  exportFileChooser;
    private static JFileChooser  scenarioFileChooser;
    private static JFileChooser  newFileChooser;
    
    /**
     * Let the user specify a new file name.
     * 
     *  @return Returns a File pointing to the export directory, or null if none selected.
     */
    public static File getExportDir(Component parent, File defaultFile, String title) {
        if (exportFileChooser == null) {
            exportFileChooser = new JFileChooser();
            exportFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            exportFileChooser.setDialogTitle(title);
            exportFileChooser.setSelectedFile(defaultFile);
        }
        int result = exportFileChooser.showDialog(parent, Config.getString("chooser.export.button"));
        
        if (result != JFileChooser.APPROVE_OPTION) {
           return null;
        }
        return exportFileChooser.getSelectedFile();
    }

    /**
     * Let the user specify a new file name.
     * 
     *  @return Returns a File pointing to the export directory, or null if none selected.
     */
    public static File getFileName(Component parent, File defaultFile, String title) {
        if (newFileChooser == null) {
            newFileChooser = new JFileChooser();
            newFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            newFileChooser.setDialogTitle(title);
            newFileChooser.setSelectedFile(defaultFile);
        }
        int result = newFileChooser.showDialog(parent, Config.getString("chooser.newFile.button"));
        
        if (result != JFileChooser.APPROVE_OPTION) {
           return null;
        }
        return newFileChooser.getSelectedFile();
    }
    
    /**
     * Select a Greenfoot scenario by using a file chooser, i.e. a file chooser which
     * recognises Greenfoot packages and treats them differently.
     * 
     * @return Returns a File pointing to the scenario directory, or null if none selected.
     */
    public static File getScenario(Component parent)
    {
        if(scenarioFileChooser == null) {
            scenarioFileChooser = new PackageChooserStrict(new File(PrefMgr.getProjectDirectory()));
        }
        scenarioFileChooser.setDialogTitle(Config.getString("chooser.scenario.title"));
        int result = scenarioFileChooser.showDialog(parent, Config.getString("chooser.scenario.button"));
        
        if (result != JFileChooser.APPROVE_OPTION) {
           return null;
        }
        PrefMgr.setProjectDirectory(scenarioFileChooser.getSelectedFile().getParentFile().getPath());
        
        return scenarioFileChooser.getSelectedFile();
    }
}
