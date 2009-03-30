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
import java.util.Iterator;
import java.util.Set;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusKind;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;

/**
 * A subversion command to commit files.
 * 
 * @author Davin McCall
 */
public class SvnCommitAllCommand extends SvnCommand
{
    protected Set newFiles;
    protected Set binaryNewFiles;
    protected Set deletedFiles;
    protected Set files;
    protected String commitComment;
    
    public SvnCommitAllCommand(SvnRepository repository, Set newFiles, Set binaryNewFiles,
            Set deletedFiles, Set files, String commitComment)
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
        
        try {
            // First "svn add" the new files
            Iterator i = newFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                
                Status status = client.singleStatus(newFile.getAbsolutePath(), false);
                if (! status.isManaged()) {
                    addDir(client, newFile.getParentFile(), files);
                    client.add(newFile.getAbsolutePath(), false);
                    if (! newFile.isDirectory()) {
                        client.propertySet(newFile.getAbsolutePath(), "svn:eol-style",
                                "native", false);
                    }
                }
            }
            
            // And binary files
            i = binaryNewFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                
                Status status = client.singleStatus(newFile.getAbsolutePath(), false);
                if (! status.isManaged()) {
                    addDir(client, newFile.getParentFile(), files);
                    client.add(newFile.getAbsolutePath(), false);
                    if (! newFile.isDirectory()) {
                        client.propertySet(newFile.getAbsolutePath(), "svn:mime-type",
                                "application/octet-stream", false);
                    }
                }
            }
            
            // "svn delete" removed files
            i = deletedFiles.iterator();
            while (i.hasNext()) {
                File newFile = (File) i.next();
                client.remove(new String[] {newFile.getAbsolutePath()}, "", true);
            }
            
            // now do the commit
            String [] commitFiles = new String[files.size()];
            i = files.iterator();
            for (int j = 0; j < commitFiles.length; j++) {
                File file = (File) i.next();
                commitFiles[j] = file.getAbsolutePath();
            }
            client.commit(commitFiles, commitComment, false);
            
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
    
    /**
     * Add ("svn add") a directory, if necessary
     * @throws ClientException
     */
    protected boolean addDir(SVNClientInterface client, File dir, Set commitFiles) throws ClientException
    {
        File projectPath = getRepository().getProjectPath().getAbsoluteFile();
        if (dir.getAbsoluteFile().equals(projectPath)) {
            return false;
        }
        
        Status status = client.singleStatus(dir.getAbsolutePath(), false);
        int istatus = status.getNodeKind();
        
        boolean doAdd = (istatus == NodeKind.none || istatus == NodeKind.unknown);
        boolean commit = (istatus == NodeKind.dir
                && status.getTextStatus() == StatusKind.added);
        
        if (doAdd || commit) {
            File parent = dir.getParentFile();
            boolean parentAdded = addDir(client, parent, commitFiles);
            if (doAdd) {
                client.add(dir.getAbsolutePath(), false);
            }
            commitFiles.add(dir);
            return true;
        }
        
        return false;
    }
}
