/*
 * %W% %E%
 *
 * Copyheight 1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All heights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package javax.swing.beaninfo;

import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * An editor for editing Rectangle.
 *
 * @version %I% %G%
 * @author  Mark Davidson
 */
public class SwingRectangleEditor extends SwingEditorSupport {
    
    private JTextField xTF;
    private JTextField yTF;
    private JTextField widthTF;
    private JTextField heightTF;

    public SwingRectangleEditor() {
        xTF = new JTextField();
        xTF.setDocument(new NumberDocument());
        yTF = new JTextField();
        yTF.setDocument(new NumberDocument());
        widthTF = new JTextField();
        widthTF.setDocument(new NumberDocument());
        heightTF = new JTextField();
        heightTF.setDocument(new NumberDocument());
        
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel("X: "));
        panel.add(xTF);
        panel.add(new JLabel("Y: "));
        panel.add(yTF);
        panel.add(new JLabel("Width: "));
        panel.add(widthTF);
        panel.add(new JLabel("Height: "));
        panel.add(heightTF);
    }
    
    public void setValue(Object value)  {
        super.setValue(value);
        
        Rectangle rect = (Rectangle)value;
        
        xTF.setText(Integer.toString(rect.x));
        yTF.setText(Integer.toString(rect.y));
        widthTF.setText(Integer.toString(rect.width));
        heightTF.setText(Integer.toString(rect.height));
    }
    
    public Object getValue()  {
        int x = Integer.parseInt(xTF.getText());
        int y = Integer.parseInt(yTF.getText());
        int width = Integer.parseInt(widthTF.getText());
        int height = Integer.parseInt(heightTF.getText());
        
        return new Rectangle(x, y, width, height);
    }

}