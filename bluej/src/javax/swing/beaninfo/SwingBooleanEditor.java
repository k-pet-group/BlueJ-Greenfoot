/*
 * @(#)SwingBooleanEditor.java	1.1 99/09/23
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * An editor which represents a boolean value. This editor is implemented
 * as a checkbox with the text of the check box reflecting the state of the
 * checkbox.
 *
 * @version 1.1 09/23/99
 * @author  Mark Davidson
 */
public class SwingBooleanEditor extends SwingEditorSupport {
    
    private JCheckBox checkbox;

    public SwingBooleanEditor() {
        checkbox = new JCheckBox();
        checkbox.addItemListener(new ItemListener()  {
            public void itemStateChanged(ItemEvent evt)  {
                if (evt.getStateChange() == ItemEvent.SELECTED)  {
                    setValue(Boolean.TRUE);
                } else {
                    setValue(Boolean.FALSE);
                }
            }
        });
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(checkbox);
    }

    public void setValue(Object value ) {
        super.setValue(value);
        if (value != null)  {
            try {
                checkbox.setText(value.toString());
                if (checkbox.isSelected() != ((Boolean)value).booleanValue()) {
                    // Don't call setSelected unless the state actually changes
                    // to avoid a loop.
                    checkbox.setSelected(((Boolean)value).booleanValue());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public Object getValue()  {
        return new Boolean(checkbox.isSelected());
    }
}
