/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;

/**
 * Class to determine team resource descriptions for use in dialogs
 * 
 * @author Bruce Quig
 */
public class ResourceDescriptor
{
           
    public static String getResource(Project project, Object value, boolean annotate)
    {
        String status = value.toString();
        if (value instanceof TeamStatusInfo) {
            TeamStatusInfo info = (TeamStatusInfo) value;
            boolean isPkgFile = BlueJPackageFile.isPackageFileName(info.getFile().getName());

            if (isPkgFile) {
                status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
            }
            if (annotate) {
                int infoStatus = info.getStatus();
                // file has been deleted
                switch (infoStatus) {
                    case TeamStatusInfo.STATUS_DELETED:
                        status += " (" + Config.getString("team.status.delete") + ")";
                        break;
                    case TeamStatusInfo.STATUS_NEEDSADD:
                        status += " (" + Config.getString("team.status.add") + ")";
                        break;
                    case TeamStatusInfo.STATUS_NEEDSCHECKOUT:
                        status += " (" + Config.getString("team.status.new") + ")";
                        break;
                    case TeamStatusInfo.STATUS_REMOVED:
                    case TeamStatusInfo.STATUS_CONFLICT_LMRD:
                        status += " (" + Config.getString("team.status.removed") + ")";
                        break;
                    case TeamStatusInfo.STATUS_NEEDSMERGE:
                        if (! isPkgFile) {
                            status += " (" + Config.getString("team.status.needsmerge") + ")";
                        }
                        break;
                    default:
                        break;
                }
                if (info.getRemoteStatus() == TeamStatusInfo.STATUS_NEEDSCHECKOUT
                        || info.getRemoteStatus() == TeamStatusInfo.STATUS_DELETED) {
                    if (!isPkgFile) {
                        //file is ok in local repo, but needs to be pushed to remote repo.
                        status += "(" + Config.getString("team.status.needsupdate") + ")";
                    }
                }
            }
        }

        return status;
    }

    public static String getDCVSResource(Project project, Object value, boolean annotate, boolean remote)
    {
        String status = value.toString();
        if (value instanceof TeamStatusInfo) {
            TeamStatusInfo info = (TeamStatusInfo) value;
            boolean isPkgFile = BlueJPackageFile.isPackageFileName(info.getFile().getName());

            if (isPkgFile) {
                status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
            }
            if (annotate) {
                int infoStatus = remote ? info.getRemoteStatus() : info.getStatus();
                // file has been deleted
                switch (infoStatus) {
                    case TeamStatusInfo.STATUS_DELETED:
                    case TeamStatusInfo.STATUS_NEEDSADD:
                    case TeamStatusInfo.STATUS_NEEDSCHECKOUT:
                    case TeamStatusInfo.STATUS_REMOVED:
                    case TeamStatusInfo.STATUS_CONFLICT_LMRD:
                    case TeamStatusInfo.STATUS_NEEDSUPDATE:
                    case TeamStatusInfo.STATUS_NEEDSCOMMIT:
                        //substitute for the new labels from teamstatusinfo
                        status += " (" + TeamStatusInfo.getDCVSStatusString(infoStatus, remote) + ")";
                        break;
                    case TeamStatusInfo.STATUS_NEEDSMERGE:
                        if (!isPkgFile) {
                            status += " (" + TeamStatusInfo.getDCVSStatusString(infoStatus, remote) + ")";
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        return status;
    }
}
