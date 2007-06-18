
package bluej.groupwork.ui;

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
 * @cvs $Id: StatusMessageCellRenderer.java 5103 2007-06-18 04:24:29Z davmac $
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
        
        int status = getStatus(jTable, row);
        setForeground(getStatusColour(status));
        String statusLabel = getStatusString(object, status, row, column);
        setText(statusLabel);
        setForeground(getStatusColour(status));
        
        return this;
    }
    
    private int getStatus(JTable table, int row) 
    {
        int status = 0;
        Object val = table.getModel().getValueAt(row, 2);
        if(val instanceof Integer) {
            status = ((Integer)val).intValue();
        }
        return status;
    }
    
    /**
     * get the String value of the statis ID
     */
    private String getStatusString(Object value, int statusValue, int row, int col) 
    {
        // TODO, change to use column names for ID
        if(col == 0 || col == 1) {
            return value.toString();
        }        
        return TeamStatusInfo.getStatusString(statusValue);
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
}
