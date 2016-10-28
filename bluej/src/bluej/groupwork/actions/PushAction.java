/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
import bluej.groupwork.ui.CommitAndPushInterface;
import bluej.groupwork.ui.TeamSettingsDialog;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.SwingWorker;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javafx.application.Platform;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * This class implements the push action.
 * @author Fabio Heday
 */
public class PushAction extends AbstractAction
{

    private CommitAndPushInterface commitCommentsFrame;
    private Set<File> filesToPush;
    private PushWorker worker;
    private StatusHandle statusHandle;

    public PushAction(CommitAndPushInterface frame)
    {
        super(Config.getString("team.push"));
        commitCommentsFrame = frame;
        this.filesToPush = new HashSet<>();
    }

    /**
     * Set the files which have to be pushed to remote repository.
     * @param filesToPush set of files to push.
     */
    public void setFilesToPush(Set<File> filesToPush)
    {
        this.filesToPush = filesToPush;
    }

    public Set<File> getFilesToPush()
    {
        return this.filesToPush;
    }

    /**
     * Cancel the push, if it is running.
     */
    public void cancel()
    {
        setEnabled(true);
        if (worker != null) {
            worker.abort();
            worker = null;
        }
    }

    /**
     * Set the status handle to use in order to perform the commit operation.
     * @param statusHandle
     */
    @OnThread(Tag.Any)
    public void setStatusHandle(StatusHandle statusHandle)
    {
        this.statusHandle = statusHandle;
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event)
    {
        Project project = commitCommentsFrame.getProject();

        if (project != null) {
            commitCommentsFrame.startProgress();
            commitCommentsFrame.displayMessage(Config.getString("team.push.statusMessage"));
            setEnabled(false);

            //doCommit(project);
            worker = new PushAction.PushWorker(project);
            worker.start();
        }
    }

    /**
     * Worker thread to perform commit operation
     *
     * @author Fabio Heday
     */
    private class PushWorker extends SwingWorker
    {

        private TeamworkCommand command;
        private TeamworkCommandResult result = null;
        private final boolean hasPassword;
        private boolean aborted;

        @OnThread(Tag.Swing)
        public PushWorker(Project project)
        {
            command = statusHandle.pushAll(filesToPush);

            //check if we have the password.
            if (!project.getTeamSettingsController().hasPasswordString()) {
                //ask for the password.
                if (project.getTeamSettingsDialog().doTeamSettings() == TeamSettingsDialog.CANCEL) {
                    //user cancelled.
                    commitCommentsFrame.setVisible(true);
                    hasPassword = false;
                    return;
                }
                //update command. now with password.
                statusHandle.getRepository().setPassword(commitCommentsFrame.getProject().getTeamSettingsController().getTeamSettingsDialog().getSettings());
            }
            hasPassword = true;
        }

        @Override
        public Object construct()
        {
            if (!hasPassword)
            {
                abort();
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
                if (!result.isError() && !result.wasAborted()) {
                    DataCollector.teamCommitProject(project, statusHandle.getRepository(), filesToPush);
                    EventQueue.invokeLater(() -> {
                        commitCommentsFrame.displayMessage(Config.getString("team.push.statusDone"));
                    });
                }
            }

            Platform.runLater(() -> TeamUtils.handleServerResponseFX(result, commitCommentsFrame.asWindow()));

            if (!aborted) {
                setEnabled(true);
                if (project.getTeamSettingsController().isDVCS()){
                    //do not close window, just update its contents.
                    commitCommentsFrame.setVisible(true);
                } else {
                    //close window.
                    commitCommentsFrame.setVisible(false);
                }
            }
        }
    }

}
