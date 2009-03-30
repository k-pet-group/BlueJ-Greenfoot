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
package bluej.groupwork.ui;

import bluej.Config;
import bluej.groupwork.TeamStatusInfo;
import bluej.pkgmgr.BlueJPackageFile;
import bluej.pkgmgr.Project;

/**
 * Class to determine team resource descriptions for use in dialogs
 * 
 * @author Bruce Quig
 * @version $Id: ResourceDescriptor.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ResourceDescriptor
{
           
    public static String getResource(Project project, Object value, boolean annotate)
    {
        String status = value.toString();
        if(value instanceof TeamStatusInfo) {
            TeamStatusInfo info = (TeamStatusInfo)value;
            boolean isPkgFile = BlueJPackageFile.isPackageFileName(info.getFile().getName());

            if (isPkgFile) {
                  status = Config.getString("team.commit.layout") + " " + project.getPackageForFile(info.getFile());
            }
            if(annotate) {
                // file has been deleted
                if(info.getStatus() == TeamStatusInfo.STATUS_DELETED) {
                    status += " (" + Config.getString("team.status.delete") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_NEEDSADD) {
                    status += " (" + Config.getString("team.status.add") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_NEEDSCHECKOUT) {
                    status += " (" + Config.getString("team.status.new") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_REMOVED
                        || info.getStatus() == TeamStatusInfo.STATUS_CONFLICT_LMRD) {
                    status += " (" + Config.getString("team.status.removed") + ")";
                }
                else if (info.getStatus() == TeamStatusInfo.STATUS_NEEDSMERGE) {
                    if (! isPkgFile) {
                        status += " (" + Config.getString("team.status.needsmerge") + ")";
                    }
                }
            }
        }
        
        return status;
    }
   
}
