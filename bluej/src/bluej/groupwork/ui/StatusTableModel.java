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

import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
 * @author Amjad Altadmri
 */
public abstract class StatusTableModel extends AbstractTableModel
{
    protected final String resourceLabel = Config.getString("team.status.resource");
    protected final String remoteStatusLabel = Config.getString("team.status.remote");
    protected final String versionLabel = Config.getString("team.status.version");

    protected Project project;
    protected String statusLabel;
    protected List<String> labelsList;
    protected ObservableList<TeamStatusInfo> resources;

    private final int COLUMN_COUNT = 3;

    /**
     *
     */
    public StatusTableModel(Project project, int initialRows)
    {
        this.project = project;
        resources = FXCollections.observableArrayList();
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
        return COLUMN_COUNT;
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
    
    public void setStatusData(ObservableList<TeamStatusInfo> statusResources)
    {
        resources = statusResources;
        fireTableDataChanged();
    }

    public ObservableList<TeamStatusInfo> getResources()
    {
        return resources;
    }
}