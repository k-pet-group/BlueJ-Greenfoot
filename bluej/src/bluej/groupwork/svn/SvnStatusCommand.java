/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2016,2017  Michael Kolling and John Rosenberg
 
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
import java.io.FileFilter;
import java.util.*;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.StatusCallback;
import org.tigris.subversion.javahl.StatusKind;

import bluej.groupwork.*;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.utility.Debug;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Subversion "status" command.
 * 
 * @author Davin McCall
 */
public class SvnStatusCommand extends SvnCommand
{
    private StatusListener listener;
    private FileFilter filter;
    private long currentRevision = -1;
    
    public SvnStatusCommand(SvnRepository repository, StatusListener listener,
            FileFilter filter, boolean includeRemote)
    {
        super(repository);
        this.listener = listener;
        this.filter = filter;
    }

    @OnThread(Tag.Worker)
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        File projectPath = getRepository().getProjectPath().getAbsoluteFile();
        
        try {
            final List<org.tigris.subversion.javahl.Status> statusList = new LinkedList<>();
            client.status(projectPath.getAbsolutePath(), Depth.infinity, true,
                    true, false, false, new String[0], new StatusCallback() {
                        public void doStatus(org.tigris.subversion.javahl.Status arg0)
                        {
                            statusList.add(arg0);
                        }
                    });

            org.tigris.subversion.javahl.Status [] status = statusList.toArray(
                    new org.tigris.subversion.javahl.Status[statusList.size()]);
            
            /*
             * Subversion is a bit stupid. If a directory has been removed from
             * the repository, status of files within still shows as "up to date".
             * We'll fix that. We need to cache status entries until we get the
             * status for the parent directory.
             */
            
            // The set of directories for which we have status
            Set<File> completed = new HashSet<File>();
            // A map (File->Set<TeamStatusInfo>) from directories for which
            // we don't yet have status, to the status of files within
            Map<File,Set<TeamStatusInfo>> unreported = new HashMap<File,Set<TeamStatusInfo>>();
            
            for (int i = 0; i < status.length; i++) {
                File file = new File(status[i].getPath());
                
                int textStat = status[i].getTextStatus();
                
                // All I've seen so far is "added", "deleted", "non-svn" (none).
                int reposStat = status[i].getRepositoryTextStatus();
                
                // the repository revision doesn't seem to be valid for
                // status "normal", or "repository: deleted".
                // it is valid for "repository: added".
                long reposRev = status[i].getReposLastCmtRevisionNumber();
                if (reposRev > currentRevision) {
                    currentRevision = reposRev;
                }
                
                TeamStatusInfo rinfo = null;
                
                if (textStat == StatusKind.missing
                        || textStat == StatusKind.deleted) {
                    String rev = "" + status[i].getLastChangedRevisionNumber();
                    if (reposStat == StatusKind.modified) {
                        rinfo = new TeamStatusInfo(file, rev, "" + reposRev, Status.CONFLICT_LDRM);
                    }
                    else {
                        rinfo = new TeamStatusInfo(file, rev, "", Status.DELETED);
                    }
                }
                else if ((textStat == StatusKind.unversioned)
                        || (textStat == StatusKind.none && reposStat == StatusKind.none)) {
                    // Bug in SVNKit 1.8.11 returns a local and remote status of "none" for unversioned local
                    // files which do not exist in the repository.
                    // https://issues.tmatesoft.com/issue/SVNKIT-643
                    if (filter.accept(file)) {
                        if (reposStat != StatusKind.added) {
                            rinfo = new TeamStatusInfo(file, "", "", Status.NEEDS_ADD);
                            if (file.isDirectory()) {
                                statLocalDir(file);
                            }
                        }
                        else {
                            // conflict: added locally and in repository
                            rinfo = new TeamStatusInfo(file, "", "" + status[i].getReposLastCmtRevisionNumber(), Status.CONFLICT_ADD);
                        }
                    }
                }
                else if (textStat == StatusKind.normal) {
                    if (reposStat == StatusKind.none && status[i].getRevisionNumber() == -1) {
                        // Bug in SVNKit
                        rinfo = new TeamStatusInfo(file, "", "", Status.NEEDS_COMMIT);
                    }
                    else if (filter.accept(file)) {
                        String rev = "" + status[i].getLastChangedRevisionNumber();
                        if (reposStat == StatusKind.deleted) {
                            rinfo = new TeamStatusInfo(file, rev, "", Status.REMOVED);
                        }
                        else if (reposStat == StatusKind.none && !file.exists()) {
                            //Bug in SVNKit
                            //File status in the repository is normal,
                            //but the file status is none and the file
                            //doesn't exists locally anymore.
                            rinfo = new TeamStatusInfo(file, rev, "", Status.DELETED);
                        }
                        else if (reposStat == StatusKind.modified) {
                            rinfo = new TeamStatusInfo(file, rev,
                                    "" + status[i].getReposLastCmtRevisionNumber(),
                                    Status.NEEDS_UPDATE);
                        }
                        else {
                            rinfo = new TeamStatusInfo(file, rev, rev, Status.UP_TO_DATE);
                        }
                    }
                }
                else if (textStat == StatusKind.modified) {
                    if (filter.accept(file)) {
                        String rev = "" + status[i].getLastChangedRevisionNumber();
                        if (reposStat == StatusKind.deleted) {
                            rinfo = new TeamStatusInfo(file, rev, "", Status.CONFLICT_LMRD);
                        }
                        else if (reposStat == StatusKind.modified) {
                            rinfo = new TeamStatusInfo(file, rev, "", Status.NEEDS_MERGE);
                        }
                        else {
                            rinfo = new TeamStatusInfo(file, rev, rev, Status.NEEDS_COMMIT);
                        }
                    }
                }
                else if (textStat == StatusKind.none) {
                    if (reposStat == StatusKind.added) {
                        rinfo = new TeamStatusInfo(file, "", "" + reposRev, Status.NEEDS_CHECKOUT);
                    }
                }
                else if (textStat == StatusKind.added) {
                    // shouldn't normally happen unless something went wrong
                    // or someone has done "svn add" from command line etc.
                    rinfo = new TeamStatusInfo(file, "", "", Status.NEEDS_COMMIT);
                }

//                if (filter.accept(file) || ! file.exists()) {
//                    System.out.println("Status for: " + status[i].getPath());
//                    System.out.println("   Revision: " + status[i].getRevisionNumber());
//                    System.out.println("   lcRev: " + status[i].getLastChangedRevisionNumber());
//                    System.out.println("   statusDesc: " + status[i].getTextStatusDescription());
//
//                    System.out.println("   repostStat: " + Status.Kind.getDescription(reposStat));
//                    System.out.println("   reposRev: " + status[i].getReposLastCmtRevisionNumber());
//                    System.out.println("   hasRemote: " + status[i].hasRemote());
//
//                    System.out.println("   conflictNew: " + status[i].getConflictNew());
//                    System.out.println("   conflictOld: " + status[i].getConflictOld());
//                    System.out.println("   conflictWorking: " + status[i].getConflictWorking());
//                }

                if (rinfo != null) {
                    if (! file.exists()) {
                        listener.gotStatus(rinfo);
                    }
                    else if (completed.contains(file.getParentFile())
                            || file.equals(projectPath)) {
                        complete(completed, unreported, rinfo);
                    }
                    else {
                        // The status of the parent directory hasn't been reported
                        // yet. If the parent has been removed, the status we have
                        // now is incorrect; we need to cache the result into the
                        // parent status is reported.
                        Set<TeamStatusInfo> s = unreported.get(file.getParentFile());
                        if (s == null) {
                            s = new HashSet<TeamStatusInfo>();
                        }
                        s.add(rinfo);
                        unreported.put(file.getParentFile(), s);
                    }
                }
            }

            listener.statusComplete(new SvnStatusHandle(getRepository(), currentRevision));
            return new TeamworkCommandResult();
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                Debug.reportError("Subversion status command exception", ce);
                return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
            }
        }

        return new TeamworkCommandAborted();
    }

    /**
     * Provide status information for files in a directory which is
     * unversioned (and therefore ignored) according to subversion
     */
    @OnThread(Tag.Worker)
    private void statLocalDir(File dir)
    {
        File [] subFiles = dir.listFiles(filter);
        for (int i = 0; i < subFiles.length; i++) {
            listener.gotStatus(new TeamStatusInfo(subFiles[i], "", "",
                    Status.NEEDS_ADD));
            if (subFiles[i].isDirectory()) {
                statLocalDir(subFiles[i]);
            }
        }
    }

    @OnThread(Tag.Worker)
    private void complete(Set<File> completed, Map<File,Set<TeamStatusInfo>> unreported,
            TeamStatusInfo rinfo)
    {
        listener.gotStatus(rinfo);

        Status rinfoStat = rinfo.getStatus();

        File file = rinfo.getFile();
        if (file.isDirectory()) {
            completed.add(file);

            Set<TeamStatusInfo> entries = unreported.remove(file);
            if (entries == null) {
                entries = Collections.emptySet();
            }
            for (Iterator<TeamStatusInfo> i = entries.iterator(); i.hasNext(); ) {
                TeamStatusInfo status = i.next();
                Status einfoStat = status.getStatus();
                if (rinfoStat == Status.CONFLICT_LMRD
                        || rinfoStat == Status.REMOVED) {

                    // Parent was removed. We must change the child status to
                    // be removed also. One case we don't have to worry about
                    // is if the child has been locally deleted.
                    if (einfoStat != Status.DELETED
                            && einfoStat != Status.NEEDS_CHECKOUT) {
                        // can these happen?
                        //if (einfoStat == Status.CONFLICT_LDRM)
                        //if (einfoStat == Status.CONFLICT_LMRD)
                        if (einfoStat == Status.NEEDS_ADD) {
                            // what status to give here?
                            einfoStat = Status.CONFLICT_LMRD;
                        }
                        else if (einfoStat == Status.NEEDS_COMMIT) {
                            einfoStat = Status.CONFLICT_LMRD;
                        }
                        else if (einfoStat == Status.NEEDS_MERGE) {
                            einfoStat = Status.CONFLICT_LMRD;
                        }
                        else if (einfoStat == Status.NEEDS_UPDATE) {
                            einfoStat = Status.REMOVED;
                        }
                        else if (einfoStat == Status.UP_TO_DATE) {
                            einfoStat = Status.REMOVED;
                        }

                        complete(completed, unreported, new TeamStatusInfo(
                                status.getFile(), status.getLocalVersion(),
                                status.getRepositoryVersion(), einfoStat));
                    }
                }
                else {
                    // parent not deleted: report normally
                    complete(completed, unreported, status);
                }
            }

        }
    }
}
