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
import java.io.IOException;
import java.util.Collection;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * Git command to share a project
 *
 * @author Fabio Heday
 */
public class GitShareCommand extends GitCommand
{

    public GitShareCommand(GitRepository repository)
    {
        super(repository);
    }

    @Override
    public TeamworkCommandResult getResult()
    {
        try {
            if (isRemoteRepoEmpty()) {
                //remote repository is empty, we can proceed initializing the
                //repository.
                //create gitRepo locally
                Git.init().setDirectory(getRepository().getProjectPath()).call();

                Git repo = Git.open(this.getRepository().getProjectPath());
                StoredConfig config = repo.getRepository().getConfig();
                //set remote repository
                config.setString("remote", "origin", "url", getRepository().getReposUrl());
                config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
                config.setString("branch", "master", "remote", "origin");
                config.setString("branch", "master", "merge", "refs/heads/master");
                config.save();
                repo.close();
            } else {
                //remote repository is not empty, fail.
                return new TeamworkCommandError("Remote Git repository already has a project.", "Remote Git repository already has a project.");
            }
        } catch (GitAPIException | IOException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
        return new TeamworkCommandResult();
    }

    private boolean isRemoteRepoEmpty() throws GitAPIException
    {
        try {
            String gitUrl = getRepository().getReposUrl();

            //perform a lsRemote on the remote git repo.
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
            lsRemoteCommand.setRemote(gitUrl); //configure remote repository address.
            lsRemoteCommand.setCredentialsProvider(getRepository().getCredentialsProvider()); //associate the repository to the username and password.
            disableFingerprintCheck(lsRemoteCommand);
            lsRemoteCommand.setTags(false); //disable refs/tags in reference results
            lsRemoteCommand.setHeads(false); //disable refs/heads in reference results

            Collection<Ref> ref; //executes the lsRemote commnand.
            ref = lsRemoteCommand.call();
            return ref.isEmpty();
        } catch (GitAPIException ex) {
            throw ex;
        }
    }

}
