/*
 * @(#)SwingIntegerEditor.java	1.9 99/11/11
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

import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import java.beans.PropertyDescriptor;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A property editor for editing integers. This editor also supports enumerated
 * type properties which are identified if the "enumerationValues" key returns
 * a non-null value.
 * Note: the init() method must be called before the set/get methods can be
 * called.
 *
 * @version 1.9 11/11/99
 * @author  Mark Davidson
 */
public class SwingIntegerEditor extends SwingEditorSupport {

    // Property editor to use if the Integer represents an Enumerated type.
    private SwingEnumEditor enumEditor = new SwingEnumEditor();

    private JTextField textfield;

    private boolean isEnumeration = false;

    public void setValue(Object value) {
        if (isEnumeration)  {
            enumEditor.setValue(value);
        } else {
            super.setValue(value);
            
            if (value != null)  {
                textfield.setText(value.toString());
            } 
        }
    }

    public Object getValue() {
        if (isEnumeration)  {
            return enumEditor.getValue();
        } else {
            return super.getValue();
        }
    }
    
    /** 
     * Must overloade the PropertyChangeListener registration because
     * this class is the only interface to the SwingEnumEditor.
     */
    public void addPropertyChangeListener(PropertyChangeListener l)  {
        enumEditor.addPropertyChangeListener(l);
        super.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)  {
        enumEditor.removePropertyChangeListener(l);
        super.removePropertyChangeListener(l);
    }

    /**
     * Initializes this property editor with the enumerated items.
     */
    public void init(PropertyDescriptor descriptor) {
        Object[] enum = (Object[])descriptor.getValue("enumerationValues");
        if ( enum != null ) {
            // The property descriptor describes an enumerated item.
            isEnumeration = true;

            enumEditor.init(descriptor);

        } else {
            // This is an integer item
            isEnumeration = false;

            if (textfield == null)  {
                textfield = new JTextField();
                textfield.setDocument(new NumberDocument());
                // XXX - Textfield should sent an actionPerformed event.
                // this was broken for 1.3 beta
                textfield.addKeyListener(new KeyAdapter()  {
                    public void keyPressed(KeyEvent evt)  {
                        if (evt.getKeyCode() == KeyEvent.VK_ENTER)  {
                            setValue(new Integer(textfield.getText()));
                        }
                    }
                });
                panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
                panel.add(textfield);
            }
        }
    }

    /**
     * Return the custom editor for the enumeration or the integer.
     */
    public Component getCustomEditor()  {
        if (isEnumeration)  {
            return enumEditor.getCustomEditor();
        } else {
            return super.getCustomEditor();
        }
    }
}
