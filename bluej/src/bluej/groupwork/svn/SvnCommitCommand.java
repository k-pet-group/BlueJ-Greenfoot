/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2013  Michael Kolling and John Rosenberg 
 
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
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;

import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.Debug;

/**
 * Implementation of Subversion commit command, handling forced commits.
 * 
 * @author Davin McCall
 */
public class SvnCommitCommand extends SvnCommitAllCommand
{
    private Set<File> forceFiles;
    private long revision;
    
    public SvnCommitCommand(SvnRepository repository, Set<File> newFiles,
            Set<File> binaryNewFiles, Set<File> deletedFiles, Set<File> files,
            Set<File> forceFiles, long revision, String commitComment)
    {
        super(repository, newFiles, binaryNewFiles, deletedFiles, files, commitComment);
        this.forceFiles = forceFiles;
        this.revision = revision;
    }
    
    @Override
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        
        // Subversion doesn't allow any sort of forced commit. So, we have to
        // make backups of the forced files (as long as they actually exist, i.e.
        // not a forced delete), then update to the correct revision, then restore
        // the backups.
        
        // Find the files to make backups of:
        Set<File> backupSet = new HashSet<File>();
        Set<File> deleteMeSet = new HashSet<File>();
        Map<File,File> bmap = null;
        for (Iterator<File> i = forceFiles.iterator(); i.hasNext(); ) {
            File file = i.next();
            if (file.exists() && ! deletedFiles.contains(file)) {
                backupSet.add(file);
            }
            else {
                deleteMeSet.add(file);
            }
        }
        try {
            bmap = TeamUtils.backupFiles(backupSet);
            
            // Perform update
            String [] paths = new String[forceFiles.size()];
            int j = 0;
            for (Iterator<File> i = forceFiles.iterator(); i.hasNext(); ) {
                File file = i.next();
                paths[j++] = file.getAbsolutePath();
                // Delete the file, so the update cannot conflict
                file.delete();
            }

            client.update(paths, Revision.getInstance(revision), Depth.infinity, false, false, false); 
            
            TeamworkCommandResult result = super.doCommand();
            return result; // finally clause below restores backups
        }
        catch (IOException ioe) {
            return new TeamworkCommandError(ioe.getMessage(), "File I/O error: " + ioe.getLocalizedMessage());
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
            }
            else {
                return new TeamworkCommandAborted();
            }
        }
        finally {
            if (bmap != null) {
                try {
                    TeamUtils.restoreBackups(bmap);
                    for (Iterator<File> i = deleteMeSet.iterator(); i.hasNext(); ) {
                        i.next().delete();
                    }
                }
                catch (IOException ioe) {
                    Debug.message("Failed to restore file after forced commit: " + ioe.getLocalizedMessage());
                }
            }
        }
    }
}
