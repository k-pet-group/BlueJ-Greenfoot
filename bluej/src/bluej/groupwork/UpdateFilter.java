/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2018  Michael Kolling and John Rosenberg
 
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

import bluej.groupwork.TeamStatusInfo.Status;

/**
 * Class to filter TeamStatusInfo objects to calculate those classes that will 
 * be changed when we next update. 
 *
 * @author Davin McCall
 */
public class UpdateFilter
{
    /**
     * Filter to identify which files in a repository will be altered at 
     * the next update.
     */
    public boolean accept(TeamStatusInfo statusInfo)
    {
        boolean isDir = statusInfo.getFile().isDirectory();
        Status stat = statusInfo.getStatus();
        Status remoteStat = statusInfo.getRemoteStatus();
        
        if (stat == Status.NEEDS_CHECKOUT || remoteStat == Status.NEEDS_CHECKOUT) {
            return true;
        }
        if (stat == Status.NEEDS_MERGE || remoteStat == Status.NEEDS_MERGE) {
            return ! isDir;
        }
        if (stat == Status.NEEDS_UPDATE || remoteStat == Status.NEEDS_UPDATE) {
            return ! isDir;
        }
        if (stat == Status.REMOVED || remoteStat == Status.REMOVED) {
            return true;
        }
        if (stat == Status.CONFLICT_LDRM || remoteStat == Status.CONFLICT_LDRM) {
            // Locally deleted, remotely modified. Update pulls the repository version
            return true;
        }
        if (stat == Status.CONFLICT_LMRD || remoteStat == Status.CONFLICT_LMRD) {
            // Update will succeed if forced (for bluej.pkg files)
            return true;
        }
        
        if (remoteStat == Status.NEEDS_UPDATE) { //REMOTE_STATUS_MODIFIED
            return true;
        }
    
        return false;
    }
    
    /**
     * For the given remote status, check whether an update will affect the file.
     */
    public boolean acceptDist(Status remoteStatus)
    {
        switch (remoteStatus)
        {
            case CONFLICT_ADD:
            case CONFLICT_LDRM:
            case CONFLICT_LMRD:
            case NEEDS_CHECKOUT:
            case NEEDS_UPDATE:
            case NEEDS_MERGE:
            case REMOVED:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * For layout files, checks whether the file should be updated unconditionally.
     */
    public boolean updateAlways(TeamStatusInfo statusInfo)
    {
        Status remoteStatus = statusInfo.getRemoteStatus();
        if (statusInfo.getStatus() == Status.NEEDS_CHECKOUT || remoteStatus == Status.NEEDS_CHECKOUT) {
            return true;
        }
        if (statusInfo.getStatus() == Status.REMOVED || remoteStatus == Status.REMOVED) {
            return true;
        }
        if (statusInfo.getStatus() == Status.CONFLICT_LMRD || remoteStatus == Status.CONFLICT_LMRD) {
            return true;
        }
        return false;
    }
}
