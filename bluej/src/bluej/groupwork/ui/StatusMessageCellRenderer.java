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
import bluej.groupwork.TeamStatusInfo.Status;
import bluej.pkgmgr.Project;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/*
 * StatusCellRenderer.java
 * Renderer to add colour to the status message of resources inside a StatusFrame
 * @author Bruce Quig
 */
public class StatusMessageCellRenderer extends DefaultTableCellRenderer 
{

    private final static int LOCAL = 0; //local status
    private final static int REMOTE = 1; //remote status
    
    Project project;
    
    public StatusMessageCellRenderer(Project project)
    {
        super();
        this.project = project;
    }
    
   
    /**
     * Over-ridden from super class. Get the status message string and appropriate colour
     * for the status. Render using these values.
     */
    public Component getTableCellRendererComponent(JTable jTable, Object object,
        boolean isSelected, boolean hasFocus , int row, int column) 
    {
        super.getTableCellRendererComponent(jTable, object, isSelected, hasFocus, row, column);
        
        int remoteStatusColumn = getRemoteStatusColumnIndex(jTable);
        
        Status status = getStatus(jTable, row, LOCAL);

        Status remoteStatus = getStatus(jTable, row, REMOTE);
        
        setForeground(status.getStatusColour());
        String statusLabel = getStatusString(jTable, object, status, row, column);
        if (column == remoteStatusColumn)
            statusLabel = getStatusString(jTable, object, remoteStatus, row, column);
        
        setText(statusLabel);
        setForeground(status.getStatusColour());
        
        return this;
    }
    
    private Status getStatus(JTable table, int row, int location)
    {
        Status status = Status.UPTODATE;
        Object val;
        
        int localStatusColumn = project.getTeamSettingsController().isDVCS()?
                table.getColumnModel().getColumnIndex(Config.getString("team.status.local"))
                :table.getColumnModel().getColumnIndex(Config.getString("team.status"));
        
        int remoteStatusColumn = getRemoteStatusColumnIndex(table);
        
        if (location == LOCAL){
            val = table.getModel().getValueAt(row, localStatusColumn);
        } else {
            val = table.getModel().getValueAt(row, remoteStatusColumn);
        }
        
        if(val instanceof Status) {
            status = ((Status)val);
        }
        return status;
    }
    
    /**
     * get the String value of the status ID
     */
    private String getStatusString(JTable jtable, Object value, Status statusValue, int row, int col)
    {
        String colName = jtable.getColumnName(col);

        if (colName.equals(Config.getString("team.status.resource")) || colName.equals(Config.getString("team.status.version"))) {
            return value.toString();
        }
        
        if (project.getTeamSettingsController().isDVCS()) {
            if (colName.equals(Config.getString("team.status.remote"))) {
                return statusValue.getDCVSStatusString(true);
            }
            //return local status.
            return statusValue.getDCVSStatusString(false);
        } else {
            return statusValue.getStatusString();
        }

    }

    /**
     * return the column index of the remote status, if this is a distributed VCS
     * @param jTable
     * @return the column number, -1 if this is not a DCVS.
     */
    private int getRemoteStatusColumnIndex(JTable jTable)
    {
        int result = -1;

        if (project.getTeamSettingsController().isDVCS()) {
            result = jTable.getColumnModel().getColumnIndex(Config.getString("team.status.remote"));
        }
        return result;
    }
}
