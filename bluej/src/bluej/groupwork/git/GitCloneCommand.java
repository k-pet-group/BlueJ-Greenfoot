/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2018,2020  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import static bluej.groupwork.git.GitProvider.connectionDiagnosis;
import bluej.utility.DialogManager;
import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * Clone a remote repository into a local directory.
 * @author Fabio Hedayioglu
 */
public class GitCloneCommand extends GitCommand 
{

    private File clonePath;

    public GitCloneCommand(GitRepository repository, File projectPath) 
    {
        super(repository);
        this.clonePath = projectPath;
    }

    @Override
    public TeamworkCommandResult getResult() 
    {
        String reposUrl= "";
        try {
            reposUrl = getRepository().getReposUrl();
            CloneCommand cloneCommand = Git.cloneRepository();
            disableFingerprintCheck(cloneCommand);
            cloneCommand.setDirectory(clonePath);
            cloneCommand.setURI(reposUrl);
            Git git = cloneCommand.call();
            StoredConfig repoConfig = git.getRepository().getConfig(); //save the repo
            repoConfig.setString("user", null, "name", getRepository().getYourName()); //register the user name
            repoConfig.setString("user", null, "email", getRepository().getYourEmail()); //register the user email
            repoConfig.save();

            //if a specific branch has been requested in the settings, we get and checkout this branch
            String specifiedBranch = getRepository().getBranch();
            if(specifiedBranch != null && specifiedBranch.trim().length() > 0) {
                git.checkout().
                    setCreateBranch(true).
                    setName(specifiedBranch).
                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint("origin/" + specifiedBranch).
                    call();
            }
            
            if (!isCancelled()) {
                return new TeamworkCommandResult();
            }
            
            return new TeamworkCommandAborted();
        } catch (GitAPIException | IOException ex) {
            if (ex.getCause() instanceof NoRemoteRepositoryException){
                String message = DialogManager.getMessage("team-noRepository-uri", ex.getLocalizedMessage());
                return new TeamworkCommandError(message, message);
            }
            if (ex instanceof InvalidRemoteException) {
                return new TeamworkCommandError(DialogManager.getMessage("team-cant-connect"), DialogManager.getMessage("team-cant-connect"));
            }
            if (ex.getCause() instanceof TransportException){
                if (ex.getLocalizedMessage().contains("Auth fail")) {
                    //The problem is the username and password.
                    return new TeamworkCommandError(DialogManager.getMessage("team-denied-invalidUser"), DialogManager.getMessage("team-denied-invalidUser"));
                }
                //problem connecting to the server. we need further diagnosis.
                TeamworkCommandResult diagnosis = connectionDiagnosis(reposUrl);
                if (diagnosis.isError())
                {
                    return diagnosis;
                }
                // Otherwise, return generic message.
            }

            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
    }
}
