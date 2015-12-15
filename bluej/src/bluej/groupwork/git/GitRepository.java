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

import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommand;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
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

    private final FileRepositoryBuilder fileRepoBuilder;
    private final String userName;
    private String password;
    private String yourName;
    private String yourEmail;

    /**
     * Create a Git repository when all fields are known. Usually when cloning a
     * repository.
     * @param projectPath path to save the project to.
     * @param protocol protocol used when communicating to the server
     * @param reposUrl repository's path on the remote server
     * @param fileRepoBuilder git object to create repositories
     * @param userName user name to be used to authenticate on the server
     * @param password user password
     * @param yourName user name to be registered on the local git repository
     * @param yourEmail user e-mail to be registered on the local git repository
     */
    public GitRepository(File projectPath, String protocol, String reposUrl, FileRepositoryBuilder fileRepoBuilder, String userName, String password, String yourName, String yourEmail)
    {
        this.projectPath = projectPath;
        this.protocol = protocol;
        this.reposUrl = reposUrl;
        this.fileRepoBuilder = fileRepoBuilder;
        this.userName = userName;
        this.password = password;
        this.yourName = yourName;
        this.yourEmail = yourEmail;
    }
    
    /**
     * opens the Git repository and returns the stored Name
     * @param projectPath path to the BlueJ project
     * @return String with the stored name in the Git repo.
     */
    static public String getYourNameFromRepo(File projectPath){
        String result = null;
        try {
            try (Git repo = Git.open(projectPath.getParentFile())) {
                StoredConfig repoConfig = repo.getRepository().getConfig(); //get repo config
                result = repoConfig.getString("user", null, "name"); //recover the user name
                repo.close();
            } //close the repo
        } catch (IOException ex) { }
        return result;
    }
    
    
    /**
     * opens the Git repository and returns the stored email
     * @param projectPath path to the BlueJ project
     * @return String with the stored email in the Git repo.
     */    
    static public String getYourEmailFromRepo(File projectPath){
        String result = null;
        try {
            try (Git repo = Git.open(projectPath.getParentFile())) {
                StoredConfig repoConfig = repo.getRepository().getConfig(); //get repo config
                result = repoConfig.getString("user", null, "email"); //recover the user email
                repo.close();
            } //close the repo
        } catch (IOException ex) { }
        return result;
    }

    
    public void setReposUrl(String url)
    {
        this.reposUrl = url;
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
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public TeamworkCommand checkout(File projectPath)
    {
        return new GitCloneCommand(this, projectPath);
    }

    @Override
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files, String commitComment)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public TeamworkCommand shareProject()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
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
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public boolean prepareDeleteDir(File dir)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void prepareCreateDir(File dir)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
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
        throw new UnsupportedOperationException("Not implemented yet.");
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
        UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(userName, password); // set a configuration with username and password.
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
}
