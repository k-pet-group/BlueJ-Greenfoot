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

import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * An editor for editing Insets.
 *
 * @version %I% %G%
 * @author  Mark Davidson
 */
public class SwingInsetsEditor extends SwingEditorSupport {
    
    private JTextField topTF;
    private JTextField leftTF;
    private JTextField bottomTF;
    private JTextField rightTF;

    public SwingInsetsEditor() {
        topTF = new JTextField();
        topTF.setDocument(new NumberDocument());
        leftTF = new JTextField();
        leftTF.setDocument(new NumberDocument());
        bottomTF = new JTextField();
        bottomTF.setDocument(new NumberDocument());
        rightTF = new JTextField();
        rightTF.setDocument(new NumberDocument());
        
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("Top: "));
        panel.add(topTF);
        panel.add(new JLabel("Left: "));
        panel.add(leftTF);
        panel.add(new JLabel("Bottom: "));
        panel.add(bottomTF);
        panel.add(new JLabel("Right: "));
        panel.add(rightTF);
    }
    
    public void setValue(Object value)  {
        super.setValue(value);
        
        Insets insets = (Insets)value;
        
        topTF.setText(Integer.toString(insets.top));
        leftTF.setText(Integer.toString(insets.left));
        bottomTF.setText(Integer.toString(insets.bottom));
        rightTF.setText(Integer.toString(insets.right));
    }
    
    public Object getValue()  {
        int top = Integer.parseInt(topTF.getText());
        int left = Integer.parseInt(leftTF.getText());
        int bottom = Integer.parseInt(bottomTF.getText());
        int right = Integer.parseInt(rightTF.getText());
        
        return new Insets(top, left, bottom, right);
    }

}