package bluej.classmgr;

import bluej.utility.Debug;
import bluej.Config;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import javax.swing.table.*;
import javax.swing.*;

/**
 ** @version $Id: ClassPathTableModel.java 105 1999-06-03 02:14:25Z ajp $
 ** @author Andrew Patterson
 ** Given a list of ClassPathEntry returns a table model which allows them to
 ** be edited in a JTable.
 **/
public class ClassPathTableModel extends AbstractTableModel
{
	static final String locationLabel = Config.getString("classmgr.locationcolumn");
	static final String descriptionLabel = Config.getString("classmgr.descriptioncolumn");
	List entries;

	public ClassPathTableModel(List entries)
	{
		this.entries = entries;
	}

	public String getColumnName(int col) {
		if (col == 0)
			return locationLabel;
		else
			return descriptionLabel;
	}
    
	public int getRowCount() { return entries.size(); }
	public int getColumnCount() { return 2; }

	public Object getValueAt(int row, int col) {
		ClassPathEntry entry = (ClassPathEntry)entries.get(row);

		if(col == 0)
			return entry.getCanonicalPathNoException();
		else
			return entry.getDescription(); 
	}

	public boolean isCellEditable(int row, int col)
	{
		return (col == 1);
	}

	public void setValueAt(Object value, int row, int col) {
		if (col == 1) {
			ClassPathEntry entry = (ClassPathEntry)entries.get(row);

			entry.setDescription((String)value);

			fireTableCellUpdated(row, col);
		}
	}

	public void addEntry(ClassPathEntry cpe)
	{
		int s = entries.size();
		entries.add(cpe);
		fireTableRowsInserted(s,s);
	}

	public void deleteEntry(int index)
	{
		if(index < entries.size() && index >= 0) {
			entries.remove(index);
	    	fireTableRowsDeleted(index, index);
		}
	}
}
