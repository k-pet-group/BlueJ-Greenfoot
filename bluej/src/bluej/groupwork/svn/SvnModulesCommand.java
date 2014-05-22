/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013,2014  Michael Kolling and John Rosenberg 
 
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

import java.util.List;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.DirEntry;
import org.tigris.subversion.javahl.ListCallback;
import org.tigris.subversion.javahl.Lock;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * A command to retrieve a list of modules from a subversion repository.
 * 
 * @author Davin McCall
 */
public class SvnModulesCommand extends SvnCommand
{
    private List<String> modulesList;
    
    public SvnModulesCommand(SvnRepository repository, List<String> modulesList)
    {
        super(repository);
        this.modulesList = modulesList;
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        
        try {
            if (! isCancelled()) {
                client.list(getRepository().getReposUrl(), Revision.HEAD, Revision.HEAD, Depth.immediates,
                        DirEntry.Fields.nodeKind, false,
                        new ListCallback() {
                      
                            @Override
                            public void doEntry(DirEntry entry, Lock arg1)
                            {
                                if (entry.getNodeKind() == NodeKind.dir) {
                                    modulesList.add(entry.getPath());
                                }
                            }
                });

                return new TeamworkCommandResult();
              }
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
            }
        }
        
        return new TeamworkCommandAborted();
    }
}
