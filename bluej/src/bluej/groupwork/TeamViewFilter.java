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
package bluej.groupwork;

import bluej.pkgmgr.BlueJPackageFile;

/**
 * Filter for filtering out certain elements from the views in the groupwork UI.
 * 
 * @author Poul Henriksen
 */
public class TeamViewFilter
{
    /**
     * Filter to identify which files will be shown in the groupwork UI.
     * <p>
     * This will filter out the old bluej package file (bluej.pkg) so that
     * Diagram Layout doesn't appear twice in the same view.
     * @return True if it should be accepted for viewing, false if not.
     */
    public boolean accept(TeamStatusInfo statusInfo)
    {
        if(BlueJPackageFile.isOldPackageFileName(statusInfo.getFile().getName())) {
            return false;
        }
        return true;
    }
}
