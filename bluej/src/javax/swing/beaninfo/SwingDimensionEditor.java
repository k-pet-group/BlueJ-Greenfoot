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

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A PropertyEditor for editing a Dimension object.
 *
 * @version %I% %G%
 * @author  Mark Davidson
 */
public class SwingDimensionEditor extends SwingEditorSupport {

    private JTextField widthTF;
    private JTextField heightTF;
    
    public SwingDimensionEditor() {
        widthTF = new JTextField();
        widthTF.setDocument(new NumberDocument());
        heightTF = new JTextField();
        heightTF.setDocument(new NumberDocument());
        
        JLabel wlabel = new JLabel("Width: ");
        JLabel hlabel = new JLabel("Height: ");
        
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(wlabel);
        panel.add(widthTF);
        panel.add(hlabel);
        panel.add(heightTF);
    }
    
    public void setValue(Object value)  {
        super.setValue(value);
        
        Dimension dim = (Dimension)value;
        
        widthTF.setText(Integer.toString(dim.width));
        heightTF.setText(Integer.toString(dim.height));
    }
    
    public Object getValue()  {
        int width = Integer.parseInt(widthTF.getText());
        int height = Integer.parseInt(heightTF.getText());
        
        return new Dimension(width, height);
    }
}