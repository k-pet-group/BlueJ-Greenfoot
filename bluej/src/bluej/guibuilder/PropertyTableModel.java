/*
 * @(#)PropertyTableModel.java	1.9 99/11/09
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

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.beans.FeatureDescriptor;


import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.Arrays;    // Collections class for sorting Since JDK 1.2
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Comparator;

import javax.swing.table.AbstractTableModel;

/**
 * Table model used to obtain property names and values. This model encapsulates an array
 * of PropertyDescriptors.
 *
 * @version 1.9 11/09/99
 * @author  Mark Davidson
 */
public class PropertyTableModel extends AbstractTableModel
{

    private PropertyDescriptor[] descriptors;
    private BeanDescriptor beanDescriptor;
    private BeanInfo info;
    private Object bean;

    // Cached property editors.
    private Hashtable propEditors;

    // Shared instance of a comparator
    private static DescriptorComparator comparator = new DescriptorComparator();


    private static final int NUM_COLUMNS = 2;

    public static final int COL_NAME = 0;
    public static final int COL_VALUE = 1;

    // View options
    public static final int VIEW_ALL       = 0;
    public static final int VIEW_STANDARD  = 1;
    public static final int VIEW_EXPERT    = 2;
    public static final int VIEW_READ_ONLY = 3;
    public static final int VIEW_BOUND     = 4;
    public static final int VIEW_CONSTRAINED = 5;
    public static final int VIEW_HIDDEN    = 6;
    public static final int VIEW_PREFERRED = 7;

    // Sort options
    public static final int SORT_DEF   = 0;
    public static final int SORT_NAME  = 1;
    public static final int SORT_TYPE  = 2;

    public PropertyTableModel()  {
        propEditors = new Hashtable();
    }

    public PropertyTableModel(Object bean)  {
        this();
        setObject(bean);
    }

    /**
     * Set the table model to represents the properties of the object.
     */
    public void setObject(Object bean)  {
        this.bean = bean;

        info = BeanInfoFactory.getBeanInfo(bean.getClass());

        if (info != null)  {
            descriptors = info.getPropertyDescriptors();
            beanDescriptor = info.getBeanDescriptor();
            sortTable(SORT_NAME);
        }

    }

    /**
     * Return the current object that is represented by this model.
     */
    public Object getObject()  {
        return bean;
    }

    /**
     * Get row count (total number of properties shown)
     */
    public int getRowCount() {
        if (descriptors == null)  {
            return 0;
        }

        return descriptors.length;
    }

    /**
     * Get column count (2: name, value)
     */
    public int getColumnCount() {
        return NUM_COLUMNS;
    }

    /**
     * Check if given cell is editable
     * @param row table row
     * @param col table column
     */
    public boolean isCellEditable(int row, int col) {
        if (col == COL_VALUE && descriptors != null) {
            return (descriptors[row].getWriteMethod() == null) ? false : true;
        } else {
            return false;
        }
    }

    /**
     * Get text value for cell of table
     * @param row table row
     * @param col table column
     */
    public Object getValueAt(int row, int col) {

        Object value = null;

        if (col == COL_NAME)  {
            value = descriptors[row].getDisplayName();
        } else {
            // COL_VALUE is handled
            Method getter = descriptors[row].getReadMethod();

            if (getter != null)  {
                Class[] paramTypes = getter.getParameterTypes();
                Object[] args = new Object[paramTypes.length];

                try {
                    for (int i = 0; i < paramTypes.length; i++) {
                        // XXX - debug
                        System.out.println("\tShouldn't happen! getValueAt getter = " + getter + " parameter = " + paramTypes[i]);
                        args[i] = paramTypes[i].newInstance();
                    }
                } catch (Exception ex) {
                    // XXX - handle better
                    ex.printStackTrace();
                }

                try {
                    value = getter.invoke(bean, args);
                } catch (IllegalArgumentException ex) {
                    // XXX - handle better
                    ex.printStackTrace();
                } catch (IllegalAccessException ex2) {
                    // XXX - handle better
                    ex2.printStackTrace();
                } catch (InvocationTargetException ex3) {
                    // XXX - handle better
                    ex3.printStackTrace();
                }
            }

        }
        return value;
    }

    /**
     * Set the value of the Values column.
     */
    public void setValueAt(Object value, int row, int column)  {

        if (column != COL_VALUE || descriptors == null
            || row > descriptors.length)  {
            return;
        }

        Method setter = descriptors[row].getWriteMethod();
        if (setter != null)  {
            try {
                setter.invoke(bean, new Object[] { value });
            } catch (IllegalArgumentException ex) {
                // XXX - handle better
                System.out.println("Setter: " + setter + "\nArgument: " + value.getClass().toString());
                System.out.println("Row: " + row + " Column: " + column);
                ex.printStackTrace();
                System.out.println("\n");
            } catch (IllegalAccessException ex2) {
                // XXX - handle better
                System.out.println("Setter: " + setter + "\nArgument: " + value.getClass().toString());
                System.out.println("Row: " + row + " Column: " + column);
                ex2.printStackTrace();
                System.out.println("\n");
            } catch (InvocationTargetException ex3) {
                // XXX - handle better
                System.out.println("Setter: " + setter + "\nArgument: " + value.getClass().toString());
                System.out.println("Row: " + row + " Column: " + column);
                ex3.printStackTrace();
                System.out.println("\n");
            }
        }
    }

