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
package bluej.classmgr;

import javax.swing.table.*;
import bluej.Config;

/**
 * Given a list of ClassPathEntry returns a table model which allows them to
 * be edited in a JTable.
 *
 * The model implements a form of rollback which allows the table to be
 * edited and then changes can be reverted or committed.
 *
 * @author  Andrew Patterson
 * @cvs     $Id: ClassPathTableModel.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ClassPathTableModel extends AbstractTableModel
{
    static final String statusLabel = Config.getString("classmgr.statuscolumn");
    static final String locationLabel = Config.getString("classmgr.locationcolumn");

    private ClassPath origcp;
    private ClassPath cp;

    /**
     * Construct a table model of a class path
     *
     * @param origcp    the class path to model
     */
    public ClassPathTableModel(ClassPath origcp)
    {
        this.origcp = origcp;
        this.cp = new ClassPath(origcp);
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
            return statusLabel;
        else if (col == 1)
            return locationLabel;

        throw new IllegalArgumentException("bad column number in ClassPathTableModel::getColumnName()");
    }

    /**
     * Return the number of rows in the table
     *
     * @return      the number of rows in the table
     */
    public int getRowCount()
    {
        return cp.getEntries().size();
    }
    
    /**
     * Return the number of columns in the table
     *
     * @return      the number of columns in the table
     */
    public int getColumnCount()
    {
        return 2;
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
        ClassPathEntry entry = (ClassPathEntry)cp.getEntries().get(row);

        if (col == 0)
            return entry.getStatusString();
        else if (col == 1)
            return entry.getCanonicalPathNoException();
        else if (col == 2)
            return entry.getDescription(); 

        throw new IllegalArgumentException("bad column number in ClassPathTableModel::getValueAt()");
    }

    /**
     * Indicate that only our location column is edititable
     */
    public boolean isCellEditable(int row, int col)
    {
        return (col == 2);
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
        if (col == 2) {
            ClassPathEntry entry = (ClassPathEntry)cp.getEntries().get(row);

            entry.setDescription((String)value);

            fireTableCellUpdated(row, col);
        }
    }

    public void addEntry(ClassPathEntry cpe)
    {
        int s = cp.getEntries().size();
        cp.getEntries().add(cpe);
        fireTableRowsInserted(s,s);
    }

    public void deleteEntry(int index)
    {
        if(index < cp.getEntries().size() && index >= 0) {
            cp.getEntries().remove(index);
            fireTableRowsDeleted(index, index);
        }
    }

    public void commitEntries()
    {
        origcp.removeAll();
        origcp.addClassPath(cp);
    }

    public void revertEntries()
    {
        cp = new ClassPath(origcp);

        fireTableDataChanged();
    }
}
