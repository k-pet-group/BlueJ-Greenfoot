/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.svn;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusCallback;

import bluej.groupwork.LogHistoryListener;
import bluej.groupwork.Repository;
import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamSettings;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.Debug;

/**
 * A subversion repository implementation.
 * 
 * @author Davin McCall
 */
public class SvnRepository
    implements Repository
{
    private File projectPath;
    private String protocol; // Only for data collection
    private String reposUrl;
    
    private SVNClientInterface client;
    private Object clientLock = new Object();
    
    public SvnRepository(File projectPath, String protocol, String reposUrl, SVNClientInterface client)
    {
        this.projectPath = projectPath;
        this.protocol = protocol;
        this.reposUrl = reposUrl;
        this.client = client;
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#versionsDirectories()
     */
    public boolean versionsDirectories()
    {
        return true;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#setPassword(bluej.groupwork.TeamSettings)
     */
    public void setPassword(TeamSettings newSettings)
    {
        client.password(newSettings.getPassword());
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#checkout(java.io.File)
     */
    public TeamworkCommand checkout(File projectPath)
    {
        return new SvnCheckoutCommand(this, projectPath);
    }

    /*
     * @see bluej.groupwork.Repository#commitAll(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.lang.String)
     */
    public TeamworkCommand commitAll(Set<File> newFiles, Set<File> binaryNewFiles,
            Set<File> deletedFiles, Set<File> files, String commitComment)
    {
        return new SvnCommitAllCommand(this, newFiles, binaryNewFiles, deletedFiles,
                files, commitComment);
    }

    /*
     * @see bluej.groupwork.Repository#getAllLocallyDeletedFiles(java.util.Set)
     */
    public void getAllLocallyDeletedFiles(final Set<File> files)
    {
        synchronized (clientLock) {
            try {
                client.status(projectPath.getAbsolutePath(), Depth.infinity,
                        false, false, false, false, null,
                        new StatusCallback() {
                    public void doStatus(Status status)
                    {
                        File file = new File(status.getPath());
                        if (! file.exists()) {
                            files.add(file);
                        }
                    }
                });
            }
            catch (ClientException ce) {
                Debug.reportError("Subversion: ClientException when getting local status", ce);
            }
        }
    }

    public TeamworkCommand getLogHistory(LogHistoryListener listener)
    {
        return new SvnHistoryCommand(this, listener);
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#getMetadataFilter()
     */
    public FileFilter getMetadataFilter()
    {
        return new FileFilter() {
            public boolean accept(File pathname)
            {
                return ! pathname.getName().equals(".svn"); 
            }
        };
    }

    /*
     * @see bluej.groupwork.Repository#getModules(java.util.List)
     */
    public TeamworkCommand getModules(List<String> modules)
    {
        return new SvnModulesCommand(this, modules);
    }

    /*
     * @see bluej.groupwork.Repository#getStatus(bluej.groupwork.StatusListener, java.util.Set, boolean)
     */
    public TeamworkCommand getStatus(StatusListener listener, FileFilter filter, boolean includeRemote)
    {
        return new SvnStatusCommand(this, listener, filter, includeRemote);
    }

    /*
     * @see bluej.groupwork.Repository#prepareCreateDir(java.io.File)
     */
    public void prepareCreateDir(final File dir)
    {
        try {
            client.status(dir.getAbsolutePath(), Depth.empty, false, true, true, false, null,
                    new StatusCallback() {
                public void doStatus(Status stat)
                {
                    if (! stat.isManaged()) {
                        try {
                            client.add(dir.getAbsolutePath(), Depth.empty, true, false, true);
                        }
                        catch (ClientException ce) {
                            Debug.message("Exception while doing svn add on directory: "
                                    + ce.getLocalizedMessage());
                        }
                    }
                }
            });
        }
        catch (ClientException ce) {
            Debug.message("Exception while doing svn status on new directory: "
                    + ce.getLocalizedMessage());
        }
    }

    /*
     * @see bluej.groupwork.Repository#prepareDeleteDir(java.io.File)
     */
    public boolean prepareDeleteDir(File dir)
    {
        synchronized (clientLock) {
            try {
                client.remove(new String[] {dir.getAbsolutePath()}, "", true, true, Collections.emptyMap());
            }
            catch (ClientException ce) {
                Debug.message("Exception while doing svn remove on directory: "
                        + ce.getLocalizedMessage());
            }
        }
        
        return false;
    }

    /*
     * @see bluej.groupwork.Repository#shareProject()
     */
    public TeamworkCommand shareProject()
    {
        return new SvnShareCommand(this);
    }

    /**
     * Execute a subversion command with the client lock held. This is called by
     * subversion commands internally, to acquire the client lock.
     */
    public TeamworkCommandResult execCommand(SvnCommand command)
    {
        synchronized (clientLock) {
            return command.doCommand(client);
        }
    }
    
    /**
     * Get the subversion URL for this project
     */
    public String getReposUrl()
    {
        return reposUrl;
    }
    
    /**
     * Get the path of the working copy
     */
    public File getProjectPath()
    {
        return projectPath;
    }
    
    /**
     * Get the client interface directly.
     */
    public SVNClientInterface getClient()
    {
        return client;
    }

    @Override
    public String getVCSType()
    {
        return "SVN";
    }

    @Override
    public String getVCSProtocol()
    {
        return protocol;
    }
}
