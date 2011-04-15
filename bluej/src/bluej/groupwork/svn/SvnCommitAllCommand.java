/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011  Michael Kolling and John Rosenberg 
 
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.PropertyData;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusCallback;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.Debug;

/**
 * A subversion command to commit files.
 * 
 * @author Davin McCall
 */
public class SvnCommitAllCommand extends SvnCommand
{
    protected Set<File> newFiles;
    protected Set<File> binaryNewFiles;
    protected Set<File> deletedFiles;
    protected Set<File> files;
    protected String commitComment;
    
    public SvnCommitAllCommand(SvnRepository repository, Set<File> newFiles, Set<File> binaryNewFiles,
            Set<File> deletedFiles, Set<File> files, String commitComment)
    {
        super(repository);
        this.newFiles = newFiles;
        this.binaryNewFiles = binaryNewFiles;
        this.deletedFiles = deletedFiles;
        this.files = files;
        this.commitComment = commitComment;
    }

    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        
        // A class to allow callbacks to pass back status information.
        class StatusRef {
            Status status;
        };

        try {
            // First "svn add" the new files
            Iterator<File> i = newFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                
                final StatusRef statusRef = new StatusRef();
                
                client.status(newFile.getAbsolutePath(), Depth.empty, false, true, true, false, null,
                        new StatusCallback() {
                            @Override
                            public void doStatus(Status status)
                            {
                                statusRef.status = status;
                            }
                        });
                
                
                Status status = statusRef.status;
                if (! status.isManaged()) {
                    client.add(newFile.getAbsolutePath(), Depth.empty, false, false, true);
                    if (! newFile.isDirectory()) {
                        client.propertySet(newFile.getAbsolutePath(), PropertyData.EOL_STYLE,
                                "native", Depth.empty, null, false, null);
                    }
                }
            }
            
            // And binary files
            i = binaryNewFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                
                final StatusRef statusRef = new StatusRef();
                client.status(newFile.getAbsolutePath(), Depth.empty, false, true, true, false, null,
                        new StatusCallback() {
                            @Override
                            public void doStatus(Status status)
                            {
                                statusRef.status = status;
                            }
                        });
                Status status = statusRef.status;
                if (! status.isManaged()) {
                    client.add(newFile.getAbsolutePath(), Depth.empty, false, false, true);
                    if (! newFile.isDirectory()) {
                        client.propertySet(newFile.getAbsolutePath(), PropertyData.MIME_TYPE,
                                "application/octet-stream", Depth.empty, null, false, null);
                    }
                }
            }
            
            // "svn delete" removed files
            i = deletedFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                client.remove(new String[] {newFile.getAbsolutePath()}, "", true, false, Collections.emptyMap());
            }
            
            // now do the commit
            String [] commitFiles = new String[files.size()];
            i = files.iterator();
            for (int j = 0; j < commitFiles.length; j++) {
                File file = (File) i.next();
                commitFiles[j] = file.getAbsolutePath();
            }
            client.commit(commitFiles, commitComment, Depth.empty, false, false, null, Collections.emptyMap());
            
            if (! isCancelled()) {
                return new TeamworkCommandResult();
            }
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                Debug.reportError("Subversion commit all exception", ce);
                return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
            }
        }

        return new TeamworkCommandAborted();
    }
}
