package bluej.groupwork.actions;

import java.io.File;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.Config;
import bluej.groupwork.BasicServerResponse;
import bluej.groupwork.InvalidCvsRootException;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.ui.ModuleSelectDialog;
import bluej.groupwork.ui.TeamSettingsDialog;
import bluej.pkgmgr.Import;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.SwingWorker;


/**
 * An action to perform a checkout of a module in CVS. The module is checked
 * out into some directory (chosen by the user) and then opened as a BlueJ
 * project.
 * 
 * @author Kasper
 * @version $Id: CheckoutAction.java 4838 2007-02-07 01:01:21Z davmac $
 */
public class CheckoutAction extends TeamAction
{
    static private CheckoutAction instance = null;

    public CheckoutAction()
    {
        super("team.checkout");
    }

    /**
     * Factory method. This is the way to retrieve an instance of the class,
     * as the constructor is private.
     * @return an instance of the class.
     */
    static public CheckoutAction getInstance()
    {
        if (instance == null) {
            instance = new CheckoutAction();
        }

        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(PkgMgrFrame oldFrame)
    {
        //TeamSettingsController tsc = new TeamSettingsController(projectDir);
        // Create a TeamSettingsController for the current project directory,
        // switch it to the new one once it's been created
        TeamSettingsController tsc = new TeamSettingsController(new File(".").getAbsoluteFile());
        TeamSettingsDialog tsd = new TeamSettingsDialog(tsc);
        tsd.setLocationRelativeTo(oldFrame);
        
        if (tsd.doTeamSettings() == TeamSettingsDialog.OK) {
            ModuleSelectDialog moduleDialog = new ModuleSelectDialog(oldFrame, tsc.getRepository());
            moduleDialog.setLocationRelativeTo(oldFrame);
            moduleDialog.setVisible(true);
            
            String moduleName = moduleDialog.getModuleName();
            if (moduleName != null) {
                // Select parent directory for the new project
                
                String chosenDir = FileUtility.getFileName(oldFrame, Config.getString("team.checkout.filechooser.title"),
                        Config.getString("team.checkout.filechooser.button"), true, null, true);
                
                if (chosenDir != null) {
                    File parentDir = new File(chosenDir);
                    File projectDir = new File(parentDir, moduleName);
                    if (projectDir.exists()) {
                        DialogManager.showError(null, "directory-exists");
                        return;
                    }
                    
                    PkgMgrFrame newFrame;
                    if (oldFrame.isEmptyFrame()) {
                        newFrame = oldFrame;
                    }
                    else {
                        newFrame = PkgMgrFrame.createFrame();
                        if(Config.isJava15()) {
                            newFrame.setLocationByPlatform(true);
                        }
                        newFrame.setVisible(true);
                        newFrame.setEnabled(false);
                    }
                    
                    new CheckoutWorker(newFrame, tsc.getRepository(), projectDir, tsc).start();
                }
            }
        }
    }
    
    /**
     * A worker to perform the checkout operation.
     * 
     * @author Davin McCall
     */
    private class CheckoutWorker extends SwingWorker
    {
        private Repository repository;
        private PkgMgrFrame newFrame;
        private File projDir;
        private TeamSettingsController tsc;
        
        private BasicServerResponse response;
        private boolean failed = true;
        
        public CheckoutWorker(PkgMgrFrame newFrame, Repository repository, File projDir, TeamSettingsController tsc)
        {
            this.newFrame = newFrame;
            this.repository = repository;
            this.projDir = projDir;
            this.tsc = tsc;
        }
        
        /*
         * Get the files from the repository.
         * @see bluej.utility.SwingWorker#construct()
         */
        public Object construct()
        {
            try {
                newFrame.setStatus(Config.getString("team.checkingout"));
                newFrame.startProgress();
                response = repository.checkout(projDir);
                failed = false;
                
                newFrame.stopProgress();
                if (response != null && ! response.isError()) {
                    newFrame.setStatus(Config.getString("team.checkedout"));
                }
            }
            catch (CommandAbortedException e) {
                e.printStackTrace();
                stopProgressBar();
            }
            catch (CommandException e) {
                e.printStackTrace();
                stopProgressBar();
            }
            catch (AuthenticationException e) {
                handleAuthenticationException(e);
            }
            catch (InvalidCvsRootException e) {
                handleInvalidCvsRootException(e);
            }

            newFrame.stopProgress();

            return response;
        }
        
        /*
         * Now open the directory as a BlueJ project.
         * @see bluej.utility.SwingWorker#finished()
         */
        public void finished()
        {
            if (! failed && response != null && ! response.isError()) {
                if (! Project.isBlueJProject(projDir.toString())) {
                    // Try and convert it to a project
                    if (! Import.convertNonBlueJ(newFrame, projDir)) {
                        cleanup();
                        return;
                    }
                }
                
                Project project = Project.openProject(projDir.toString());
                
                project.setTeamSettingsController(tsc);
                Package initialPackage = project.getPackage(project.getInitialPackageName());
                newFrame.openPackage(initialPackage);
                newFrame.setEnabled(true);
            }
            else {
                handleServerResponse(response);
                cleanup();
            }
        }
        
        /**
         * Clean up after failed checkout.
         */
        public void cleanup()
        {
            projDir.delete();
            newFrame.doClose(true);
            // The frame might not have closed if it was the
            // last frame. In that case we want to enable it.
            newFrame.setEnabled(true);
        }
    }
}
