/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2019  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.ui.CommitAndPushFrame;
import bluej.pkgmgr.Project;
import bluej.utility.FXWorker;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Platform;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class implements the push action.
 * @author Fabio Heday
 */
@OnThread(Tag.FXPlatform)
public class PushAction extends TeamAction
{
    private CommitAndPushFrame commitCommentsFrame;
    private Set<File> filesToPush;
    private PushWorker worker;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private StatusHandle statusHandle;

    public PushAction(CommitAndPushFrame frame)
    {
        super(Config.getString("team.push"), false);
        commitCommentsFrame = frame;
        this.filesToPush = new HashSet<>();
    }

    /**
     * Set the status handle to use in order to perform the commit operation.
     * @param statusHandle
     */
    @OnThread(Tag.Worker)
    public synchronized void setStatusHandle(StatusHandle statusHandle)
    {
        this.statusHandle = statusHandle;
    }

    @Override
    protected void actionPerformed(Project project)
    {
        commitCommentsFrame.startProgress();
        commitCommentsFrame.displayMessage(Config.getString("team.push.statusMessage"));
        setEnabled(false);
        this.filesToPush.clear();
        filesToPush.addAll(commitCommentsFrame.getFilesToPush());

        worker = new PushAction.PushWorker(project);
        worker.start();
    }

    /**
     * Worker thread to perform commit operation
     *
     * @author Fabio Heday
     */
    private class PushWorker extends FXWorker
    {
        private TeamworkCommand command;
        private TeamworkCommandResult result = null;
        private final boolean hasPassword;
        private boolean aborted;

        @OnThread(Tag.FXPlatform)
        public PushWorker(Project project)
        {
            command = statusHandle.pushAll(filesToPush);

            //check if we have the password.
            if (!project.getTeamSettingsController().hasPasswordString()) {
                //ask for the password.
                if ( ! project.getTeamSettingsDialog().showAndWait().isPresent() ) {
                    //user cancelled.
                    commitCommentsFrame.setVisible();
                    hasPassword = false;
                    return;
                }
                //update command. now with password.
                statusHandle.getRepository().setPassword(commitCommentsFrame.getProject().getTeamSettingsController().getTeamSettingsDialog().getSettings());
            }
            hasPassword = true;
        }

        @Override
        @OnThread(Tag.Worker)
        public Object construct()
        {
            if (!hasPassword)
            {
                Platform.runLater(this::abort);
                return null;
            }
            result = command.getResult();
            return result;
        }

        public void abort()
        {
            command.cancel();
            aborted = true;
        }

        @Override
        public void finished()
        {
            final Project project = commitCommentsFrame.getProject();

            if (!aborted) {
                commitCommentsFrame.stopProgress();
                if (!result.isError()) {
                    if ( !result.wasAborted()) {
                        DataCollector.teamPushProject(project, statusHandle.getRepository(), filesToPush);
                        commitCommentsFrame.displayMessage(Config.getString("team.push.statusDone"));
                    }
                }
                else { // result is Error
                    commitCommentsFrame.displayMessage(Config.getString("team.push.error"));
                }
            }

            TeamUtils.handleServerResponseFX(result, commitCommentsFrame.asWindow());

            if (!aborted) {
                setEnabled(true);
                //do not close window, just update its contents.
                commitCommentsFrame.setVisible();
            }
        }
    }
}
