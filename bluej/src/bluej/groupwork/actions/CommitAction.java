/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2014,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
import java.util.HashSet;
import java.util.Set;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.groupwork.StatusHandle;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.ui.CommitAndPushFrame;
import bluej.pkgmgr.Project;
import bluej.utility.FXWorker;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An action to do an actual commit.
 * 
 * <p>This action should not be enabled until the following methods have
 * been called:
 * 
 * <ul>
 * <li>setNewFiles()
 * <li>setDeletedFiles()
 * <li>setFiles()
 * <li>setStatusHandle()
 * </ul>
 * 
 * @author Kasper
 */
@OnThread(Tag.FXPlatform)
public class CommitAction extends TeamAction
{
    private Set<File> newFiles; // which files are new files
    private Set<File> deletedFiles; // which files are to be removed
    private Set<File> files; // files to commit (includes both of above)
    private CommitAndPushFrame commitCommentsFrame;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private StatusHandle statusHandle;
    
    private CommitWorker worker;
    
    public CommitAction(CommitAndPushFrame frame)
    {
        super(Config.getString("team.commitButton"), false);
        commitCommentsFrame = frame;
    }
    
    /**
     * Set the files which are new, that is, which aren't presently under
     * version management and which need to be added. If the version management
     * system versions directories, the set must be ordered and new directories
     * must precede any files they contain.
     */
    public void setNewFiles(Set<File> newFiles)
    {
        this.newFiles = newFiles;
    }
    
    /**
     * Set the files which have been deleted locally, and the deletion
     * needs to be propagated to the repository.
     */
    public void setDeletedFiles(Set<File> deletedFiles)
    {
        this.deletedFiles = deletedFiles;
    }
    
    /**
     * Set all files which are to be committed. This should include both
     * the new files and the deleted files, as well as any other files
     * which have been locally modified and need to be committed.
     */
    public void setFiles(Set<File> files)
    {
        this.files = files;
    }
    
    /**
     * Set the status handle to use in order to perform the commit operation.
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
        commitCommentsFrame.displayMessage(Config.getString("team.commit.statusMessage"));

        worker = new CommitWorker();
        worker.start();
    }
    
    /**
     * Cancel the commit, if it is running.
     */
    public void cancel()
    {
        if(worker != null) {
            worker.abort();
            worker = null;
        }
    }

    /**
     * Worker thread to perform commit operation
     * 
     * @author Davin McCall
     */
    private class CommitWorker extends FXWorker
    {
        private TeamworkCommand command;
        private TeamworkCommandResult result;
        private boolean aborted;

        @OnThread(Tag.FXPlatform)
        public CommitWorker()
        {
            String comment = commitCommentsFrame.getComment();
            Set<TeamStatusInfo> forceFiles = new HashSet<>();
            
            //last step before committing is to add in modified diagram 
            //layouts if selected in commit comments dialog
            if(commitCommentsFrame.includeLayout()) {
                forceFiles = commitCommentsFrame.getChangedLayoutInfo();
                files.addAll(commitCommentsFrame.getChangedLayoutFiles());
            }

            Set<File> binFiles = TeamUtils.extractBinaryFilesFromSet(newFiles);

            command = statusHandle.commitAll(newFiles, binFiles, deletedFiles, files,
                    forceFiles, comment);
        }

        @OnThread(Tag.Worker)
        public Object construct()
        {
            result = command.getResult();
            return result;
        }
        
        public void abort()
        {
            command.cancel();
            aborted = true;
        }
        
        public void finished()
        {
            final Project project = commitCommentsFrame.getProject();
            
            if (! aborted) {
                commitCommentsFrame.stopProgress();
                if (! result.isError() && ! result.wasAborted()) {
                    DataCollector.teamCommitProject(project, statusHandle.getRepository(), files);
                    commitCommentsFrame.displayMessage(Config.getString("team.commit.statusDone"));
                }
            }
            
            TeamUtils.handleServerResponseFX(result, commitCommentsFrame.asWindow());
            
            if (! aborted) {
                commitCommentsFrame.setVisible();
            }
        }
    }
}
