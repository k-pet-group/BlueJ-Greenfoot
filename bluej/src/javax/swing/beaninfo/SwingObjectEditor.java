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
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import java.beans.Beans;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;

/**
 * A property editor which allows for the display and instantaition of
 * arbitrary objects. To instantiate the object, type the package and the
 * class in the text field and press Enter. The Class should be in the 
 * classpath.
 *
 * @version %I% %G%
 * @author  Mark Davidson
 */
public class SwingObjectEditor extends SwingEditorSupport {
    
    private JTextField textfield;

    public SwingObjectEditor() {
        textfield = new JTextField();

        // NOTE: This should work but there was a regression in JDK 1.3 beta which
        // doesn't fire for text fields.
        // This should be fixed for RC 1.
        textfield.addActionListener(new ActionListener()  {
            public void actionPerformed(ActionEvent evt)  {
                // XXX - debug
                System.out.println("SwingObjectEditor.actionPerformed");
                handleAction();
            }
        });

        // XXX - Temporary workaround for 1.3 beta
        textfield.addKeyListener(new KeyAdapter()  {
            public void keyPressed(KeyEvent evt)  {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER)  {
                    handleAction();
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
            // Truncate the address from the object reference.
            String text = value.toString();
            
            // XXX javax.swing.AccessibleRelationSet.toString() has a bug in which
            // null is returned. Intecept this and other cases so that the tool
            // doens't get hosed.
            if (text == null) text = "";
            
            int index = text.indexOf('@');
            if (index != -1)  {
                text = text.substring(0, index);
            }
            textfield.setText(text);
        } else {
            textfield.setText("");
        }
    }

    /** 
     * Callback method which gets handled for actionPerformed.
     */
    private void handleAction()  {
        String beanText = textfield.getText();

        try {
            Object obj = Beans.instantiate(this.getClass().getClassLoader(), beanText);
            setValue(obj);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel, "Can't find or load\n" + beanText);
        }
    }

}
