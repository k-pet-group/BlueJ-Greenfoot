/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import java.util.List;
import java.util.Set;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.Status;

import bluej.groupwork.*;
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
    private String reposUrl;
    
    private SVNClientInterface client;
    private Object clientLock = new Object();
    
    public SvnRepository(File projectPath, String reposUrl, SVNClientInterface client)
    {
        this.projectPath = projectPath;
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

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#commitAll(java.util.Set, java.util.Set, java.util.Set, java.util.Set, java.lang.String)
     */
    public TeamworkCommand commitAll(Set newFiles, Set binaryNewFiles, Set deletedFiles, Set files, String commitComment)
    {
        return new SvnCommitAllCommand(this, newFiles, binaryNewFiles, deletedFiles,
                files, commitComment);
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#getAllLocallyDeletedFiles(java.util.Set)
     */
    public void getAllLocallyDeletedFiles(Set files)
    {
        synchronized (clientLock) {
            try {
                Status [] status = client.status(projectPath.getAbsolutePath(),
                        true, false, false);
                for (int i = 0; i < status.length; i++) {
                    File file = new File(status[i].getPath());
                    if (! file.exists()) {
                        files.add(file);
                    }
                }
            }
            catch (ClientException ce) {
                
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
     * (non-Javadoc)
     * @see bluej.groupwork.Repository#getModules(java.util.List)
     */
    public TeamworkCommand getModules(List modules)
    {
        return new SvnModulesCommand(this, modules);
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#getStatus(bluej.groupwork.StatusListener, java.util.Set, boolean)
     */
    public TeamworkCommand getStatus(StatusListener listener, FileFilter filter, boolean includeRemote)
    {
        return new SvnStatusCommand(this, listener, filter, includeRemote);
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#prepareCreateDir(java.io.File)
     */
    public void prepareCreateDir(File dir)
    {
        try {
            Status stat = client.singleStatus(dir.getAbsolutePath(), false);
            if (!stat.isManaged()) {
                client.add(dir.getAbsolutePath(), false);
            }
        }
        catch (ClientException ce) {
            Debug.message("Exception while doing svn add on directory: "
                    + ce.getLocalizedMessage());
        }
    }

    /* (non-Javadoc)
     * @see bluej.groupwork.Repository#prepareDeleteDir(java.io.File)
     */
    public boolean prepareDeleteDir(File dir)
    {
        synchronized (clientLock) {
            try {
                client.remove(new String[] {dir.getAbsolutePath()}, "", true);
            }
            catch (ClientException ce) {
                Debug.message("Exception while doing svn remove on directory: "
                        + ce.getLocalizedMessage());
            }
        }
        
        return false;
    }

    /* (non-Javadoc)
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
}
