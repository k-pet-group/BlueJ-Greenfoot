/*
 * @(#)PropertyColumnModel.java	1.4 99/10/13
 *
 * Copyright 1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
package bluej.guibuilder;

import java.awt.Color;
import java.awt.Component;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;

import javax.swing.border.Border;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import javax.swing.beaninfo.SwingEditorSupport;

/**
 * Column model for the PropertyTable
 *
 * @version 1.4 10/13/99
 * @author  Mark Davidson
 */
public class PropertyColumnModel extends DefaultTableColumnModel  {
    private final static String COL_LABEL_PROP = "Property";
    private final static String COL_LABEL_DESC = "Description";
    private final static String COL_LABEL_VALUE = "Value";

    private static final int minColWidth = 150;

    public PropertyColumnModel()  {
        // Configure the columns and add them to the model
        TableColumn column;

        // Property
        column = new TableColumn(0);
        column.setHeaderValue(COL_LABEL_PROP);
        column.setPreferredWidth(minColWidth);
        column.setCellRenderer(new PropertyNameRenderer());
        addColumn(column);

        // Value
        column = new TableColumn(1);
        column.setHeaderValue(COL_LABEL_VALUE);
        column.setPreferredWidth(minColWidth * 2);
        column.setCellEditor(new PropertyValueEditor());
        column.setCellRenderer(new PropertyValueRenderer());
        addColumn(column);
    }

	/**
	 * Renders the name of the property. Sets the short description of the
     * property as the tooltip text.
	 */
	class PropertyNameRenderer extends DefaultTableCellRenderer  {

        /**
         * Get UI for current editor, including custom editor button
         * if applicable.
         */
        public Component getTableCellRendererComponent(JTable table, Object value,
                                boolean isSelected, boolean hasFocus, int row, int column) {

            PropertyTableModel model = (PropertyTableModel)table.getModel();
            PropertyDescriptor desc = model.getPropertyDescriptor(row);

            setToolTipText(desc.getShortDescription());
            setBackground(UIManager.getColor("control"));

            return super.getTableCellRendererComponent(table, value,
                                    isSelected, hasFocus, row, column);
        }
    }


	/**
	 * Renderer for a value with a property editor or installs the default cell rendererer.
	 */
	class PropertyValueRenderer implements TableCellRenderer  {

    	private DefaultTableCellRenderer renderer;
        private PropertyEditor editor;

        private Hashtable editors;
        private Class type;

        private Border selectedBorder;
        private Border emptyBorder;

        public PropertyValueRenderer()  {
        	renderer = new DefaultTableCellRenderer();
            editors = new Hashtable();
        }

        /**
         * Get UI for current editor, including custom editor button
         * if applicable.
         * XXX - yuck! yuck! yuck!!!!
         */
        public Component getTableCellRendererComponent(JTable table, Object value,
                                boolean isSelected, boolean hasFocus, int row, int column) {

            PropertyTableModel model = (PropertyTableModel)table.getModel();
            type = model.getPropertyType(row);
            if (type != null)  {
                editor = (PropertyEditor)editors.get(type);
                if (editor == null)  {
                    editor = model.getPropertyEditor(row);

                    if (editor != null)  {
                        editors.put(type, editor);
                    }
                }
            } else {
                editor = null;
            }

            if (editor != null)  {
                // Special case for the enumerated properties. Must reinitialize
                // to reset the combo box values.
                if (editor instanceof SwingEditorSupport)  {
                    ((SwingEditorSupport)editor).init(model.getPropertyDescriptor(row));
                }

                editor.setValue(value);

                Component comp = editor.getCustomEditor();
                if (comp != null)  {
                    comp.setEnabled(isSelected);

                    if (comp instanceof JComponent)  {
                        if (isSelected)  {
                            if (selectedBorder == null)
                                selectedBorder = BorderFactory.createLineBorder(table.getSelectionBackground(), 1);

                            ((JComponent)comp).setBorder(selectedBorder);
                        } else {
                            if (emptyBorder == null)
                                emptyBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);

                            ((JComponent)comp).setBorder(emptyBorder);
                        }
                    }
                    return comp;
                }
            }
            return renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        /**
         * Retrieves the property editor for this value.
         */
        public PropertyEditor getPropertyEditor()  {
            return editor;
        }
	}

}
