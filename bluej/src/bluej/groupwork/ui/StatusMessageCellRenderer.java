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
import bluej.pkgmgr.Project;
import java.awt.Color;
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
    private final static Color UPTODATE = Color.BLACK;
    private final static Color NEEDSUPDATE = new Color(11,57,120);  // blue
    private final static Color NEEDSCHECKOUT = NEEDSUPDATE;
    private final static Color REMOVED = new Color(135,150,170);     // grey-blue
    private final static Color NEEDSMERGE = new Color(137,13,19);   // red
    private final static Color NEEDSCOMMIT = new Color(10,85,15);   // green
    private final static Color NEEDSADD = NEEDSCOMMIT;
    private final static Color DELETED = new Color(122,143,123);      // grey-green
    private final static Color CONFLICT = NEEDSMERGE;   // darker red

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
        
        int status = getStatus(jTable, row, LOCAL);
        int remoteStatus = getStatus(jTable, row, REMOTE);
        
        setForeground(getStatusColour(status));
        String statusLabel = getStatusString(jTable, object, status, row, column);
        if (column == remoteStatusColumn)
            statusLabel = getStatusString(jTable, object, remoteStatus, row, column);
        
        setText(statusLabel);
        setForeground(getStatusColour(status));
        
        return this;
    }
    
    private int getStatus(JTable table, int row, int location) 
    {
        int status = 0;
        Object val;
        
        int localStatusColumn = project.getTeamSettingsController().isDVCS()?
                table.getColumnModel().getColumnIndex(Config.getString("team.status.status"))
                :table.getColumnModel().getColumnIndex(Config.getString("team.status"));
        
        int remoteStatusColumn = getRemoteStatusColumnIndex(table);
        
        if (location == LOCAL){
            val = table.getModel().getValueAt(row, localStatusColumn);
        } else {
            val = table.getModel().getValueAt(row, remoteStatusColumn);
        }
        
        if(val instanceof Integer) {
            status = ((Integer)val);
        }
        return status;
    }
    
    /**
     * get the String value of the status ID
     */
    private String getStatusString(JTable jtable, Object value, int statusValue, int row, int col) 
    {
        String colName = jtable.getColumnName(col);

        if (colName.equals(Config.getString("team.status.resource")) || colName.equals(Config.getString("team.status.version"))) {
            return value.toString();
        }
        
        if (project.getTeamSettingsController().isDVCS()) {
            if (colName.equals(Config.getString("team.status.remoteStatus"))) {
                return TeamStatusInfo.getDCVSStatusString(statusValue,true);
            }
            //return local status.
            return TeamStatusInfo.getDCVSStatusString(statusValue, false);
        } else {
            return TeamStatusInfo.getStatusString(statusValue);
        }

    }
    
    /**
     * get the colour for the given status ID value
     */
    private Color getStatusColour(int statusValue) 
    {
        Color color = Color.BLACK;
        
        if (statusValue == TeamStatusInfo.STATUS_UPTODATE) {
            color = UPTODATE;
        }
        else if (statusValue == TeamStatusInfo.STATUS_NEEDSCHECKOUT) {
            color = NEEDSCHECKOUT;
        }
        else if (statusValue == TeamStatusInfo.STATUS_DELETED) {
            color = DELETED;
        }
        else if (statusValue == TeamStatusInfo.STATUS_NEEDSUPDATE) {
            color = NEEDSUPDATE;
        }
        else if (statusValue == TeamStatusInfo.STATUS_NEEDSCOMMIT) {
            color = NEEDSCOMMIT;
        }
        else if (statusValue == TeamStatusInfo.STATUS_NEEDSMERGE) {
            color = NEEDSMERGE;
        }
        else if (statusValue == TeamStatusInfo.STATUS_NEEDSADD) {
            color = NEEDSADD;
        }
        else if (statusValue == TeamStatusInfo.STATUS_REMOVED) {
            color = REMOVED;
        }
        else {
            color = CONFLICT;
        }
        
        return color;
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
            result = jTable.getColumnModel().getColumnIndex(Config.getString("team.status.remoteStatus"));
        }
        return result;
    }
}
