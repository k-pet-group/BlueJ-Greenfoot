/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * A list based on JTable that behaves similar to a JList but allows editing of
 * the elements.
 * 
 * TODO: it is to easy to accidentally start editing. Editing should only be
 * triggered by a few specific actions (double click, enter, F2) depending on
 * OS.
 * 
 * @author Poul Henriksen
 */
public class EditableList<T> extends JTable
{
    private DefaultTableModel tableModel;
    private ListSelectionListener selectionListener;

    /**
     * Construct an empty EditableList.
     */
    public EditableList(final boolean editable)
    {
        tableModel = new DefaultTableModel(1, 1) {
            public boolean isCellEditable(int row, int col)
            {
                return editable;
            }
        };
        setModel(tableModel);

        setShowGrid(false);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setIntercellSpacing(new Dimension());
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        getTableHeader().setVisible(false);

        getSelectionModel().addListSelectionListener(new ListSelectionHandler());
    }

    /**
     * Notify that the component has been added as a child of a container.
     */
    public void addNotify()
    {
        // The table header gets added to the enclosing scrollpane from
        // JTable.addNotify(). Remove it again immediately.
        super.addNotify();
        removeHeader();
    }

    /**
     * Ensures that the header of the table is not shown at all!
     * 
     */
    private void removeHeader()
    {
        this.unconfigureEnclosingScrollPane();
    }

    /**
     * Ensure that the preferred width can fit the biggest element in the list.
     */
    private void setPreferredWidthToFit(List<T> listData)
    {
        TableColumn tableColumn = getColumnModel().getColumn(0);
        int contentsMaxWidth = getMaxWidth(listData);
        int prefWidth = tableColumn.getPreferredWidth();
        if (prefWidth < contentsMaxWidth) {
            tableColumn.setPreferredWidth(contentsMaxWidth);
        }
    }

    /**
     * Finds the maximum width among all the elements in the given data.
     */
    private int getMaxWidth(List<T> listData)
    {
        TableColumn tableColumn = getColumnModel().getColumn(0);
        TableCellRenderer ltcr = tableColumn.getCellRenderer();
        int contentsMaxWidth = 0;
        int row = 0;
        for (T data : listData) {
            Component n = ltcr.getTableCellRendererComponent(this, data, false, false, row, 0);
            int labelWidth = n.getPreferredSize().width;
            if (labelWidth > contentsMaxWidth) {
                contentsMaxWidth = labelWidth;
            }
            row++;
        }

        return contentsMaxWidth;
    }

    /**
     * Set the data to populate this list. Will replace the current list with
     * this data.
     */
    public void setListData(List<T> data)
    {

        tableModel.setRowCount(0);
        if (data == null) {
            return;
        }

        for (T object : data) {
            tableModel.addRow(new Object[]{object});

        }

        setPreferredWidthToFit(data);
        revalidate();
    }