    /**
     * Returns the Java type info for the property at the given row.
     */
    public Class getPropertyType(int row)  {
        return descriptors[row].getPropertyType();
    }

    /**
     * Returns the PropertyDescriptor for the row.
     */
    public PropertyDescriptor getPropertyDescriptor(int row)  {
        return descriptors[row];
    }

    /**
     * Returns a new instance of the property editor for a given class. If an
     * editor is not specified in the property descriptor then it is looked up
     * in the PropertyEditorManager.
     */
    public PropertyEditor getPropertyEditor(int row)  {
        Class cls = descriptors[row].getPropertyEditorClass();

        PropertyEditor editor = null;

        if (cls != null)  {
            try {
                editor = (PropertyEditor)cls.newInstance();
            } catch (Exception ex) {
                // XXX - debug
                System.out.println("PropertyTableModel: Instantiation exception creating PropertyEditor");
            }
        } else {
            // Look for a registered editor for this type.
            Class type = getPropertyType(row);
            if (type != null)  {
                editor = (PropertyEditor)propEditors.get(type);

                if (editor == null)  {
                    // Load a shared instance of the property editor.
                    editor = PropertyEditorManager.findEditor(type);
                    if (editor != null)
                        propEditors.put(type, editor);
                }

                if (editor == null)  {
                    // Use the editor for Object.class
                    editor = (PropertyEditor)propEditors.get(Object.class);
                    if (editor == null)  {
                        editor = PropertyEditorManager.findEditor(Object.class);
                        if (editor != null)
                            propEditors.put(Object.class, editor);
                    }

                }
            }
        }
        return editor;
    }

    /**
     * Returns a flag indicating if the encapsulated object has a customizer.
     */
    public boolean hasCustomizer()  {
        if (beanDescriptor != null)  {
            Class cls = beanDescriptor.getCustomizerClass();
            return (cls != null);
        }

        return false;
    }

    /**
     * Gets the customizer for the current object.
     * @return New instance of the customizer or null if there isn't a customizer.
     */
    public Component getCustomizer()  {
        Component customizer = null;

        if (beanDescriptor != null)  {
            Class cls = beanDescriptor.getCustomizerClass();

            if (cls != null)  {
                try {
                    customizer = (Component)cls.newInstance();
                } catch (Exception ex) {
                    // XXX - debug
                    System.out.println("PropertyTableModel: Instantiation exception creating Customizer");
                }
            }
        }

        return customizer;
    }

    /**
     * Sorts the table according to the sort type.
     */
    public void sortTable(int sort)  {
        if (sort == SORT_DEF || descriptors == null)
            return;

        if (sort == SORT_NAME)  {
            Arrays.sort(descriptors, comparator);
        } else {
            Arrays.sort(descriptors, comparator);
        }
        fireTableDataChanged();
    }

    /**
     * Filters the table to display only properties with specific attributes.
     * @param view The properties to display.
     */
    public void filterTable(int view)  {
        if (info == null)
            return;

        descriptors = info.getPropertyDescriptors();

        // Use collections to filter out unwanted properties
        ArrayList list = new ArrayList();
        list.addAll(Arrays.asList(descriptors));

        ListIterator iterator = list.listIterator();
        PropertyDescriptor desc;
        while (iterator.hasNext()) {
            desc = (PropertyDescriptor)iterator.next();

            switch (view) {
                case VIEW_ALL:
                    if (desc.isHidden())  {
                        iterator.remove();
                    }
                    break;
                case VIEW_STANDARD:
                    if (desc.getWriteMethod() == null || desc.isExpert() || desc.isHidden()) {
                        iterator.remove();
                    }
                    break;
                case VIEW_EXPERT:
                    if (desc.getWriteMethod() == null || !desc.isExpert() || desc.isHidden()) {
                        iterator.remove();
                    }
                    break;
                case VIEW_READ_ONLY:
                    if (desc.getWriteMethod() != null || desc.isHidden()) {
                        iterator.remove();
                    }
                    break;
                case VIEW_HIDDEN:
                    if (!desc.isHidden()) {
                        iterator.remove();
                    }
                    break;
                case VIEW_BOUND:
                    if (!desc.isBound() || desc.isHidden()) {
                        iterator.remove();
                    }
                    break;
                case VIEW_CONSTRAINED:
                    if (!desc.isConstrained() || desc.isHidden()) {
                        iterator.remove();
                    }
                    break;
                case VIEW_PREFERRED:
                    if (!desc.isPreferred() || desc.isHidden()) {
                        iterator.remove();
                    }
                    break;
            }
        }
        descriptors = (PropertyDescriptor[])list.toArray(new PropertyDescriptor[list.size()]);
        sortTable(SORT_NAME);
    }

}

/**
 * Comparator used to compare java.beans.FeatureDescriptor objects.
 * The Strings returned from getDisplayName are used in the comparison.
 *
 * @version 1.1 09/23/99
 * @author  Mark Davidson
 */
class DescriptorComparator implements Comparator
{
    /**
     * Compares two FeatureDescriptor objects
     */
    public int compare(Object o1, Object o2)  {
        FeatureDescriptor f1 = (FeatureDescriptor)o1;
        FeatureDescriptor f2 = (FeatureDescriptor)o2;

        return f1.getDisplayName().compareTo(f2.getDisplayName());
    }
}
