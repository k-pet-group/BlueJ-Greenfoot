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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import bluej.Config;
import bluej.groupwork.TeamStatusInfo;
import bluej.pkgmgr.Project;

/**
 * Given a list of StatusEntry(s) returns a table model which allows them to
 * be edited in a JTable.
 * 
 * 
 * @author Bruce Quig
 */
public abstract class StatusTableModel extends AbstractTableModel
{
    protected final String resourceLabel = Config.getString("team.status.resource");
    protected final String remoteStatusLabel = Config.getString("team.status.remote");
    protected final String versionLabel = Config.getString("team.status.version");

    protected Project project;
    protected List<TeamStatusInfo> resources;
    protected String statusLabel;

    /**
     *
     */
    public StatusTableModel(Project project, int initialRows)
    {
        this.project = project;
        resources = new ArrayList<>();
        for(int i = 0; i < initialRows; i++) {
            resources.add(new TeamStatusInfo());
        }
    }

    /**
     * Return the number of rows in the table
     *
     * @return      the number of rows in the table
     */
    public int getRowCount()
    {
        return resources.size();
    }
    
    /**
     * Return the number of columns in the table
     *
     * @return      the number of columns in the table
     */
    public int getColumnCount()
    {
        return 3;
    }
    
    /**
     * Indicate that nothing is editable
     */
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

    /**
     * Set the table entry at a particular row and column (only
     * valid for the location column)
     *
     * @param   value   the Object at that location in the table
     * @param   row     the table row
     * @param   col     the table column
     */
    public void setValueAt(Object value, int row, int col)
    {
       // do nothing here
    }
    
    public void clear()
    {
        resources.clear();
        fireTableDataChanged();
    }
    
    public void setStatusData(List<TeamStatusInfo> statusResources)
    {
        resources = statusResources;
        fireTableDataChanged();
    }
}