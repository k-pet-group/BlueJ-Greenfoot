/*
 * %W% %E%
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
package javax.swing.beaninfo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyDescriptor;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 * A property editor for a swing enumerated type. Handles the case in which the
 * PropertyDescriptor has a value for "enumerationValues".
 * Note: the init() method must be called before the set/get methods can be
 * called.
 *
 * @version %I% %G%
 * @author  Mark Davidson
 */
public class SwingEnumEditor extends SwingEditorSupport implements ActionListener {

    public JComboBox combobox;

    public void setValue(Object value) {
        super.setValue(value);

        // Set combo box if it's a new value. We want to reduce number
        // of extraneous events.
        EnumeratedItem item = (EnumeratedItem)combobox.getSelectedItem();
        if (value != null && !value.equals(item.getValue()))  {
            for (int i = 0; i < combobox.getItemCount(); ++i ) {
                item = (EnumeratedItem)combobox.getItemAt(i);
                if (item.getValue().equals(value)) {
                    // XXX - hack! Combo box shouldn't call action event
                    // for setSelectedItem!!
                    combobox.removeActionListener(this);
                    combobox.setSelectedItem(item);
                    combobox.addActionListener(this);
                    return;
                }
            }
        }
    }

    /**
     * Initializes this property editor with the enumerated items. Instances
     * can be shared but there are issues.
     * <p>
     * This method does a lot of jiggery pokery since enumerated
     * types are unlike any other homogenous types. Enumerated types may not
     * represent the same set of values.
     * <p>
     * One method would be to empty the list of values which would have the side
     * effect of firing notification events. Another method would be to recreate
     * the combobox.
     */
    public void init(PropertyDescriptor descriptor) {
        Object[] enum = (Object[])descriptor.getValue( "enumerationValues" );
        if (enum != null) {
            if (combobox == null)  {
                combobox = new JComboBox();

                panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                panel.add(combobox);
            } else {
                // Remove action listener to reduce extra events.
                combobox.removeActionListener(this);
                combobox.removeAllItems();
            }

            for ( int i = 0; i < enum.length; i += 3 ) {
                combobox.addItem(new EnumeratedItem((Integer)enum[i+1], (String)enum[i] ) );
            }

            combobox.addActionListener(this);
        }
    }

    /**
     * Event is set when a combo selection changes.
     */
    public void actionPerformed(ActionEvent evt)  {
        EnumeratedItem item = (EnumeratedItem)combobox.getSelectedItem();
        if (item != null && !getValue().equals(item.getValue()))  {
            setValue(item.getValue());
        }
    }

    /**
     * Object which holds an enumerated item plus its label.
     */
    private class EnumeratedItem  {
        private Integer value;
        private String name;

        public EnumeratedItem(Integer value, String name) {
            this.value = value;
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public String toString() {
            return name;
        }
    }
}