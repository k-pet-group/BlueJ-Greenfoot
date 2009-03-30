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
    private Set<File> files;
    private Set<File> forceFiles;
    private UpdateListener listener;
    private List<File> conflicts = new ArrayList<File>();
    private Set<File> binaryConflicts = new HashSet<File>();
    
    public SvnUpdateToCommand(SvnRepository repository, UpdateListener listener,
            long version, Set<File> files, Set<File> forceFiles)
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
        final List<File> addedList = new ArrayList<File>();
        final List<File> updatedList = new ArrayList<File>();
        final List<File> removedList = new ArrayList<File>();
        final List<File> removedDirs = new ArrayList<File>();
        
        try {
            String [] paths = new String[forceFiles.size() + files.size()];
            int j = 0;
            for (Iterator<File> i = forceFiles.iterator(); i.hasNext(); ) {
                File file = i.next();
                paths[j++] = file.getAbsolutePath();
                // Delete the file, so the update cannot conflict
                file.delete();
            }
            for (Iterator<File> i = files.iterator(); i.hasNext(); ) {
                File file = i.next();
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
                    else if (ninfo.getKind() == NodeKind.dir) {
                        int action = ninfo.getAction();
                        if (action == NotifyAction.update_delete) {
                            removedDirs.add(new File(ninfo.getPath()));
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
            
            Iterator<File> i;
            for (i = addedList.iterator(); i.hasNext(); ) {
                listener.fileAdded(i.next());
            }
            for (i = updatedList.iterator(); i.hasNext(); ) {
                listener.fileUpdated(i.next());
            }
            for (i = removedList.iterator(); i.hasNext(); ) {
                listener.fileRemoved(i.next());
            }
            for (i = removedDirs.iterator(); i.hasNext(); ) {
                listener.dirRemoved(i.next());
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
        
        for (Iterator<File> i = binaryConflicts.iterator(); i.hasNext(); ) {
            File file = i.next();
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
