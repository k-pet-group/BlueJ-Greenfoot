/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.groupwork.actions;

import java.io.File;

import bluej.Config;
import bluej.groupwork.*;
import bluej.groupwork.ui.ModuleSelectDialog;
import bluej.groupwork.ui.TeamSettingsDialog;
import bluej.pkgmgr.Import;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.SwingWorker;


/**
 * An action to perform a checkout of a module in CVS. The module is checked
 * out into some directory (chosen by the user) and then opened as a BlueJ
 * project.
 * 
 * @author Kasper
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
        TeamSettingsDialog tsd = tsc.getTeamSettingsDialog();
        tsd.setLocationRelativeTo(oldFrame);
        
        if (tsd.doTeamSettings() == TeamSettingsDialog.OK) {
            ModuleSelectDialog moduleDialog = new ModuleSelectDialog(oldFrame, tsc.getRepository(true));
            moduleDialog.setLocationRelativeTo(oldFrame);
            moduleDialog.setVisible(true);
            
            String moduleName = moduleDialog.getModuleName();
            if (moduleName != null) {
                // Select parent directory for the new project
                
                File parentDir = FileUtility.getDirName(oldFrame, Config.getString("team.checkout.filechooser.title"),
                        Config.getString("team.checkout.filechooser.button"), true, true);
                
                if (parentDir != null) {
                    
                    if (Package.isPackage(parentDir)) {
                        Debug.message("Attempted to checkout a project into an existing project: " + parentDir);
                        DialogManager.showError(null, "team-cannot-import-into-existing-project");
                        return;
                    }
                    
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
                    
                    new CheckoutWorker(newFrame, tsc.getRepository(true), projectDir, tsc).start();
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
        
        private TeamworkCommandResult response;
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
            newFrame.setStatus(Config.getString("team.checkingout"));
            newFrame.startProgress();
            TeamworkCommand checkoutCmd = repository.checkout(projDir);
            response = checkoutCmd.getResult();

            failed = response.isError();

            newFrame.stopProgress();
            if (! failed) {
                newFrame.setStatus(Config.getString("team.checkedout"));
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
            if (! failed) {
                if (! Project.isProject(projDir.toString())) {
                    // Try and convert it to a project
                    if (! Import.convertNonBlueJ(newFrame, projDir)) {
                        cleanup();
                        return;
                    }
                }
                
                Project project = Project.openProject(projDir.toString(), newFrame);
                
                project.setTeamSettingsController(tsc);
                Package initialPackage = project.getPackage(project.getInitialPackageName());
                newFrame.openPackage(initialPackage);
                newFrame.setEnabled(true);
            }
            else {
                TeamUtils.handleServerResponse(response, newFrame);
                cleanup();
            }
        }
        
        /**
         * Clean up after failed checkout.
         */
        public void cleanup()
        {
            projDir.delete();
            newFrame.doClose(true, false);
            // The frame might not have closed if it was the
            // last frame. In that case we want to enable it.
            newFrame.setEnabled(true);
        }
    }
}
