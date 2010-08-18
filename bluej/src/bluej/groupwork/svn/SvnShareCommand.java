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
package bluej.groupwork.svn;

import java.io.File;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.Config;
import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * Subversion command to share a project
 * 
 * @author Davin McCall
 */
public class SvnShareCommand extends SvnCommand
{
    public SvnShareCommand(SvnRepository repository)
    {
        super(repository);
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        File projectPath = getRepository().getProjectPath();
        String projUrl = getRepository().getReposUrl() + "/" + projectPath.getName();
        
        try {
            client.mkdir(new String[] {projUrl},
                    Config.getString("team.import.initialMessage"));
            
            client.checkout(projUrl, projectPath.getAbsolutePath(), Revision.HEAD, Revision.HEAD,
                    Depth.empty, false, true);
            
            if (! isCancelled()) {
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
