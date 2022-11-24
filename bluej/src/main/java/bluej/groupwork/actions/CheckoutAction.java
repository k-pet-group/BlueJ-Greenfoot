/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2015,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.Import;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.FXWorker;
import bluej.utility.javafx.FXPlatformConsumer;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An action to perform a checkout of a module in CVS. The module is checked
 * out into some directory (chosen by the user) and then opened as a BlueJ
 * project.
 *
 * @author Kasper
 */
@OnThread(Tag.FXPlatform)
public class CheckoutAction extends TeamAction
{
    public CheckoutAction()
    {
        super("team.checkout", true);
    }

    public void actionPerformed(PkgMgrFrame oldFrame)
    {
        //TeamSettingsController tsc = new TeamSettingsController(projectDir);
        // Create a TeamSettingsController for the current project directory,
        // switch it to the new one once it's been created
        final TeamSettingsController tsc = new TeamSettingsController(new File(".").getAbsoluteFile());

        if (tsc.getTeamSettingsDialog().showAndWait().isPresent()) {
            Repository repository = tsc.trytoEstablishRepository(true);
            if (repository == null) {
                // The repository can't be established, so no need to establish the worker
                return;
            }

            FXPlatformConsumer<File> finishCheckout = projectDir -> {
                PkgMgrFrame newFrame;
                if (oldFrame.isEmptyFrame())
                {
                    newFrame = oldFrame;
                }
                else
                {
                    newFrame = PkgMgrFrame.createFrame();
                    newFrame.setVisible(true);
                }

                new CheckoutWorker(newFrame, repository, projectDir, tsc).start();
            };

            File projectDir = FileUtility.getSaveProjectFX(oldFrame.getProject(),oldFrame.getWindow(),
                    Config.getString("team.checkout.DVCS.filechooser.title"));
            if (projectDir == null)
                return;//no project dir. nothing to do.

            if (Package.isPackage(projectDir))
            {
                Debug.message("Attempted to checkout a project into an existing project: " + projectDir);
                DialogManager.showErrorFX(null, "team-cannot-import-into-existing-project");
                return;
            }
            finishCheckout.accept(projectDir);
        }
    }

    /**
     * A worker to perform the checkout operation.
     *
     * @author Davin McCall
     */
    private class CheckoutWorker extends FXWorker
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
         * @see bluej.utility.FXWorker#construct()
         */
        @OnThread(Tag.Worker)
        public Object construct()
        {
            newFrame.setStatus(Config.getString("team.checkingout"));
            newFrame.startProgress();

            // Repository can't be null; checked before constructing the worker
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
         * @see bluej.utility.FXWorker#finished()
         */
        public void finished()
        {
            if (! failed) {
                if (! Project.isProject(projDir.toString())) {
                    // Try and convert it to a project
                    if (! Import.convertNonBlueJ(newFrame::getWindow, projDir)) {
                        cleanup();
                        return;
                    }
                }

                Project project = Project.openProject(projDir.toString());

                project.setTeamSettingsController(tsc);
                Package initialPackage = project.getPackage(project.getInitialPackageName());
                newFrame.openPackage(initialPackage, newFrame);
            }
            else {
                TeamUtils.handleServerResponseFX(response, newFrame.getWindow());
                cleanup();
            }
        }

        /**
         * Clean up after failed checkout.
         */
        public void cleanup()
        {
            deleteDirectory(projDir);
            projDir.delete();
            newFrame.doClose(true, false);
        }
        
        /**
         * deletes all files and subdirectories inside a directory
         * @param dir 
         */
        private void deleteDirectory(File dir)
        {
            if (dir != null && dir.listFiles() != null) {
                for (File f : dir.listFiles()) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    }
                    f.delete();
                }
            }
        }

        public void abort() {}
    }
}
