/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017 Michael Kolling and John Rosenberg
 
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
package bluej.groupwork;

import bluej.Config;

import java.awt.*;
import java.io.File;

/**
 * Team status information for a file
 * 
 * @author Davin McCall
 */
public class TeamStatusInfo
{
    private File file;
    private String localVersion;
    private String remoteVersion;
    private Status status;
    private Status remoteStatus;
    private final static String prefix = "team.statusinfo.";

    private final static Color UPTODATE_COLOR = Color.BLACK;
    private final static Color CONFLICT_COLOR = new Color(137,13,19);   // darker red
    private final static Color NEEDSUPDATE_COLOR = new Color(11,57,120);  // blue
    private final static Color REMOVED_COLOR = new Color(135,150,170);     // grey-blue
    private final static Color NEEDSCOMMIT_COLOR = new Color(10,85,15);   // green
    private final static Color DELETED_COLOR = new Color(122,143,123);      // grey-green

    public enum Status
    {
        /** The file is up-to-date, the local revision is the same as in the repository */
        UPTODATE("upToDate", "", "", UPTODATE_COLOR),
        /* File is up-to-date on the remote repository*/
        // REMOTE_STATUS_UPTODATE = UPTODATE

        /** The file doesn't exist locally, but is in the repository */
        NEEDSCHECKOUT("needsCheckout", "", "dcvs.remote.needs.pull", NEEDSUPDATE_COLOR),
        /** The file has been deleted locally, but the deletion hasn't been committed yet */
        DELETED("deleted", "dcvs.local.deleted", "dcvs.remote.deleted", DELETED_COLOR),
        /** The repository version is newer */
        NEEDSUPDATE("needsUpdate", "", "dcvs.remote.needs.pull", NEEDSUPDATE_COLOR),
        /* File has been modified on remote repository */
        //REMOTE_STATUS_MODIFIED = NEEDSUPDATE

        /** The local version has been modified */
        NEEDSCOMMIT("needsCommit", "dcvs.local.modified", "dcvs.remote.modified", NEEDSCOMMIT_COLOR),
        /** The repository version is newer, but the file is also locally modified */
        NEEDSMERGE("needsMerge", "needsMerge", "needsMerge"),
        /** The file exists locally, but not in the repository */
        NEEDSADD("needsAdd", "dcvs.local.new", "dcvs.remote.new", NEEDSCOMMIT_COLOR),
        /** The file exists locally, but has been removed in the repository */
        REMOVED("removed", "", "dcvs.remote.needs.pull", REMOVED_COLOR),
        /**
         * An unresolved conflict. This can happen when:<ul>
         * <li>when two binary files have been modified maybe?
         * </ul><p>
         *
         * The only way out is to either delete the file locally, or do a forced
         * commit or a forced update.
         */
        UNRESOLVED("unresolved", "unresolved", "unresolved"),
        /**
         * There are conflicts now present in the local file. The local file
         * needs to be edited to resolve the conflicts. (This state occurs
         * after an update which merged changes from the repository).
         */
        HASCONFLICTS("hasConflicts", "hasConflicts", "hasConflicts"),
        /** Unknown */
        WEIRD("weird", "weird", "weird"),
        /** It has no status, only used for default constructor while waiting for cvs */
        BLANK("", "", ""),
        /**
         * The file has been created locally, but a file with the same name has been
         * added in the repository. This is a conflict.
         */
        CONFLICT_ADD("conflictAdd", "conflictAdd", "conflictAdd"),
        /**
         * Locally modified, but deleted in repository (conflict)
         */
        CONFLICT_LMRD("conflictLMRD", "conflictLMRD", "conflictLMRD"),
        /**
         * Locally deleted, but modified in repository (conflict)
         */
        CONFLICT_LDRM("conflictLDRM", "conflictLDRM", "conflictLDRM"),
        /** The file was renamed **/
        RENAMED("renamed", "renamed", "renamed"),
        /* File has been renamed */
        //REMOTE_STATUS_RENAMED = RENAMED;

        /** File needs to be pushed to remote repository */
        NEEDS_PUSH("dcvs.needsPush", "", "dcvs.needsPush");

        private final String statusString;
        private final String dcvsStatusString;
        private final String dcvsRemoteStatusString;
        private final Color  color;

        Status(String statusString, String dcvsStatusString, String dcvsRemoteStatusString)
        {
            this(statusString, dcvsStatusString, dcvsRemoteStatusString, CONFLICT_COLOR);
        }

        Status(String statusString, String dcvsStatusString, String dcvsRemoteStatusString, Color color)
        {
            this.statusString = statusString;
            this.dcvsStatusString = dcvsStatusString;
            this.dcvsRemoteStatusString = dcvsRemoteStatusString;
            this.color = color;
        }

        public String getStatusString()
        {
            return statusString.isEmpty() ? "" : Config.getString(prefix + statusString);
        }

        public String getDCVSStatusString(boolean remote)
        {
            String label = (remote) ? dcvsRemoteStatusString : dcvsStatusString;
            return label.isEmpty() ? "" : Config.getString( prefix + label);
        }

        /**
         * get the colour for the given status ID value
         */
        public Color getStatusColour()
        {
            return color;
        }
    }
    
    /**
     * Default "blank" status info. Used to pre-populate status table whilst 
     * awaiting repository response.
     */
    public TeamStatusInfo()
    {
        this(new File(""), "", "", Status.BLANK, Status.BLANK);
    }

    /**
     * constructor used with DVCS.
     *
     * @param file file in the local file system.
     * @param localVersion file version in the local repository.
     * @param remoteVersion file version in the remote server.
     * @param status file status in the local repository.
     * @param remoteStatus file status in the remote server.
     */
    public TeamStatusInfo(File file, String localVersion, String remoteVersion, Status status, Status remoteStatus)
    {
        this(file, localVersion, remoteVersion, status);
        this.remoteStatus = remoteStatus;
    }
    
    public TeamStatusInfo(File file, String localVersion, String remoteVersion, Status status)
    {
        this.file = file;
        this.localVersion = localVersion;
        this.remoteVersion = remoteVersion;
        this.status = status;
        //no information about the remote status, therefore assume it is up-to-date:
        //this will be re-writen by getStatus command, if necessary.
        this.remoteStatus = Status.UPTODATE; //REMOTE_STATUS_UPTODATE
    }
    
    public File getFile()
    {
        return file;
    }
    
    public String getLocalVersion()
    {
        return localVersion;
    }
    
    public String getRepositoryVersion()
    {
        return remoteVersion;
    }
    
    public Status getStatus()
    {
        return status;
    }
    
    public void setStatus(Status s)
    {
        status = s;
    }
    
    public Status getRemoteStatus()
    {
        return remoteStatus;
    }

    public void setRemoteStatus(Status remoteStatus)
    {
        this.remoteStatus = remoteStatus;
    }
    
    @Override
    public String toString()
    {
        return getFile().getName();
    }
}