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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.status.StatusInformation;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.file.FileStatus;

import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.UpdateListener;

/**
 * A command to get status of files in a CVS repository.
 * 
 * @author Davin McCall
 */
public class CvsStatusCommand extends CvsCommand
{
    private StatusListener listener;
    private Set<File> files;
    private boolean includeRemote;
    
    public CvsStatusCommand(CvsRepository repository, StatusListener listener,
            Set<File> files, boolean includeRemote)
    {
        super(repository);
        this.listener = listener;
        this.files = files;
        this.includeRemote = includeRemote;
    }
    
    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        LinkedList<TeamStatusInfo> returnInfo = new LinkedList<TeamStatusInfo>();
        File projectPath = repository.getProjectPath();
        Set<File> remoteDirs;
        
        Client client = getClient();
        
        // First we need to figure out remote directories
        if (includeRemote) {
            remoteDirs = new HashSet<File>();
            List<File> remoteFiles = repository.getRemoteFiles(client, remoteDirs);
            files.addAll(remoteFiles);
        }
        else {
            remoteDirs = repository.getRemoteDirs(client);
        }
        client = null;
        
        // First, it's necessary to filter out files which are in
        // directories not in the repository. Otherwise the
        // CVS status command barfs when it hits such a file.
        for (Iterator<File> i = files.iterator(); i.hasNext(); ) {
            File file = i.next();
            File parent = file.getParentFile();
            if (! remoteDirs.contains(parent) && ! repository.isDirectoryUnderCVS(parent)) {
                i.remove();
                // All such files have status NEEDSADD.
                TeamStatusInfo teamInfo = new TeamStatusInfo(file,
                        "",
                        null,
                        TeamStatusInfo.STATUS_NEEDSADD);
                returnInfo.add(teamInfo);
            }
        }
        
        StatusServerResponse statusServerResponse =
            repository.getStatus(getClient(), files, remoteDirs);
        
