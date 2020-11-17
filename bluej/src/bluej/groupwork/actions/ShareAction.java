/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2014,2016,2017,2019,2020  Michael Kolling and John Rosenberg
 
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
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamSettingsController;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.Utility;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An action to share a project into a repository.
 * 
 * @author Kasper
 */
@OnThread(Tag.FXPlatform)
public class ShareAction extends TeamAction
{
    public ShareAction()
    {
        super("team.share", true);
    }
    
    @Override
    public void actionPerformed(PkgMgrFrame pmf)
    {
        Project project = pmf.getProject();
        
        if (project == null) {
            return;
        }

        doShare(pmf, project);
    }

    private void doShare(final PkgMgrFrame pmf, final Project project)
    {
        // The team settings controller is not initially associated with the
        // project, so you can still modify the repository location
        final TeamSettingsController tsc = new TeamSettingsController(project.getProjectDir());
        Repository repository = tsc.trytoEstablishRepository(true, true);
        if (repository == null) {
            // User cancelled, or there is an error in establishing the repository
            return;
        }
        try {
            project.saveAll();
            project.saveAllEditors();
        }
        catch(IOException ioe) {
            String msg = DialogManager.getMessage("team-error-saving-project");
            if (msg != null) {
                String finalMsg = Utility.mergeStrings(msg, ioe.getLocalizedMessage());
                DialogManager.showErrorTextFX(pmf.getWindow(), finalMsg);
                return;
            }
        }
        pmf.setStatus(Config.getString("team.sharing"));
        pmf.startProgress();
        
        Thread thread = new Thread() {
            
            TeamworkCommandResult result = null;

            @OnThread(value = Tag.Worker, ignoreParent = true)
            public void run()
            {
                // boolean resetStatus = true;
                TeamworkCommand command = repository.shareProject();
                result = command.getResult();

                if (! result.isError())
                {
                    // Run and Wait
                    CompletableFuture<Set<File>> filesFuture = new CompletableFuture<>();

                    Platform.runLater( () -> {
                        project.setTeamSettingsController(tsc);
                        Set<File> projFiles = tsc.getProjectFiles(true);
                        // Make copy, to ensure thread safety:
                        filesFuture.complete(new HashSet<>(projFiles));
                    });

                    try {
                        Set<File> files = filesFuture.get();

                        Set<File> newFiles = new LinkedHashSet<>(files);
                        Set<File> binFiles = TeamUtils.extractBinaryFilesFromSet(newFiles);
                        command = repository.commitAll(newFiles, binFiles, Collections.emptySet(),
                                files, Config.getString("team.share.initialMessage"));
                        result = command.getResult();
                        command = repository.pushChanges();
                        result = command.getResult();
                    }
                    catch (InterruptedException | ExecutionException e)
                    {
                        Debug.reportError(e);
                    }
                }

                Platform.runLater(() -> {
                    TeamUtils.handleServerResponseFX(result, pmf.getWindow());
                    pmf.stopProgress();
                    if (!result.isError())
                    {
                        pmf.setStatus(Config.getString("team.shared"));
                        DataCollector.teamShareProject(project, repository);
                    }
                    else
                    {
                        pmf.clearStatus();
                    }
                });
            }
        };
        thread.start();
    }
}
