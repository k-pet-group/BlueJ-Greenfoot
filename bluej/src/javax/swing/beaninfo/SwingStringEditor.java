/*
 * @(#)SwingStringEditor.java	1.2 99/11/05
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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A property editor for editing strings.
 *
 * @version 1.2 11/05/99
 * @author  Mark Davidson
 */
public class SwingStringEditor extends SwingEditorSupport {
    
    private JTextField textfield;

    public SwingStringEditor() {
        textfield = new JTextField();
        textfield.addKeyListener(new KeyAdapter()  {
            // XXX - JTextfield should send an actionPerformed event.
            // this was broken for 1.3 beta but fixed in 1.3. This
            // is the workaround.
            public void keyPressed(KeyEvent evt)  {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER)  {
                    setValue(textfield.getText());
                }
            }
        });

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(textfield);
    }

    public void setValue(Object value)  {
        super.setValue(value);
        if (value != null)  {
            textfield.setText(value.toString());
        } else {
            textfield.setText("");
        }
    }
}
