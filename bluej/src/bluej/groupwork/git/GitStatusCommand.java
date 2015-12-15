/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.git;

import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommandResult;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;

/**
 * Checks the status of a Git repository
 *
 * @author Fabio Hedayioglu
 */
public class GitStatusCommand extends GitCommand
{
    StatusListener listener;
    FileFilter filter;
    boolean includeRemote;

    public GitStatusCommand(GitRepository repository, StatusListener listener, FileFilter filter, boolean includeRemote) 
    {
        super(repository);
        this.listener = listener;
        this.filter = filter;
        this.includeRemote = includeRemote;
    }

    @Override
    public TeamworkCommandResult getResult()
    {
        LinkedList<TeamStatusInfo> returnInfo = new LinkedList<>();
        
        try (Git repo = Git.open(this.getRepository().getProjectPath().getParentFile())) {
            Status s = repo.status().call();

            File gitPath = new File(this.getRepository().getProjectPath().getParent());
            s.getMissing().stream().map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSCHECKOUT)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });
            
            s.getUncommittedChanges().stream().map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSCOMMIT)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });
            
            s.getConflicting().stream().map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSMERGE)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

            s.getUntracked().stream().map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSADD)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });
            
            s.getUntrackedFolders().stream().map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_NEEDSADD)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });
            
            s.getRemoved().stream().map((item) -> new TeamStatusInfo(new File(gitPath, item), "", null, TeamStatusInfo.STATUS_REMOVED)).forEach((teamInfo) -> {
                returnInfo.add(teamInfo);
            });

        } catch (IOException | GitAPIException | NoWorkTreeException ex) {
            Logger.getLogger(GitStatusCommand.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (listener != null) {
            while (!returnInfo.isEmpty()) {
                TeamStatusInfo teamInfo = (TeamStatusInfo) returnInfo.removeFirst();
                listener.gotStatus(teamInfo);
            }
            listener.statusComplete(new GitStatusHandle(getRepository()));
        }
        return new TeamworkCommandResult();
    }

}
