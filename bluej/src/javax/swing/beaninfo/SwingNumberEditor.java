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

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * An PropertyEdtitor for editing numbers.
 *
 * @version %I% %G%
 * @author  Mark Davidson
 */
public class SwingNumberEditor extends SwingEditorSupport {

    private JTextField textfield;

    public SwingNumberEditor() {
        textfield = new JTextField();
        textfield.setDocument(new NumberDocument());
        // XXX - testing
        textfield.addKeyListener(new KeyAdapter()  {
            public void keyPressed(KeyEvent evt)  {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER)  {
                    // XXX - debug
                    System.out.println("SwingNumberEditor.keyEvent: " + textfield.getText());
                    setValue(new Float(textfield.getText()));
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
        }
    }

}