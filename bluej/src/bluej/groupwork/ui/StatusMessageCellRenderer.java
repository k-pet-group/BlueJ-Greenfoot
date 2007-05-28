
package bluej.groupwork.ui;

import bluej.groupwork.TeamStatusInfo;
import bluej.utility.Debug;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/*
 * StatusCellRenderer.java
 * Renderer to add colour to the status message of resources inside a StatusFrame
 * @author Bruce Quig
 * @cvs $Id: StatusMessageCellRenderer.java 5066 2007-05-28 04:15:04Z bquig $
 */
public class StatusMessageCellRenderer extends DefaultTableCellRenderer 
{
    
    final static Color UPTODATE = Color.BLACK;
    final static Color NEEDSCHECKOUT = Color.GREEN;
    final static Color DELETED = Color.GRAY;
    final static Color NEEDSUPDATE = Color.BLUE;
    final static Color NEEDSCOMMIT = Color.BLUE;
    final static Color NEEDSMERGE = Color.BLACK;
    final static Color NEEDSADD = Color.GREEN.darker().darker();
    final static Color REMOVED = Color.GRAY;
    

    /**
     * Over-ridden from super class. Get the status message string and appropriate colour
     * for the status. Render using these values.
     */
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean b, boolean b0, int i, int i0)
    {
        if(object instanceof Integer){
            Integer statusInteger = (Integer)object;
            int status = statusInteger.intValue();
            String statusLabel = getStatusString(status);
            setText(statusLabel);
            Color statusColor = getStatusColour(status);
            setForeground(getStatusColour(status));
        }
        return this;
    }
    
    /**
     * get the String value of the statis ID
     */            
    private String getStatusString(int statusValue)
    {
        return TeamStatusInfo.getStatusString(statusValue);
    }
    
    /**
     * get the colour for the given status ID value
     */
    private Color getStatusColour(int statusValue)
    {
        Color color = Color.BLACK;
        
        if(statusValue == TeamStatusInfo.STATUS_UPTODATE)
            color = UPTODATE;
        else if(statusValue == TeamStatusInfo.STATUS_NEEDSCHECKOUT)
            color = NEEDSCHECKOUT;
        else if(statusValue == TeamStatusInfo.STATUS_DELETED)
            color = DELETED;
        else if(statusValue == TeamStatusInfo.STATUS_NEEDSUPDATE)
            color = NEEDSUPDATE;
        else if(statusValue == TeamStatusInfo.STATUS_NEEDSCOMMIT)
            color = NEEDSCOMMIT;
        else if(statusValue == TeamStatusInfo.STATUS_NEEDSMERGE)
            color = NEEDSMERGE;
        else if(statusValue == TeamStatusInfo.STATUS_NEEDSADD)
            color = NEEDSADD;
         else if(statusValue == TeamStatusInfo.STATUS_REMOVED)
            color = REMOVED;
        
        return color;
    }    
}
