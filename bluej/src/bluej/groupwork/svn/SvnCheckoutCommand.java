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

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * Subversion "checkout" command.
 * 
 * @author Davin McCall
 */
public class SvnCheckoutCommand extends SvnCommand
{
    private File checkoutPath;
    private String moduleName;
    
    public SvnCheckoutCommand(SvnRepository repository, File projectPath)
    {
        super(repository);
        this.checkoutPath = projectPath.getAbsoluteFile();
        moduleName = projectPath.getName();
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        String reposUrl = getRepository().getReposUrl();
        reposUrl += "/" + moduleName;
        
        try {
            client.checkout(reposUrl, checkoutPath.getAbsolutePath(),
                Revision.HEAD, Revision.HEAD, Depth.infinity, true, true);
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
