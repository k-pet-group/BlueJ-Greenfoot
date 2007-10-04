package greenfoot.actions;

import greenfoot.core.GProject;
import greenfoot.gui.GreenfootFrame;

import java.awt.event.ActionEvent;
import java.io.File;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;

import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;

/**
 * An action to save a copy of a project into another location.
 * 
 * @author Davin McCall
 */
public class SaveCopyAction extends AbstractAction
{
    private GreenfootFrame gfFrame;
    
    public SaveCopyAction(GreenfootFrame gfFrame)
    {
        super(Config.getString("project.savecopy"));
        this.gfFrame = gfFrame;
        setEnabled(false);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        // get a file name to save under
        String newName = FileUtility.getFileName(gfFrame,
                Config.getString("project.savecopy.title"),
                Config.getString("pkgmgr.saveAs.buttonLabel"), true, null, true);

        if (newName != null) {
            GProject project = gfFrame.getProject();

            int result = FileUtility.COPY_ERROR;
            
            try {
                project.save();

                result = FileUtility.copyDirectory(project.getDir(),
                        new File(newName));
            }
            catch (RemoteException re) {
                re.printStackTrace();
            }
            catch (ProjectNotOpenException pnoe) {
                // can't happen
                pnoe.printStackTrace();
            }

            switch (result) {
                case FileUtility.NO_ERROR:
                    break;

                case FileUtility.DEST_EXISTS:
                    DialogManager.showError(gfFrame, "directory-exists");

                    return;

                case FileUtility.SRC_NOT_DIRECTORY:
                case FileUtility.COPY_ERROR:
                    DialogManager.showError(gfFrame, "cannot-copy-package");

                    return;
            }
        }
    }
}
