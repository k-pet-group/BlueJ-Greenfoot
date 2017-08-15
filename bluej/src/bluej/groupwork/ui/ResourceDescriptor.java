/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.ui;

import bluej.Config;
import bluej.groupwork.TeamStatusInfo;
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Class to determine team resource descriptions for use in dialogs
 * 
 * @author Bruce Quig
 */
@OnThread(Tag.FXPlatform)
public class ResourceDescriptor
{
    public static String getResource(Project project, Object value, boolean annotate)
    {
        String status;
        if (value instanceof UpdateStatus) {
        	UpdateStatus updateStatus = (UpdateStatus) value;
        	if (updateStatus.infoStatus != null) {
        		TeamStatusInfo info = updateStatus.infoStatus;
        		status = info.toString();
                boolean isPkgFile = BlueJPackageFile.isPackageFileName(info.getFile().getName());

                if (isPkgFile) {
                    status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
                }
                if (annotate) {
                    Status infoStatus = info.getStatus();
                    // file has been deleted
                    switch (infoStatus) {
                        case DELETED:
                            status += " (" + Config.getString("team.status.delete") + ")";
                            break;
                        case NEEDS_ADD:
                            status += " (" + Config.getString("team.status.add") + ")";
                            break;
                        case NEEDS_CHECKOUT:
                            status += " (" + Config.getString("team.status.new") + ")";
                            break;
                        case REMOVED:
                        case CONFLICT_LMRD:
                            status += " (" + Config.getString("team.status.removed") + ")";
                            break;
                        case NEEDS_MERGE:
                            if (! isPkgFile) {
                                status += " (" + Config.getString("team.status.needsmerge") + ")";
                            }
                            break;
                        default:
                            break;
                    }
                    if (info.getRemoteStatus() == Status.NEEDS_CHECKOUT
                            || info.getRemoteStatus() == Status.DELETED) {
                        if (!isPkgFile) {
                            //file is ok in local repo, but needs to be pushed to remote repo.
                            status += "(" + Config.getString("team.status.needsupdate") + ")";
                        }
                    }
                }
        	}
        	else {
        		status = updateStatus.stringStatus;
        	}
        }
        else {
        	status = value.toString();
        }
        
        return status;
    }

    public static String getDCVSResource(Project project, Object value, boolean annotate, boolean remote)
    {
        String status;
        if (value instanceof UpdateStatus) {
        	UpdateStatus updateStatus = (UpdateStatus) value;
        	if (updateStatus.infoStatus != null) {

        		TeamStatusInfo info = updateStatus.infoStatus;
        		status = info.toString();
        		boolean isPkgFile = BlueJPackageFile.isPackageFileName(info.getFile().getName());

        		if (isPkgFile) {
        			status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
        		}
        		if (annotate) {
        			Status infoStatus = remote ? info.getRemoteStatus() : info.getStatus();
        			// file has been deleted
        			switch (infoStatus) {
        			case DELETED:
        			case NEEDS_ADD:
        			case NEEDS_CHECKOUT:
        			case REMOVED:
        			case CONFLICT_LMRD:
        			case NEEDS_UPDATE:
        			case NEEDS_COMMIT:
        				//substitute for the new labels from teamstatusinfo
        				status += " (" + infoStatus.getDCVSStatusString(remote) + ")";
        				break;
        			case NEEDS_MERGE:
        				if (!isPkgFile) {
        					status += " (" + infoStatus.getDCVSStatusString(remote) + ")";
        				}
        				break;
        			default:
        				break;
        			}
        		}
        	}
        	else {
        		status = updateStatus.stringStatus;
        	}
        }
        else {
        	status = value.toString();
        }

        return status;
    }
}
