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

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.Debug;

/**
 * Base class for subversion command implementations.
 * 
 * @author Davin McCall
 */
abstract public class SvnCommand
    implements TeamworkCommand
{
    private SvnRepository repository;
    private SVNClientInterface client;
    private boolean cancelled = false;
    
    protected SvnCommand(SvnRepository repository)
    {
        this.repository = repository;
    }
    
    public synchronized void cancel()
    {
        cancelled = true;
        if (client != null) {
            try {
                client.cancelOperation();
            }
            catch (ClientException ce) {
                // Why would we get an exception on a cancel?
                Debug.message("Exception during subversion cancel:");
                ce.printStackTrace(System.out);
            }
        }
    }
    
    /**
     * Check whether this command has been cancelled.
     */
    protected synchronized boolean isCancelled()
    {
        return cancelled;
    }
    
    /**
     * Get a handle to the SVN client interface.
     */
    protected SVNClientInterface getClient()
    {
        return client;
    }
    
    /**
     * Get a handle to the repository.
     */
    protected SvnRepository getRepository()
    {
        return repository;
    }

    public TeamworkCommandResult getResult()
    {
        return repository.execCommand(this);
    }
    
    public TeamworkCommandResult doCommand(SVNClientInterface client)
    {
        synchronized (this) {
            if (cancelled) {
                return new TeamworkCommandAborted();
            }
            this.client = client;
        }
        
        TeamworkCommandResult result = doCommand();
        this.client = null; // so that cancellation after completion doesn't
                       // cause the next command to be cancelled
        return result;
    }
    
    abstract protected TeamworkCommandResult doCommand();
}
