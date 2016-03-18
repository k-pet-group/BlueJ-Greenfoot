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
package bluej.groupwork.git;

import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.DialogManager;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;

/**
 * Git command to push project changes to the upstream repository.
 *
 * @author Fabio Heday
 */
public class GitPushChangesCommand extends GitCommand
{

    public GitPushChangesCommand(GitRepository repository)
    {
        super(repository);
    }

    @Override
    public TeamworkCommandResult getResult()
    {
        try (Git repo = Git.open(this.getRepository().getProjectPath())) {
            PushCommand push = repo.push();
            disableFingerprintCheck(push);
            Iterable<PushResult> pushResults = push.call();
            for (PushResult r:pushResults){
                Iterable<RemoteRefUpdate> updates = r.getRemoteUpdates();
                for (RemoteRefUpdate remoteRefUpdate: updates){
                    if (remoteRefUpdate.getStatus() != Status.OK && remoteRefUpdate.getStatus() != Status.UP_TO_DATE){
                        if (remoteRefUpdate.getMessage() == null){
                            String message = DialogManager.getMessage("team-uptodate-failed");
                            return new TeamworkCommandError(message, message);
                        }
                        return new TeamworkCommandError(remoteRefUpdate.getMessage(),remoteRefUpdate.getMessage());
                    }
                }
            }
        } catch (IOException | GitAPIException ex) {
            return new TeamworkCommandError(ex.getMessage(),ex.getLocalizedMessage());
        }
        return new TeamworkCommandResult();
    }   

}
