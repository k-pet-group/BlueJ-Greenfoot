/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017,2019  Michael Kolling and John Rosenberg
 
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
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.List;

/**
 * Given a list of StatusEntry(s) returns a table model which allows them to
 * be edited in a JTable.
 *
 * @author Amjad Altadmri
 */
public class StatusTableModel
{
    private final String resourceLabel = Config.getString("team.status.resource");
    private final String remoteStatusLabel = Config.getString("team.status.remote");

    private String statusLabel;
    private List<String> labelsList;
    private ObservableList<TeamStatusInfo> resources;
    /**
     *
     */
    public StatusTableModel()
    {
        statusLabel = Config.getString("team.status.local");
        labelsList = Arrays.asList(resourceLabel, statusLabel, remoteStatusLabel);
    }
    
     /**
     * Return the name of a particular column
     *
     * @param col   the column we are naming
     * @return      a string of the columns name
     */
    public String getColumnName(int col)
    {
        try {
            return labelsList.get(col);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("bad column number in StatusTableModel::getColumnName()");
        }
    }

    public void setStatusData(ObservableList<TeamStatusInfo> statusResources)
    {
        resources = statusResources;
    }

    public ObservableList<TeamStatusInfo> getResources()
    {
        return resources;
    }
}