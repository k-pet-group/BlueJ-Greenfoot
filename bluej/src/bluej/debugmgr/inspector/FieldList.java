/*
 * Created on May 18, 2004
 */
package bluej.debugmgr.inspector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import bluej.Config;
import bluej.debugger.DebuggerObject;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 *
 */
public class FieldList extends JTable
{
    
    /**
     * 
     */
    public FieldList(int maxDescriptionLength) {
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
        
        
        //TODO make some experiments with a mehtod that does something like this:
        //this.configureEnclosingScrollPane()
        //this.unconfigureEnclosingScrollPane()
    }
    
    
    public void setData(Object[] listData) {
        ((ListTableModel) getModel()).setDataVector(listData);
    }

    
    
    static class ListTableModel extends DefaultTableModel {
        public ListTableModel() {
            super();
        }
        
        public ListTableModel(Object[] rows) {
            setDataVector(rows);
        }
        
        public void setDataVector(Object[] rows) {
            Object[][] cells = new Object[rows.length][2];
            for (int i = 0; i < rows.length; i++) {
                String s = (String) rows[i];
                String descriptionString;
                String valueString;
                //split on "="
                int delimiterIndex = s.indexOf('=');
                if (delimiterIndex >= 0) {
                    descriptionString = s.substring(0, delimiterIndex);
                    valueString = s.substring(delimiterIndex + 1);
                    
                } else {
                    //It was not a "normal" object. We just show the string.
                    //It could be an array compression [...]
                    descriptionString = s;
                    valueString = "";
                }
                cells[i][0] = descriptionString; 
                cells[i][1] = valueString;               
            }
            this.setDataVector(cells, new Object[] { "", "" });
        }
        
        public boolean isCellEditable(int row, int column) {
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
        
        public ListTableCellRenderer(int maxDescriptionLength) {
            this.maxDescriptionLength = maxDescriptionLength;
            this.setOpaque(true);            
        }       
      
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {           
            
            String valueString = (String) value; 
            
            if(valueString.equals(" " + DebuggerObject.OBJECT_REFERENCE)) {                                
                this.setIcon(objectrefIcon);
                this.setText("");
            } else {
                // display some control characters in the displayed string in the
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
            } else {
                this.setBackground(table.getBackground());
            }
            
            Border b = BorderFactory.createLineBorder(this.getBackground(), 3);
            
            super.setBorder(b);  
            
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            int preferredWidth = tableColumn.getPreferredWidth();
            int labelWidth = this.getPreferredSize().width;                       
            if(labelWidth > preferredWidth) {
                preferredWidth = labelWidth;
                tableColumn.setPreferredWidth(preferredWidth);
            }        
            
            //depending in which column we are in, we have to do some different stuff
            if(column==1) {
                this.setBackground(Color.white);                
                this.setHorizontalAlignment(JLabel.CENTER);
                Border compoundBorder = BorderFactory.createCompoundBorder(getBorder(),valueBorder);
                super.setBorder(compoundBorder);                
            } else {
                this.setHorizontalAlignment(JLabel.LEADING);
                //Determine the minimum width
                if(valueString.length() < maxDescriptionLength) { 
                    if(preferredWidth > tableColumn.getMinWidth()) {
                        tableColumn.setMinWidth(preferredWidth);
                    }
                } else {
                    String tmp = valueString.substring(0, maxDescriptionLength);
                    JLabel dummy = new JLabel(tmp);
                    int minWidth = dummy.getPreferredSize().width;
                    if(tableColumn.getMinWidth() < minWidth) {
                        tableColumn.setMinWidth(minWidth);
                    }
                }
            }           
            return this;
        }

        private void replaceAll(StringBuffer sb, String orig, String replacement)
        {
            //The call to toString is not efficient, but this method will not be 
            //called that many times anyway, so it doesn't matter that much.
            int location = sb.toString().indexOf(orig);
            while(location != -1) {
                sb.replace(location, location+orig.length(), replacement);
                location = sb.toString().indexOf(orig);
            }
        }
    }
    
}


