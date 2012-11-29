/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012  Michael Kolling and John Rosenberg 
 
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.ConflictDescriptor;
import org.tigris.subversion.javahl.ConflictResult;
import org.tigris.subversion.javahl.Depth;
import org.tigris.subversion.javahl.NodeKind;
import org.tigris.subversion.javahl.Notify2;
import org.tigris.subversion.javahl.NotifyAction;
import org.tigris.subversion.javahl.NotifyInformation;
import org.tigris.subversion.javahl.NotifyStatus;
import org.tigris.subversion.javahl.PropertyData;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClientInterface;
import org.tigris.subversion.javahl.Status;
import org.tigris.subversion.javahl.StatusCallback;
import org.tigris.subversion.javahl.SubversionException;

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
                    // System.out.println("NotifyInfo:");
                    // System.out.println("  path: " + ninfo.getPath());
                    // System.out.println("  revision: " + ninfo.getRevision());
                    // System.out.println("  action: " + NotifyAction.actionNames[ninfo.getAction()]);
                    // System.out.println("  errmsg: " + ninfo.getErrMsg());
                    // System.out.println("  contentstate: " + NotifyStatus.statusNames[ninfo.getContentState()]);
                    // System.out.println("  node kind = " + NodeKind.getNodeKindName(ninfo.getKind()));
                    // System.out.println("  mimetype = " + ninfo.getMimeType());
                    
                    if (ninfo.getKind() == NodeKind.file
                            || ninfo.getKind() == NodeKind.none) {
                        int action = ninfo.getAction();
                        if (ninfo.getPath() != null) {
                            File file = new File(ninfo.getPath());
                            if (action == NotifyAction.update_add
                                    || action == NotifyAction.restore) {
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
                            else if (action == NotifyAction.tree_conflict) {
                                // We get this if the file has been removed in
                                // repository, but modified here.
                                binaryConflicts.add(file);
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
            
            client.update(paths, Revision.getInstance(version),
                    Depth.immediates, false, false, false);
        }
        catch (ClientException ce) {
            if (isCancelled()) {
                return new TeamworkCommandAborted();
            } else {
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
                                file.getAbsolutePath(), "svn:mime-type", Revision.getInstance(version));
                        if (pdata != null && "application/octet-stream".equals(pdata.getValue())) {
                            // This is a binary file
                            i.remove();
                            binaryConflicts.add(file);
                        }
                        else {
                            // remove all the extraneous files that subversion generates
                            client.resolve(file.getAbsolutePath(), 0, ConflictResult.chooseMerged);
                        }
                    }
                    catch (SubversionException se) {
                        Debug.message("Subversion client exception when resolving conflicts: " + se.getLocalizedMessage());
                        Debug.message("   (on file: " + file + ")");
                    }
                }
            }
            if (! conflicts.isEmpty() || ! binaryConflicts.isEmpty()) {
                listener.handleConflicts(this);
            }
        }
        
        return new TeamworkCommandResult();
    }

    public Set<File> getBinaryConflicts()
    {
        return binaryConflicts;
    }
    
    public List<File> getConflicts()
    {
        return conflicts;
    }
    
    public void overrideFiles(final Set<File> files)
    {
        final SVNClientInterface client = getRepository().getClient();

        StatusCallback scb = new StatusCallback() {
            public void doStatus(Status status)
            {
                File file = new File(status.getPath());
                try {
                    ConflictDescriptor cd = status.getConflictDescriptor();
                    if (cd != null) {
                        int action = cd.getAction();
                        int kind = cd.getKind();
                        int reason = cd.getReason();

                        if (kind != ConflictDescriptor.Kind.text) {
                            return; // Can't handle property conflicts
                        }

                        if (action == ConflictDescriptor.Action.delete
                                && reason == ConflictDescriptor.Reason.edited) {
                            // The file was deleted in the repository, but locally edited.
                            // For some reason "chooseMerged" is the only option which works:
                            client.resolve(file.getAbsolutePath(), 0, ConflictResult.chooseMerged);

                            // That leaves the file in the "added" status.
                            boolean keepLocal = ! files.contains(file); 

                            // Doing a remove will either remove the "added" status
                            // (keepLocal == true) or completely remove the file.
                            String [] paths = new String[] { file.getPath() };
                            client.remove(paths, "", true, keepLocal, Collections.emptyMap());

                            return;
                        }
                        else if (action == ConflictDescriptor.Action.edit
                                && reason == ConflictDescriptor.Reason.deleted) {
                            // Locally deleted, but modifications in the repository.
                            // Shouldn't see this normally, seeing as BlueJ does a
                            // "forced update" of such files.
                            client.resolve(file.getAbsolutePath(), 0, ConflictResult.chooseTheirsFull);
                        }
                    }

                    String conflictOld = status.getConflictOld();
                    if (conflictOld != null) {
                        File oldRev = new File(file.getParent(), status.getConflictOld());
                        oldRev.delete();
                    }

                    if (files.contains(file)) {
                        // override with repository version
                        client.resolve(file.getAbsolutePath(), 0, ConflictResult.chooseTheirsFull);
                    }
                    else {
                        // keep working copy version
                        client.resolve(file.getAbsolutePath(), 0, ConflictResult.chooseMineFull);
                    }
                }
                catch (SubversionException sve) {
                    Debug.message("Subversion library exception trying to resolve binary conflict: " + sve.getLocalizedMessage());
                    Debug.message("   (on file: " + file + ")");
                }
            } 
        };
        
        for (Iterator<File> i = binaryConflicts.iterator(); i.hasNext(); ) {
            File file = i.next();
            try {
                client.status(file.getAbsolutePath(), Depth.empty, false,
                        true, true, true, new String[0], scb);
            }
            catch (ClientException ce) {
                Debug.message("Subversion library exception trying to resolve binary conflict: " + ce.getLocalizedMessage());
                Debug.message("   (on file: " + file + ")");
            }
        }
    }
}
