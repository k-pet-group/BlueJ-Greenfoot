package bluej.debugmgr.inspector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
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

    /**
     * Creates a new fieldlist with no data.
     * 
     * @param maxDescriptionLength
     *            The maximum number of characters for the "description" of the
     *            field (the name and modifiers)
     */
    public FieldList(int maxDescriptionLength)
    {
        super(new ListTableModel());
        this.setShowGrid(false);
        this.setRowSelectionAllowed(true);
        this.setColumnSelectionAllowed(false);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setIntercellSpacing(new Dimension());
        this.setDefaultRenderer(Object.class, new ListTableCellRenderer(maxDescriptionLength));

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

        // Calculate preferrred column widths. Use at most the first four
        // rows; the size is also dynamically updated as the cells are
        // displayed.
        TableCellRenderer ltcr = getDefaultRenderer(Object.class);
        for (int column = 0; column < 2; column++) {
            TableColumn tableColumn = getColumnModel().getColumn(column);
            int preferredWidth = tableColumn.getPreferredWidth();
            for (int row = 0; row < 4 && row < listData.length; row++) {
                Component n = ltcr.getTableCellRendererComponent(this, dataModel.getValueAt(row, column), false, false,
                        row, column);
                int labelWidth = n.getPreferredSize().width;
                if (labelWidth > preferredWidth) {
                    preferredWidth = labelWidth;
                }
            }
            tableColumn.setPreferredWidth(preferredWidth);
        }
        revalidate();
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
    public static class ListTableCellRenderer extends JLabel
        implements TableCellRenderer
    {
        final static private ImageIcon objectrefIcon = Config.getImageAsIcon("image.inspector.objectref");
        final private static Border valueBorder = BorderFactory.createLineBorder(Color.gray);

        private int maxDescriptionLength;

        public ListTableCellRenderer(int maxDescriptionLength)
        {
            this.maxDescriptionLength = maxDescriptionLength;
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
            int preferredWidth = tableColumn.getPreferredWidth();
            int labelWidth = this.getPreferredSize().width;
            if (labelWidth > preferredWidth) {
                preferredWidth = labelWidth;
                tableColumn.setPreferredWidth(preferredWidth);
            }

            //depending in which column we are in, we have to do some different
            // stuff
            if (column == 1) {
                this.setBackground(Color.white);
                this.setHorizontalAlignment(JLabel.CENTER);
                Border compoundBorder = BorderFactory.createCompoundBorder(getBorder(), valueBorder);
                super.setBorder(compoundBorder);
            }
            else {
                this.setHorizontalAlignment(JLabel.LEADING);
                //Determine the minimum width
                if (valueString.length() < maxDescriptionLength) {
                    if (preferredWidth > tableColumn.getMinWidth()) {
                        tableColumn.setMinWidth(preferredWidth);
                    }
                }
                else {
                    String tmp = valueString.substring(0, maxDescriptionLength);
                    JLabel dummy = new JLabel(tmp);
                    int minWidth = dummy.getPreferredSize().width;
                    if (tableColumn.getMinWidth() < minWidth) {
                        tableColumn.setMinWidth(minWidth);
                    }
                }
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