        List<StatusInformation> statusInfo = statusServerResponse.getStatusInformation();
        for (Iterator<StatusInformation> i = statusInfo.iterator(); i.hasNext(); ) {
            StatusInformation sinfo = i.next();
            int status;
            boolean deletedInRepos = false;
            
            FileStatus fstatus = sinfo.getStatus();
            String workingRev = sinfo.getWorkingRevision();
            if (workingRev == null || workingRev.startsWith("No entry")) {
                workingRev = "";
            }
            
            // There's a bug in the netbeans CVS library which can cause files
            // with the same base name (eg. multiple "bluej.pkg" files) to sometimes
            // get mixed up. However the repository file name will always
            // be correct, so we'll use that instead.
            File file;
            String reposName = sinfo.getRepositoryFileName();
            if (reposName != null) {
                if (reposName.endsWith(",v")) {
                    reposName = reposName.substring(0, reposName.length() - 2);
                }
                String reposRoot = repository.getRepositoryRoot();
                if (! reposRoot.endsWith("/")) {
                    reposRoot += "/";
                }
                reposRoot += projectPath.getName() + "/";
                String fname = reposName.substring(reposRoot.length());
                
                file = new File(projectPath, fname);

                // Files are in "Attic" if they were deleted in the repository
                File parentDir = file.getParentFile();
                if (parentDir.getName().equals("Attic")) {
                    file = new File(parentDir.getParentFile(), file.getName());
                    deletedInRepos = true;
                }
            }
            else {
                // Of course, for files not in the repository, no repository
                // version is available.
                file = sinfo.getFile();
            }
            
            if (fstatus == FileStatus.NEEDS_CHECKOUT) {
                // For deleted files, CVS returns NEEDS_CHECKOUT because
                // we haven't executed "cvs remove" yet.
                if (workingRev.length() > 0) {
                    String reposRev = sinfo.getRepositoryRevision();
                    if (workingRev.equals(reposRev)) {
                        status = TeamStatusInfo.STATUS_DELETED;    

                    }
                    else {
                        // Not up-to-date, but locally deleted
                        status = TeamStatusInfo.STATUS_CONFLICT_LDRM;
                    }
                }
                else {
                    status = TeamStatusInfo.STATUS_NEEDSCHECKOUT;
                }
            }
            else if (fstatus == FileStatus.NEEDS_PATCH) {
                status = TeamStatusInfo.STATUS_NEEDSUPDATE;
            }
            else if (fstatus == FileStatus.NEEDS_MERGE) {
                status = TeamStatusInfo.STATUS_NEEDSMERGE;
            }
            else if (fstatus == FileStatus.MODIFIED || fstatus == FileStatus.ADDED) {
                // We only get status "ADDED" if a commit was cancelled
                // (after the "cvs add", but before "cvs commit"). It's easiest
                // in that case to pretend that the file is actually in the
                // repository (otherwise we'd need to special case the commit handling,
                // to prevent attempting to "cvs add" a file which had already been
                // added)
                status = TeamStatusInfo.STATUS_NEEDSCOMMIT;
            }
            else if (fstatus == FileStatus.UNKNOWN) {
                // present locally, not present in repository
                status = TeamStatusInfo.STATUS_NEEDSADD;
            }
            else if (fstatus == FileStatus.UP_TO_DATE) {
                status = TeamStatusInfo.STATUS_UPTODATE;
            }
            else if (fstatus == FileStatus.INVALID) {
                status = TeamStatusInfo.STATUS_REMOVED;
            }
            else if (fstatus == FileStatus.UNRESOLVED_CONFLICT) {
                if (deletedInRepos) {
                    // There's been a local modification, but the file has been
                    // removed in the repository
                    status = TeamStatusInfo.STATUS_CONFLICT_LMRD;
                }
                else {
                    if (workingRev.length() == 0) {
                        // File has been added locally. This can only be a conflict
                        // if the file has also been added in the repository.
                        status = TeamStatusInfo.STATUS_CONFLICT_ADD;
                    }
                    else {
                        status = TeamStatusInfo.STATUS_UNRESOLVED;
                    }
                }
            }
            else if (fstatus == FileStatus.HAS_CONFLICTS) {
                // The local file still has conflicts in it from the last update.
                // The file needs to modified before this status will change.
                status = TeamStatusInfo.STATUS_HASCONFLICTS;
            }
            else if (fstatus == FileStatus.REMOVED) {
                // "cvs remove" command has been run for this file. This
                // shouldn't really happen, because we only do that just
                // before a commit.
                status = TeamStatusInfo.STATUS_NEEDSCOMMIT;
            }
            else {
                status = TeamStatusInfo.STATUS_WEIRD;
            }
            
            
            if (files.remove(file)) {
                TeamStatusInfo teamInfo = new TeamStatusInfo(file,
                        workingRev,
                        sinfo.getRepositoryRevision(),
                        status);
                returnInfo.add(teamInfo);
            }
        }
        
        // Now we may have some local files left which cvs hasn't given any
        // status for...
        for (Iterator<File> i = files.iterator(); i.hasNext(); ) {
            File file = i.next();
            TeamStatusInfo teamInfo = new TeamStatusInfo(file,
                    "",
                    null,
                    TeamStatusInfo.STATUS_NEEDSADD);
            returnInfo.add(teamInfo);
        }

        if (listener != null) {
            while (! returnInfo.isEmpty()) {
                TeamStatusInfo teamInfo = (TeamStatusInfo) returnInfo.removeFirst();
                listener.gotStatus(teamInfo);
            }
            
            listener.statusComplete(new CvsStatusHandle(repository));
        }
        
        return statusServerResponse;
    }
    
    /* (non-Javadoc)
     * @see bluej.groupwork.cvsnb.CvsCommand#getUpdateTo(bluej.groupwork.UpdateListener, java.util.Set, java.util.Set)
     */
    public TeamworkCommand getUpdateTo(UpdateListener listener, Set<File> files, Set<File> forceFiles)
    {
        return new CvsUpdateCommand(repository, listener, files, forceFiles);
    }
}
