/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2019  Michael Kolling and John Rosenberg
 
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
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git command to fetch project changes from the upstream repository.
 * This command essentially updates the local copy of the remote branch
 * @author Fabio Heday
 */
public class GitFetchCommand extends GitCommand
{
    public GitFetchCommand(GitRepository repository)
    {
        super(repository);
    }
    
      @Override
    public TeamworkCommandResult getResult()
    {
        try (Git repo = Git.open(this.getRepository().getProjectPath())) {
            
            FetchCommand fetch = repo.fetch();
            disableFingerprintCheck(fetch);
            fetch.call();
        } catch (IOException | GitAPIException ex) {
            return new TeamworkCommandError(ex.getMessage(),ex.getLocalizedMessage());
        }
        return new TeamworkCommandResult();
    }
    
}
