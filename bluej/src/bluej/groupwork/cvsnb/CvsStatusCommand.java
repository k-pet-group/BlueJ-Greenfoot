package bluej.groupwork.cvsnb;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.AdminHandler;
import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.status.StatusInformation;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.file.FileStatus;

import bluej.groupwork.StatusListener;
import bluej.groupwork.TeamStatusInfo;

/**
 * A command to get status of files in a CVS repository.
 * 
 * @author Davin McCall
 */
public class CvsStatusCommand extends CvsCommand
{
    private StatusListener listener;
    private Set files;
    private boolean includeRemote;
    
    public CvsStatusCommand(CvsRepository repository, StatusListener listener, Set files, boolean includeRemote)
    {
        super(repository);
        this.listener = listener;
        this.files = files;
        this.includeRemote = includeRemote;
    }
    
    protected BasicServerResponse doCommand()
        throws CommandAbortedException, CommandException, AuthenticationException
    {
        LinkedList returnInfo = new LinkedList();
        File projectPath = repository.getProjectPath();
        Set remoteDirs;
        
        Client client = getClient();
        AdminHandler adminHandler = client.getAdminHandler();
        
        // First we need to figure out remote directories
        if (includeRemote) {
            remoteDirs = new HashSet();
            List remoteFiles = repository.getRemoteFiles(client, remoteDirs);
            files.addAll(remoteFiles);
        }
        else {
            remoteDirs = repository.getRemoteDirs(client);
        }
        client = null;
        
        // First, it's necessary to filter out files which are in
        // directories not in the repository. Otherwise the
        // CVS status command barfs when it hits such a file.
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
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
        
        List statusInfo = statusServerResponse.getStatusInformation();
        for (Iterator i = statusInfo.iterator(); i.hasNext(); ) {
            StatusInformation sinfo = (StatusInformation) i.next();
            
            int status;
            FileStatus fstatus = sinfo.getStatus();
            String workingRev = sinfo.getWorkingRevision();
            if (workingRev == null || workingRev.startsWith("No entry")) {
                workingRev = "";
            }
            
            if (fstatus == FileStatus.NEEDS_CHECKOUT) {
                if (workingRev.length() > 0) {
                    status = TeamStatusInfo.STATUS_DELETED;
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
            else if (fstatus == FileStatus.MODIFIED) {
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
                // This seems to indicate that there's been a local modification,
                // but the file has been removed in the repository
                status = TeamStatusInfo.STATUS_UNRESOLVED;
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
            }
            else {
                // Of course, for files not in the repository, no repository
                // version is available.
                file = sinfo.getFile();
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
        for (Iterator i = files.iterator(); i.hasNext(); ) {
            File file = (File) i.next();

            // See if there's an entry for this file in the metadata
            Entry entry = null;
            try {
                entry = adminHandler.getEntry(file);
            }
            catch (IOException ioe) {
                // Assume no entry
            }
            
            // If there's a metadata entry, it means the file was removed in the
            // repository; otherwise, it has been added locally. We need to check
            // this as if a file was added which previously existed in the repository,
            // the response from CVS won't be recognized in the above code (because
            // the file is in the Attic).
            int status = entry == null ? TeamStatusInfo.STATUS_NEEDSADD :
                TeamStatusInfo.STATUS_REMOVED;
            
            TeamStatusInfo teamInfo = new TeamStatusInfo(file,
                    "",
                    null,
                    status);
            returnInfo.add(teamInfo);
        }

        if (listener != null) {
            while (! returnInfo.isEmpty()) {
                TeamStatusInfo teamInfo = (TeamStatusInfo) returnInfo.removeFirst();
                listener.gotStatus(teamInfo);
            }
        }
        
        return statusServerResponse;
    }

}
