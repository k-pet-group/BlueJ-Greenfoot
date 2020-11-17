/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2015,2016,2017,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommand;
import bluej.utility.Debug;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 *
 * @author Fabio Hedayioglu
 */
public class GitRepository implements Repository
{

    private final File projectPath;
    private final String protocol; // Only for data collection
    private String reposUrl;

    private final String userName;
    private String password;
    private final String yourName;
    private final String yourEmail;
    private final String branch;

    /**
     * Create a Git repository when all fields are known. Usually when cloning a
     * repository.
     * @param projectPath path to save the project to.
     * @param protocol protocol used when communicating to the server
     * @param reposUrl repository's path on the remote server
     * @param branch the optional branch
     * @param userName user name to be used to authenticate on the server
     * @param password user password
     * @param yourName user name to be registered on the local git repository
     * @param yourEmail user e-mail to be registered on the local git repository
     */
    public GitRepository(File projectPath, String protocol, String reposUrl, String branch, String userName, String password, String yourName, String yourEmail)
    {
        this.projectPath = projectPath;
        this.protocol = protocol;
        this.reposUrl = reposUrl;
        this.branch = branch;
        this.userName = userName;
        this.password = password;
        this.yourName = yourName;
        this.yourEmail = yourEmail;
    }

    public String getReposUrl() 
    {
        return this.reposUrl;
    }

    @Override
    public void setPassword(TeamSettings newSettings) 
    {
        this.password = newSettings.getPassword();
    }
    
    @Override
    public boolean versionsDirectories() 
    {
        return true;
    }

    @Override
    public TeamworkCommand checkout(File projectPath) 
    {
        return new GitCloneCommand(this, projectPath);
    }

    @Override
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files, String commitComment)
    {
        //we dont' need a list of binary files and regular files. merge them.
        newFiles.addAll(binaryNewFiles);

        return new GitCommitAllCommand(this, newFiles, deletedFiles, files, commitComment);
    }

    @Override
    public TeamworkCommand shareProject() 
    {
        return new GitShareCommand(this);
    }

    @Override
    public TeamworkCommand pushChanges()
    {
        return new GitPushChangesCommand(this);
    }

    @Override
    public TeamworkCommand getStatus(StatusListener listener, FileFilter filter, boolean includeRemote) 
    {
        return new GitStatusCommand(this, listener, filter, includeRemote);
    }

    @Override
    public TeamworkCommand getModules(List<String> modules)
    {
        return null; // not necessary on Git repositories.
    }

    @Override
    public TeamworkCommand getLogHistory(LogHistoryListener listener) 
    {
        return new GitHistoryCommand(this, listener);
    }

    @Override
    public boolean prepareDeleteDir(File dir) 
    {
        return true;
    }

    @Override
    public void prepareCreateDir(File dir) 
    {
        
    }

    @Override
    public FileFilter getMetadataFilter() 
    {
        return new FileFilter()
        {
            public boolean accept(File pathname) 
            {
                return !pathname.getName().equals(".git");
            }
        };
    }

    @Override
    public void getAllLocallyDeletedFiles(Set<File> files)
    {
        
        try (Git repo = Git.open(getProjectPath())) {
            Status s = repo.status().call();

            Set<String> filesStr = s.getMissing();
            filesStr.stream().forEach((fileName) -> {
                files.add(new File(fileName));
            });
        }catch (IOException | GitAPIException | NoWorkTreeException ex) {
            Debug.reportError("Git get all locally deleted command exception", ex);
        }
    }

    @Override
    public String getVCSType() 
    {
        return "Git";
    }

    @Override
    public String getVCSProtocol() 
    {
        return protocol;
    }
    
    public UsernamePasswordCredentialsProvider getCredentialsProvider() 
    {
        // set a configuration with username and password.
        UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(userName,
                password == null ? "" : password);
        return cp;
    }

    /**
     * @return the yourName
     */
    public String getYourName() 
    {
        return yourName;
    }

    /**
     * @return the yourEmail
     */
    public String getYourEmail() 
    {
        return yourEmail;
    }
    
    protected File getProjectPath() 
    {
        return this.projectPath;
    }

    public String getBranch(){
        return branch;
    }
}
