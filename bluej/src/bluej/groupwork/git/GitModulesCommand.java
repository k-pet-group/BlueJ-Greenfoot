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

import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.pkgmgr.BlueJPackageFile;
import com.google.common.io.Files;
import java.io.File;
import java.util.List;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Command to retrieve a list of blueJ projects from a remote Git repository.
 * @author Fabio Hedayioglu
 */
class GitModulesCommand extends GitCommand
{

    private List<String> modulesList;

    public GitModulesCommand(GitRepository repository, List<String> modules)
    {
        super(repository);
        this.modulesList = modules;
    }

    @Override
    public TeamworkCommandResult getResult()
    {
        
        try {
            if (!isCancelled()) {
                String reposUrl = getRepository().getReposUrl();

                File tmpFile = Files.createTempDir();
                tmpFile.deleteOnExit(); //remove file, if exits
                CloneCommand cloneCommand = Git.cloneRepository();
                disableFingerprintCheck(cloneCommand);
                cloneCommand.setDirectory(tmpFile);
                cloneCommand.setURI(reposUrl);
                cloneCommand.call();
                
                String[] directories = tmpFile.list((File current, String name) -> new File(current, name).isDirectory());
                
                for (String i:directories){
                    //exclude directories that starts with . (e.g. .git) and
                    //checks if the directory is a valid BlueJ project
                    if (!i.startsWith(".") && BlueJPackageFile.exists(new File(tmpFile, i)) ) {
                        modulesList.add(i);
                    }
                }

            }
            return new TeamworkCommandResult();
        } catch (GitAPIException ex) {
            return new TeamworkCommandError(ex.getMessage(), ex.getLocalizedMessage());
        }
    }

}
