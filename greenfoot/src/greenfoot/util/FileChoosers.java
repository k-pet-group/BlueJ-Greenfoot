package greenfoot.util;

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
    
    /**
     * Select a directory to export to.
     * 
     *  @return Returns a File pointing to the export directory, or null if none selected.
     */
    public static File getExportFile(Component parent) {
        if (exportFileChooser == null) {
            exportFileChooser = new JFileChooser();
            exportFileChooser.setDialogTitle("Choose Export Directory");
            exportFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        int result = exportFileChooser.showDialog(parent, "Choose");
        
        if (result != JFileChooser.APPROVE_OPTION) {
           return null;
        }
        return exportFileChooser.getSelectedFile();
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
        scenarioFileChooser.setDialogTitle("Open Scenario");
        int result = scenarioFileChooser.showDialog(parent, "Open");
        
        if (result != JFileChooser.APPROVE_OPTION) {
           return null;
        }
        PrefMgr.setProjectDirectory(scenarioFileChooser.getSelectedFile().getParentFile().getPath());
        
        return scenarioFileChooser.getSelectedFile();
    }
}