    /**
     * If the given value exists in this list, it will be selected.
     * 
     * Uses the 'equals' method to determine if the value exist.
     * 
     * @return The row that was selected, or -1 if the value could not be found.
     */
    public int setSelectedValue(Object value)
    {
        if (value == null) {
            return -1;
        }
        int rowCount = tableModel.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            if (value.equals(getValueAt(row, 0))) {
                setSelectedRow(row);
                return row;
            }
        }
        return -1;
    }
    
    /**
     * Sets the selected row
     */
    public void setSelectedRow(int row)
    {
        getSelectionModel().setSelectionInterval(row, row);
    }

    /**
     * Scrolls the list within an enclosing viewport to make the specified cell
     * completely visible. This calls {@code scrollRectToVisible} with the
     * bounds of the specified cell. For this method to work, the {@code
     * EditableList} must be within a <code>JViewport</code>.
     * <p>
     * If the given index is outside the list's range of cells, this method
     * results in nothing.
     * 
     * @param index the index of the cell to make visible
     * @see JComponent#scrollRectToVisible
     * @see #getCellRect(int, int, boolean)
     */
    public void ensureIndexIsVisible(int index)
    {
        if (index < 0 || index >= tableModel.getRowCount()) {
            return;
        }
        Rectangle cellBounds = getCellRect(index, 0, false);
        if (cellBounds != null) {
            scrollRectToVisible(cellBounds);
        }
    }

    /**
     * Get the currently selected value.
     */
    @SuppressWarnings("unchecked")
    public T getSelectedValue()
    {
        int row = getSelectedRow();
        if (row == -1)
            return null;
        return (T) tableModel.getValueAt(row, 0);
    }
    
    /**
     * Returns an array of the values for the selected cells. The returned values are sorted in increasing index order.
     * @returns: the selected values or an empty list if nothing is selected
     */
    public Object[] getSelectedValues()
    {
        int[] rows = getSelectedRows();
        Object[] selected = new Object[rows.length];
        int count = 0;
        for (int row : rows) {
            selected[count] = tableModel.getValueAt(row, 0);
            count++;
        }
        return selected;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
     */
    public Dimension getPreferredScrollableViewportSize()
    {
        // Limit the preferred viewport width to the preferred width (which will
        // be calculated based on the contents of the list)
        Dimension d = super.getPreferredScrollableViewportSize();
        d.width = Math.min(d.width, getPreferredSize().width);
        return d;
    }

    /**
     * Notifies {@code ListSelectionListener}s added directly to the list of
     * selection changes made to the selection model. {@code JList} listens for
     * changes made to the selection in the selection model, and forwards
     * notification to listeners added to the list directly, by calling this
     * method.
     * <p>
     * This method constructs a {@code ListSelectionEvent} with this list as the
     * source, and the specified arguments, and sends it to the registered
     * {@code ListSelectionListeners}.
     * 
     * @param firstIndex the first index in the range, {@code <= lastIndex}
     * @param lastIndex the last index in the range, {@code >= firstIndex}
     * @param isAdjusting whether or not this is one in a series of multiple
     *            events, where changes are still being made
     * 
     * @see #addListSelectionListener
     * @see #removeListSelectionListener
     * @see javax.swing.event.ListSelectionEvent
     * @see EventListenerList
     */
    protected void fireSelectionValueChanged(int firstIndex, int lastIndex, boolean isAdjusting)
    {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
                }
                ((ListSelectionListener) listeners[i + 1]).valueChanged(e);
            }
        }
    }

    /*
     * A ListSelectionListener that forwards ListSelectionEvents from the
     * selectionModel to the JList ListSelectionListeners. The forwarded events
     * only differ from the originals in that their source is the JList instead
     * of the selectionModel itself.
     */
    private class ListSelectionHandler
        implements ListSelectionListener, Serializable
    {
        public void valueChanged(ListSelectionEvent e)
        {
            fireSelectionValueChanged(e.getFirstIndex(), e.getLastIndex(), e.getValueIsAdjusting());
        }
    }

    /**
     * Adds a listener to the list, to be notified each time a change to the
     * selection occurs; the preferred way of listening for selection state
     * changes. {@code JList} takes care of listening for selection state
     * changes in the selection model, and notifies the given listener of each
     * change. {@code ListSelectionEvent}s sent to the listener have a {@code
     * source} property set to this list.
     * 
     * @param listener the {@code ListSelectionListener} to add
     * @see #getSelectionModel
     * @see #getListSelectionListeners
     */
    public void addListSelectionListener(ListSelectionListener listener)
    {
        if (selectionListener == null) {
            selectionListener = new ListSelectionHandler();
            getSelectionModel().addListSelectionListener(selectionListener);
        }

        listenerList.add(ListSelectionListener.class, listener);
    }

    /**
     * Removes a selection listener from the list.
     * 
     * @param listener the {@code ListSelectionListener} to remove
     * @see #addListSelectionListener
     * @see #getSelectionModel
     */
    public void removeListSelectionListener(ListSelectionListener listener)
    {
        listenerList.remove(ListSelectionListener.class, listener);
    }

}
