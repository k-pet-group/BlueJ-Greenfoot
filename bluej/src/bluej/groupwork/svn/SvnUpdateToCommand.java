package bluej.groupwork.svn;

import java.io.File;
import java.util.*;

import org.tigris.subversion.javahl.*;

import bluej.groupwork.TeamworkCommandAborted;
import bluej.groupwork.TeamworkCommandError;
import bluej.groupwork.TeamworkCommandResult;
import bluej.groupwork.UpdateListener;
import bluej.groupwork.UpdateResults;
import bluej.utility.Debug;

/**
 * Subversion command to update to a particular revision.
 * 
 * @author Davin McCall
 */
public class SvnUpdateToCommand extends SvnCommand implements UpdateResults
{
    private long version;
    private Set files;
    private Set forceFiles;
    private UpdateListener listener;
    private List conflicts = new ArrayList();
    private Set binaryConflicts = new HashSet();
    
    public SvnUpdateToCommand(SvnRepository repository, UpdateListener listener,
            long version, Set files, Set forceFiles)
    {
        super(repository);
        this.version = version;
        this.files = files;
        this.forceFiles = forceFiles;
        this.listener = listener;
    }
    
    protected TeamworkCommandResult doCommand()
    {
        SVNClientInterface client = getClient();

        // The subversion client library gives us update notifications before
        // the notifications have actually been performed. So, we save the
        // list of updated files and notify the listener only once the update
        // is complete.
        final List addedList = new ArrayList();
        final List updatedList = new ArrayList();
        final List removedList = new ArrayList();
        
        try {
            String [] paths = new String[forceFiles.size() + files.size()];
            int j = 0;
            for (Iterator i = forceFiles.iterator(); i.hasNext(); ) {
                File file = (File) i.next();
                paths[j++] = file.getAbsolutePath();
                // Delete the file, so the update cannot conflict
                file.delete();
            }
            for (Iterator i = files.iterator(); i.hasNext(); ) {
                File file = (File) i.next();
                paths[j++] = file.getAbsolutePath();
            }

            client.notification2(new Notify2() {
                public void onNotify(NotifyInformation ninfo)
                {
//                    System.out.println("NotifyInfo:");
//                    System.out.println("  path: " + ninfo.getPath());
//                    System.out.println("  revision: " + ninfo.getRevision());
//                    System.out.println("  action: " + NotifyAction.actionNames[ninfo.getAction()]);
//                    System.out.println("  errmsg: " + ninfo.getErrMsg());
//                    System.out.println("  contentstate: " + NotifyStatus.statusNames[ninfo.getContentState()]);
//                    System.out.println("  node kind = " + NodeKind.getNodeKindName(ninfo.getKind()));
//                    System.out.println("  mimetype = " + ninfo.getMimeType());
                    
                    if (ninfo.getKind() == NodeKind.file
                            || ninfo.getKind() == NodeKind.none) {
                        int action = ninfo.getAction();
                        if (ninfo.getPath() != null) {
                            File file = new File(ninfo.getPath());
                            if (action == NotifyAction.update_add) {
                                addedList.add(file);
                            }
                            else if (action == NotifyAction.update_update) {
                                updatedList.add(file);
                                if (ninfo.getContentState() == NotifyStatus.conflicted) {
                                    conflicts.add(file);
                                }
                            }
                            else if (action == NotifyAction.update_delete) {
                                removedList.add(file);
                            }
                        }
                    }
                    
                }
            });
            
            client.update(paths, Revision.getInstance(version), true, false);
        }
        catch (ClientException ce) {
            if (! isCancelled()) {
                return new TeamworkCommandError(ce.getMessage(), ce.getLocalizedMessage());
            }
        }
        finally {
            client.notification2(null);
            
            Iterator i;
            for (i = addedList.iterator(); i.hasNext(); ) {
                listener.fileAdded((File) i.next());
            }
            for (i = updatedList.iterator(); i.hasNext(); ) {
                listener.fileUpdated((File) i.next());
            }
            for (i = removedList.iterator(); i.hasNext(); ) {
                listener.fileRemoved((File) i.next());
            }
            
            if (! conflicts.isEmpty()) {
                for (i = conflicts.iterator(); i.hasNext(); ) {
                    File file = (File) i.next();
                    try {
                        PropertyData pdata = client.propertyGet(
                                file.getAbsolutePath(), "svn:mime-type");
                        if ("application/octet-stream".equals(pdata.getValue())) {
                            // This is a binary file
                            i.remove();
                            binaryConflicts.add(file);
                        }
                        else {
                            // remove all the extraneous files that subversion generates
                            client.resolved(file.getAbsolutePath(), false);
                        }
                    }
                    catch (ClientException ce) {
                        Debug.message("Subversion client exception when resolving conflicts: " + ce.getLocalizedMessage());
                        Debug.message("   (on file: " + file + ")");
                    }
                }
            }
            if (! conflicts.isEmpty() || ! binaryConflicts.isEmpty()) {
                listener.handleConflicts(this);
            }
        }
        
        return new TeamworkCommandAborted();
    }

    public Set getBinaryConflicts()
    {
        return binaryConflicts;
    }
    
    public List getConflicts()
    {
        return conflicts;
    }
    
    public void overrideFiles(Set files)
    {
        SVNClientInterface client = getRepository().getClient();
        
        for (Iterator i = binaryConflicts.iterator(); i.hasNext(); ) {
            File file = (File) i.next();
            try {
                Status status = client.singleStatus(file.getAbsolutePath(), false);
                File working = new File(file.getParent(), status.getConflictWorking());
                File oldRev = new File(file.getParent(), status.getConflictOld());
                File newRev = new File(file.getParent(), status.getConflictNew());
                
                oldRev.delete();
                if (files.contains(file)) {
                    // override with repository version
                    working.delete();
                    if (! newRev.renameTo(file)) {
                        // on some systems, have to remove destination first
                        file.delete();
                        newRev.renameTo(file);
                    }
                }
                else {
                    // keep working copy version
                    newRev.delete();
                    working.delete();
                }
            }
            catch (ClientException ce) {
                Debug.message("Subversion library exception trying to resolve binary conflict: " + ce.getLocalizedMessage());
                Debug.message("   (on file: " + file + ")");
            }
        }
    }
}
