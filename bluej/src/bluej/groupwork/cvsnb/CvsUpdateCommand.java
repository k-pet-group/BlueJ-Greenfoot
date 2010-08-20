/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import java.io.File;
import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.UpdateListener;

/**
 * Command to perform an update of a specified set of files
 * 
 * @author Davin McCall
 */
public class CvsUpdateCommand extends CvsCommand
{
    private UpdateListener listener; // may be null
    private Set<File> theFiles;
    private Set<File> forceFiles;
    
    public CvsUpdateCommand(CvsRepository repository, UpdateListener listener,
            Set<File> theFiles, Set<File> forceFiles)
    {
        super(repository);
        this.listener = listener;
        this.theFiles = theFiles;
        this.forceFiles = forceFiles;
    }

    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        UpdateServerResponse response = null;
        
        if (! theFiles.isEmpty()) {
            BlueJCvsClient client = getClient();
            response = repository.doUpdateFiles(client, listener, theFiles, false);
            if (listener != null) {
                listener.handleConflicts(response);
            }
            
            if (response.isError()) {
                return response;
            }
        }
        
        if (! forceFiles.isEmpty()) {
            BlueJCvsClient client = getClient();
            response = repository.doUpdateFiles(client, listener, forceFiles, true);
        }
        
        return response;
    }
}
