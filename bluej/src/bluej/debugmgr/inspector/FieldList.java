/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.*;
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
     *            Strings that include a "="
     */
    public void setData(Object[] listData)
    {
        ((ListTableModel) getModel()).setDataVector(listData);

        setColumnWidths(listData);
        
        revalidate();        
    }

    /**
     * Calculate column widths based on the data.
     */
    private void setColumnWidths(Object[] listData)
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
    private int getMaxWidth(Object[] listData, int column)
    {
        TableCellRenderer ltcr = getDefaultRenderer(Object.class);
        TableColumn tableColumn = getColumnModel().getColumn(column);
        int contentsMaxWidth = tableColumn.getPreferredWidth();
        for (int row = 0; row < listData.length; row++) {
            Component n = ltcr.getTableCellRendererComponent(this, dataModel.getValueAt(row, column), false, false,
                    row, column);
            int labelWidth = n.getPreferredSize().width;
            if (labelWidth > contentsMaxWidth) {
                contentsMaxWidth = labelWidth;
            }
        }
        return contentsMaxWidth;
    }

    /**
     * Ensures that the header of the table is not shown at all!
     *  
     */
    private void removeHeader()
    {
        this.unconfigureEnclosingScrollPane();
    }

    static class ListTableModel extends AbstractTableModel
    {
        private Object[][] cells;

        public Object getValueAt(int row, int col)
        {
            return cells[row][col];
        }

        public int getColumnCount()
        {
            return 2;
        }

        public int getRowCount()
        {
            if (cells != null)
                return cells.length;
            else
                return 0;
        }

        public ListTableModel()
        {
            super();
        }

        public ListTableModel(Object[] rows)
        {
            setDataVector(rows);
        }

        public void setDataVector(Object[] rows)
        {
            cells = new Object[rows.length][2];
            for (int i = 0; i < rows.length; i++) {
                String s = (String) rows[i];
                String descriptionString;
                String valueString;
                //split on "="
                int delimiterIndex = s.indexOf('=');
                if (delimiterIndex >= 0) {
                    descriptionString = s.substring(0, delimiterIndex);
                    valueString = s.substring(delimiterIndex + 1);

                }
                else {
                    //It was not a "normal" object. We just show the string.
                    //It could be an array compression [...]
                    descriptionString = s;
                    valueString = "";
                }
                cells[i][0] = descriptionString;
                cells[i][1] = valueString;
            }
        }

        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    }

    /**
     * Cell renderer that makes a two column table look like a list.
     * 
     * @author Poul Henriksen
     *  
     */
    public static class ListTableCellRenderer extends DefaultTableCellRenderer
    {
        final static private ImageIcon objectrefIcon = Config.getImageAsIcon("image.inspector.objectref");
        final private static Border valueBorder = BorderFactory.createLineBorder(Color.gray);
        private Color valueColor;


        public ListTableCellRenderer(Color valueColor)
        {
            this.valueColor = valueColor;
            this.setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column)
        {

            String valueString = (String) value;

            if (valueString.equals(" " + DebuggerObject.OBJECT_REFERENCE)) {
                this.setIcon(objectrefIcon);
                this.setText("");
            }
            else {
                // display some control characters in the displayed string in
                // the
                // correct form. This code should probably be somewhere else
                StringBuffer displayString = new StringBuffer(valueString);
                replaceAll(displayString, "\n", "\\n");
                replaceAll(displayString, "\t", "\\t");
                replaceAll(displayString, "\r", "\\r");

                this.setIcon(null);
                this.setText(displayString.toString());
            }

            if (isSelected) {
                this.setBackground(table.getSelectionBackground());
            }
            else {
                this.setBackground(table.getBackground());
            }

            Border b = BorderFactory.createLineBorder(this.getBackground(), 3);

            super.setBorder(b);

            TableColumn tableColumn = table.getColumnModel().getColumn(column);

            // depending in which column we are in, we have to do some different
            // things
            if (column == 1) {
                this.setBackground(valueColor);
                this.setHorizontalAlignment(JLabel.CENTER);
                Border compoundBorder = BorderFactory.createCompoundBorder(getBorder(), valueBorder);
                super.setBorder(compoundBorder);
            }
            else {
                this.setHorizontalAlignment(JLabel.LEADING);
            }
            return this;
        }

        private void replaceAll(StringBuffer sb, String orig, String replacement)
        {
            //The call to toString is not efficient, but this method will not
            // be
            //called that many times anyway, so it doesn't matter that much.
            int location = sb.toString().indexOf(orig);
            while (location != -1) {
                sb.replace(location, location + orig.length(), replacement);
                location = sb.toString().indexOf(orig);
            }
        }
    }
}

