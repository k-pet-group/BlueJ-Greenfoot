/*
 * @(#)PropertyValueEditor.java	1.4 99/10/27
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

import java.awt.Component;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;

import java.util.Hashtable;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;

import javax.swing.border.Border;

import javax.swing.table.TableCellEditor;

import javax.swing.beaninfo.SwingEditorSupport;

/**
 * An editor for types which have a property editor.
 * Note: Should consolidate with the PropertyValueRenderer
 */
public class PropertyValueEditor extends AbstractCellEditor implements TableCellEditor,
                                                PropertyChangeListener {

    private PropertyEditor editor;
    private DefaultCellEditor cellEditor;
    private Class type;

    private Border selectedBorder;
    private Border emptyBorder;

    private Hashtable editors;

    public PropertyValueEditor()  {
        editors = new Hashtable();
        cellEditor = new DefaultCellEditor(new JTextField());
    }

    /**
     * Get UI for current editor, including custom editor button
     * if applicable.
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
                            boolean isSelected, int row, int column) {

        PropertyTableModel model = (PropertyTableModel)table.getModel();
        type = model.getPropertyType(row);

        if (type != null)  {
            editor = (PropertyEditor)editors.get(type);
            if (editor == null)  {
                PropertyEditor ed = model.getPropertyEditor(row);

                // Make a copy of this prop editor and register this as a
                // prop change listener.
                // We have to do this since we want a unique PropertyEditor
                // instance to be used for an editor vs. a renderer.
                if (ed != null)  {
                    Class editorClass = ed.getClass();
                    try {
                        editor = (PropertyEditor)editorClass.newInstance();
                        editor.addPropertyChangeListener(this);
                        editors.put(type, editor);

                    } catch (Exception ex) {
                        System.out.println("Couldn't instantiate type editor \"" +
                                                editorClass.getName() + "\" : " + ex);
                    }
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
        return cellEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    /**
     * Get cellEditorValue for current editor
     */
    public Object getCellEditorValue() {
        Object obj = null;

        if (editor != null)  {
            obj = editor.getValue();
        } else {
            obj = cellEditor.getCellEditorValue();
        }

        if (type != null && obj != null &&
            !type.isPrimitive() && !type.isAssignableFrom(obj.getClass()))  {
            // XXX - debug
	        System.out.println("Type mismatch in getCellEditorValue() = " + obj.getClass() + " type = " + type);

            try {
            	obj = type.newInstance();
            } catch (Exception ex) {
            	ex.printStackTrace();
            }
        }
        return obj;
    }

    //
    // Property Change handler.
    //

    public void propertyChange(PropertyChangeEvent evt)  {
        stopCellEditing();
    }
}
