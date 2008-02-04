package bluej.groupwork.svn;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusKind;

import bluej.groupwork.*;

/**
 * Subversion "status" command.
 * 
 * @author Davin McCall
 */
public class SvnStatusCommand extends SvnCommand
{
    private StatusListener listener;
    private FileFilter filter;
    private boolean includeRemote;
    private long currentRevision = -1;
    
    public SvnStatusCommand(SvnRepository repository, StatusListener listener,
            FileFilter filter, boolean includeRemote)
    {
        super(repository);
        this.listener = listener;
        this.filter = filter;
        this.includeRemote = includeRemote;
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();
        File projectPath = getRepository().getProjectPath().getAbsoluteFile();
        
        try {
            Status [] status = client.status(projectPath.getAbsolutePath(),
                    true, true, true);
            
            /*
             * Subversion is a bit stupid. If a directory has been removed from
             * the repository, status of files within still shows as "up to date".
             * We'll fix that. We need to cache status entries until we get the
             * status for the parent directory.
             */
            
            // The set of directories for which we have status
            Set completed = new HashSet();
            // A map (File->Set<TeamStatusInfo>) from directories for which
            // we don't yet have status, to the status of files within
            Map unreported = new HashMap();
            
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
                    rinfo = new TeamStatusInfo(file, "" + status[i].getRevisionNumber(), "", TeamStatusInfo.STATUS_DELETED);
                }
                else if (textStat == StatusKind.unversioned) {
                    if (filter.accept(file)) {
                        rinfo = new TeamStatusInfo(file, "", "", TeamStatusInfo.STATUS_NEEDSADD);
                        if (file.isDirectory()) {
                            statLocalDir(file);
                        }
                    }
                }
                else if (textStat == StatusKind.normal) {
                    if (filter.accept(file)) {
                        String rev = "" + status[i].getRevisionNumber();
                        if (reposStat == StatusKind.deleted) {
                            rinfo = new TeamStatusInfo(file, rev, "", TeamStatusInfo.STATUS_REMOVED);
                        }
                        else if (reposStat == StatusKind.modified) {
                            rinfo = new TeamStatusInfo(file, rev,
                                    "" + status[i].getReposLastCmtRevisionNumber(),
                                    TeamStatusInfo.STATUS_NEEDSUPDATE);
                        }
                        else {
                            rinfo = new TeamStatusInfo(file, rev, rev, TeamStatusInfo.STATUS_UPTODATE);
                        }
                    }
                }
                else if (textStat == StatusKind.modified) {
                    if (filter.accept(file)) {
                        String rev = "" + status[i].getRevisionNumber();
                        if (reposStat == StatusKind.deleted) {
                            rinfo = new TeamStatusInfo(file, rev, "", TeamStatusInfo.STATUS_CONFLICT_LMRD);
                        }
                        else if (reposStat == StatusKind.modified) {
                            rinfo = new TeamStatusInfo(file, rev, "", TeamStatusInfo.STATUS_NEEDSMERGE);
                        }
                        else {
                            rinfo = new TeamStatusInfo(file, rev, rev, TeamStatusInfo.STATUS_NEEDSCOMMIT);
                        }
                    }
                }
                else if (textStat == StatusKind.none) {
                    if (reposStat == StatusKind.added) {
                        rinfo = new TeamStatusInfo(file, "", "" + reposRev, TeamStatusInfo.STATUS_NEEDSCHECKOUT);
                    }
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
                        Set s = (Set) unreported.get(file.getParentFile());
                        if (s == null) {
                            s = new HashSet();
                        }
                        s.add(rinfo);
                        unreported.put(file.getParentFile(), s);
                    }
                }
            }
            
            return new TeamworkCommandResult();
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                return new TeamworkCommandError(ce.getLocalizedMessage());
            }
        }

        return new TeamworkCommandAborted();
    }
    
    /**
     * Provide status information for files in a directory which is
     * unversioned (and therefore ignored) according to subversion
     */
    private void statLocalDir(File dir)
    {
        File [] subFiles = dir.listFiles(filter);
        for (int i = 0; i < subFiles.length; i++) {
            listener.gotStatus(new TeamStatusInfo(subFiles[i], "", "",
                    TeamStatusInfo.STATUS_NEEDSADD));
            if (subFiles[i].isDirectory()) {
                statLocalDir(subFiles[i]);
            }
        }
    }
    
    private void complete(Set completed, Map unreported, TeamStatusInfo rinfo)
    {
        listener.gotStatus(rinfo);

        int rinfoStat = rinfo.getStatus();

        File file = rinfo.getFile();
        if (file.isDirectory()) {
            completed.add(file);

            Set entries = (Set) unreported.remove(file);
            if (entries == null) {
                entries = Collections.EMPTY_SET;
            }
            for (Iterator i = entries.iterator(); i.hasNext(); ) {
                TeamStatusInfo status = (TeamStatusInfo) i.next();
                int einfoStat = status.getStatus();
                if (rinfoStat == TeamStatusInfo.STATUS_CONFLICT_LMRD
                        || rinfoStat == TeamStatusInfo.STATUS_REMOVED) {

                    // Parent was removed. We must change the child status to
                    // be removed also. One case we don't have to worry about
                    // is if the child has been locally deleted.
                    if (einfoStat != TeamStatusInfo.STATUS_DELETED
                            && einfoStat != TeamStatusInfo.STATUS_NEEDSCHECKOUT) {
                        // can these happen?
                        //if (einfoStat == TeamStatusInfo.STATUS_CONFLICT_LDRM)
                        //if (einfoStat == TeamStatusInfo.STATUS_CONFLICT_LMRD)
                        if (einfoStat == TeamStatusInfo.STATUS_NEEDSADD) {
                            // what status to give here?
                            einfoStat = TeamStatusInfo.STATUS_CONFLICT_LMRD;
                        }
                        else if (einfoStat == TeamStatusInfo.STATUS_NEEDSCOMMIT) {
                            einfoStat = TeamStatusInfo.STATUS_CONFLICT_LMRD;
                        }
                        else if (einfoStat == TeamStatusInfo.STATUS_NEEDSMERGE) {
                            einfoStat = TeamStatusInfo.STATUS_CONFLICT_LMRD;
                        }
                        else if (einfoStat == TeamStatusInfo.STATUS_NEEDSUPDATE) {
                            einfoStat = TeamStatusInfo.STATUS_REMOVED;
                        }
                        else if (einfoStat == TeamStatusInfo.STATUS_UPTODATE) {
                            einfoStat = TeamStatusInfo.STATUS_REMOVED;
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
    
    /*
     * @see bluej.groupwork.svn.SvnCommand#getUpdateTo()
     */
    public TeamworkCommand getUpdateTo(UpdateListener listener, Set files, Set forceFiles)
    {
        return new SvnUpdateToCommand(getRepository(), listener,
                currentRevision, files, forceFiles);
    }
}
