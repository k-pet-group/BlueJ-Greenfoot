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
 * @cvs $Id: StatusTableModel.java 6215 2009-03-30 13:28:25Z polle $
 */
public class StatusTableModel extends AbstractTableModel
{
    static final String resourceLabel = Config.getString("team.status.resource");
    static final String statusLabel = Config.getString("team.status.status");
    static final String versionLabel = Config.getString("team.status.version");
 
    private Project project;
    private List<TeamStatusInfo> resources;
    
    /**
     *
     */
    public StatusTableModel(Project project, int initialRows)
    {
        this.project = project;
        resources = new ArrayList<TeamStatusInfo>();
        for(int i = 0; i < initialRows; i++) {
            resources.add(new TeamStatusInfo());
        }
    }
    
    /**
     * Return the name of a particular column
     *
     * @param col   the column we are naming
     * @return      a string of the columns name
     */
    public String getColumnName(int col)
    {
        if (col == 0)
            return resourceLabel;
         else if (col == 1)
            return versionLabel;
        else if (col == 2)
            return statusLabel;

        throw new IllegalArgumentException("bad column number in StatusTableModel::getColumnName()");
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
     * Find the table entry at a particular row and column
     *
     * @param   row     the table row
     * @param   col     the table column
     * @return          the Object at that location in the table
     */
    public Object getValueAt(int row, int col)
    {
        TeamStatusInfo info = (TeamStatusInfo) resources.get(row);
        
        if (col == 0)
            return ResourceDescriptor.getResource(project, info, false);
        else if (col == 1)
            return info.getLocalVersion(); 
        else if (col == 2)
            return new Integer(info.getStatus());

        return null;
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
