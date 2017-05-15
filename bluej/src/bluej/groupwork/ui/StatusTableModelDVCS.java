/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017  Michael Kolling and John Rosenberg
 
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
import bluej.pkgmgr.Project;

import java.util.Arrays;
/**
 * Given a list of StatusEntry(s) returns a table model which allows them to
 * be edited in a JTable.
 * This is only for DVCS providers. It aims to eliminate some confusion in conditions.
 * 
 * @author Amjad Altadmri
 */
public class StatusTableModelDVCS extends StatusTableModel
{
    /**
     *
     */
    public StatusTableModelDVCS(Project project, int initialRows)
    {
        super(project, initialRows);
        statusLabel = Config.getString("team.status.local");
        labelsList = Arrays.asList(resourceLabel, statusLabel, remoteStatusLabel);
    }
    
    /**
     * Find the table entry at a particular row and column
     *
     * @param   row     the table row
     * @param   col     the table column
     * @return          the Object at that location in the table
     */
    public Object getValueAt(int row, int col)
    {
        TeamStatusInfo info = resources.get(row);
        switch (col) {
            case 0:
                return ResourceDescriptor.getResource(project, info, false);
            case 1:
                return info.getStatus();
            case 2:
                return info.getRemoteStatus();
            default:
                break;
        }

        return null;
    }
}