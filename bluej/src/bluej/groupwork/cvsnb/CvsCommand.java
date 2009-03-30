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
package bluej.groupwork.cvsnb;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.*;

/**
 * Base class for Cvs commands. Provides functionality to allow cancelling CVS
 * operations, and converting CVS library errors (exceptions) to appropriate
 * teamwork result objects.
 * 
 * @author Davin McCall
 */
public abstract class CvsCommand
    implements TeamworkCommand
{
    protected CvsRepository repository;
    private BlueJCvsClient client;
    private boolean cancelled;
    
    protected CvsCommand(CvsRepository repository)
    {
        this.repository = repository;
        cancelled = false;
    }
    
    /**
     * Get a new client to be used for command processing.
     */
    protected synchronized BlueJCvsClient getClient()
        throws CommandAbortedException, AuthenticationException
    {
        if (cancelled) {
            throw new CommandAbortedException("","");
        }
        client = repository.getClient();
        return client;
    }
    
    public synchronized void cancel()
    {
        if (! cancelled) {
            cancelled = true;
            if (client != null) {
                client.abort();
            }
        }
    }
    
    public TeamworkCommandResult getResult()
    {
        try {
            BasicServerResponse response = doCommand();

            if (response.isError()) {
                return new TeamworkCommandError(response.getMessage(), null);
            }

            // command completed successfully
            return new TeamworkCommandResult();
        }
        catch (CommandAbortedException cae) {
            return new TeamworkCommandAborted();
        }
        catch (CommandException ce) {
            return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
        }
        catch (AuthenticationException ae) {
            return new TeamworkCommandAuthFailure();
        }
    }
    
    /**
     * Actually peform the command and return a response object.
     * 
     * @param client  A client, with connection established, for use in processing the
     *                command.
     * 
     * @throws CommandAbortedException
     * @throws CommandException
     * @throws AuthenticationException
     * @throws InvalidCvsRootException
     */
    protected abstract BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException;
}
