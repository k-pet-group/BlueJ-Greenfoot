package bluej.groupwork.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import bluej.Config;
import bluej.groupwork.TeamStatusInfo;
import java.io.File;

/**
 * Given a list of StatusEntry(s) returns a table model which allows them to
 * be edited in a JTable.
 * 
 * 
 * @author Bruce Quig
 * @cvs $Id: StatusTableModel.java 5066 2007-05-28 04:15:04Z bquig $
 */
public class StatusTableModel extends AbstractTableModel
{
    static final String resourceLabel = Config.getString("team.status.resource");
    static final String statusLabel = Config.getString("team.status.status");
    static final String versionLabel = Config.getString("team.status.version");
 
    private List resources;
    
    /**
     *
     */
    public StatusTableModel(int initialRows)
    {
        resources = new ArrayList();
        for(int i = 0; i < initialRows; i++) {
            resources.add(new TeamStatusInfo());
        }
    }
    
    /**
     * Construct a table model
     *
     */
    public StatusTableModel(List teamResources)
    {
        resources = teamResources;
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
            return info.getFile().getName();
        else if (col == 1)
            return info.getLocalVersion(); 
        else if (col == 2)
            return Integer.valueOf(info.getStatus());

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
    
    public void setStatusData(List statusResources)
    {
        resources = statusResources;
        fireTableDataChanged();
    }
    

}
