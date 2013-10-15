/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.debugmgr.inspector;

import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import bluej.Config;
import bluej.debugger.DebuggerObject;

/**
 * A graphical representation of a list of fields from a class or object.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 *  
 */
public class FieldList extends JTable
{
    private int preferredWidth;

    /**
     * Creates a new fieldlist with no data.
     * 
     * @param preferredWidth
     *            Used to determine the width of the columns. This will be the
     *            default width of the list when the inspector is first shown.
     * @param valueFieldColor
     *            background color of the value field.
     */
    public FieldList(int preferredWidth, Color valueFieldColor)
    {
        super(new ListTableModel());

        this.preferredWidth = preferredWidth;
        this.setShowGrid(false);
        this.setRowSelectionAllowed(true);
        this.setColumnSelectionAllowed(false);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setIntercellSpacing(new Dimension());
        this.setDefaultRenderer(Object.class, new ListTableCellRenderer(valueFieldColor));

        this.setRowHeight(25);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.getTableHeader().setVisible(false);
        
        // Allow tab/shift-tab to leave the component.  From:
        // http://stackoverflow.com/questions/12154734/change-focus-to-next-component-in-jtable-using-tab
        Set<AWTKeyStroke> forward = new HashSet<AWTKeyStroke>(
                getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forward.add(KeyStroke.getKeyStroke("TAB"));
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forward);
        Set<AWTKeyStroke> backward = new HashSet<AWTKeyStroke>(
                getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        backward.add(KeyStroke.getKeyStroke("shift TAB"));
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backward);
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
     * A list of fields that should be shown in this list.
     * 
     * @param listData
     *            The list of fields
     */
    public void setData(List<FieldInfo> listData)
    {
        ((ListTableModel) getModel()).setData(listData);

        setColumnWidths(listData);
        
        revalidate();        
    }

    /**
     * Calculate column widths based on the data.
     */
    private void setColumnWidths(List<FieldInfo> listData)
    {
        int colPrefWidth = preferredWidth/2;

        for (int column = 0; column < 2; column++) {
            TableColumn tableColumn = getColumnModel().getColumn(column);

            int contentsMaxWidth = getMaxWidth(listData, column);
            
            // Make the minimum width look nice. A minimum width is always
            // needed so short columns doesn't get squeezed by long columns.
            if (contentsMaxWidth < colPrefWidth) {
                tableColumn.setMinWidth(contentsMaxWidth);
            }
            else {
                tableColumn.setMinWidth(colPrefWidth);
            }
            
            tableColumn.setPreferredWidth(contentsMaxWidth);
        }
    }

    /**
     * Finds the maximum width among all the elements in the given column in the data.
     */
    private int getMaxWidth(List<FieldInfo> listData, int column)
    {
        TableCellRenderer ltcr = getDefaultRenderer(Object.class);
        TableColumn tableColumn = getColumnModel().getColumn(column);
        int contentsMaxWidth = tableColumn.getPreferredWidth();
        for (int row = 0; row < listData.size(); row++) {
            Component n = ltcr.getTableCellRendererComponent(this, dataModel.getValueAt(row, column),
                    false, false, row, column);
            contentsMaxWidth = Math.max(contentsMaxWidth, n.getPreferredSize().width);
        }
        return contentsMaxWidth;
    }

    /**
     * Ensures that the header of the table is not shown at all!
     */
    private void removeHeader()
    {
        this.unconfigureEnclosingScrollPane();
    }

    static class ListTableModel extends AbstractTableModel
    {
        private String[][] cells;

        public ListTableModel()
        {
            super();
        }

        public ListTableModel(List<FieldInfo> rows)
        {
            setData(rows);
        }
        
        @Override
        public String getValueAt(int row, int col)
        {
            return cells[row][col];
        }

        @Override
        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public int getRowCount()
        {
            if (cells != null) {
                return cells.length;
            }
            else {
                return 0;
            }
        }

        /**
         * Set the field list data.
         */
        public void setData(List<FieldInfo> rows)
        {
            cells = new String[rows.size()][2];
            Iterator<FieldInfo> f = rows.iterator();
            for (int i = 0; i < rows.size(); i++) {
                FieldInfo field = f.next();
                cells[i][0] = field.getDescription();
                cells[i][1] = field.getValue();
            }
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    }

    /**
     * Cell renderer that makes a two column table look like a list.
     * 
     * @author Poul Henriksen
     */
    public static class ListTableCellRenderer extends DefaultTableCellRenderer
    {
        final static private ImageIcon objectrefIcon = Config.getImageAsIcon("image.inspector.objectref");
        final private static Border valueBorder = BorderFactory.createLineBorder(Color.gray);
        private Color bkColor;


        public ListTableCellRenderer(Color bkColor)
        {
            this.bkColor = bkColor;
            this.setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column)
        {
            String valueString = (String) value;

            // It seems the JRE can pass in null in certain situations. Specifically,
            // turning the Voiceover utility on in Mac OS X (10.6.2, Java 1.6.0_17)
            // causes this method to be called with a null value every time the list
            // selection changes.
            if (valueString == null) {
                return this;
            }

            if (valueString.equals(DebuggerObject.OBJECT_REFERENCE)) {
                this.setIcon(objectrefIcon);
                this.setText("");
            }
            else {
                this.setIcon(null);
                this.setText(valueString);
                if (valueString.startsWith("\"")) {
                    this.setToolTipText(valueString);
                }
                else {
                    this.setToolTipText(null);
                }
            }

            Color rowBackground = isSelected ? table.getSelectionBackground() : bkColor;
            this.setBackground(rowBackground);
            
            Border b = BorderFactory.createLineBorder(rowBackground, 3);

            super.setBorder(b);

            // depending in which column we are in, we have to do some different
            // things
            if (column == 1) {
                this.setBackground(new Color(255,255,255));
                this.setHorizontalAlignment(JLabel.CENTER);
                Border compoundBorder = BorderFactory.createCompoundBorder(getBorder(), valueBorder);
                super.setBorder(compoundBorder);
            }
            else {
                this.setHorizontalAlignment(JLabel.LEADING);
            }
            
            getAccessibleContext().setAccessibleName(table.getModel().getValueAt(row, 0) + " = " + table.getModel().getValueAt(row, 1));
            
            return this;
        }
    }
}

