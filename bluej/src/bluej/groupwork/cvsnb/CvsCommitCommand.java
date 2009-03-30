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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;

import bluej.groupwork.TeamStatusInfo;

/**
 * A CVS commit operation which supports "forced commit".
 * 
 * @author Davin McCall
 */
public class CvsCommitCommand extends CvsCommitAllCommand
{
    private Set<TeamStatusInfo> forceFiles;
    
    public CvsCommitCommand(CvsRepository repository, Set<File> newFiles,
            Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files,
            Set<TeamStatusInfo> forceFiles, String commitComment)
    {
        super(repository, newFiles, binaryNewFiles, deletedFiles, files, commitComment);
        this.forceFiles = forceFiles;
    }
    
    protected BasicServerResponse doCommand() throws CommandAbortedException,
            CommandException, AuthenticationException
    {
        // First "update" all forced files to current revisions
        for (Iterator<TeamStatusInfo> i = forceFiles.iterator(); i.hasNext(); ) {
            TeamStatusInfo info = i.next();
            String reposVer = info.getRepositoryVersion();
            if (reposVer != null && reposVer.length() != 0) {
                try {
                    repository.setFileVersion(info.getFile(), reposVer);
                }
                catch (IOException ioe) {
                    throw new CommandException(ioe, "Can't set file version");
                }
            }
        }
        
        return super.doCommand();
    }
}
