/*
 * Copyright 1999 Sun Microsystems, Inc. All Rights Reserved
 */

/*
 * $Id: SwingColorEditor.java 422 2000-04-13 00:20:40Z ajp $
 */

package javax.swing.beaninfo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Rectangle;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JColorChooser;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIDefaults;

import javax.swing.plaf.metal.MetalComboBoxIcon;

/**
 * Swing version of a Color property editor.
 *
 * @version $Revision: 422 $
 * @author  Tom Santos
 * @author  Mark Davidson
 */
public class SwingColorEditor extends SwingEditorSupport { 
    private JTextField rgbValue;
    private JButton    colorChooserButton;
    private Color      color = Color.black;

    private ChooserComboButton	 colorChooserCombo;

    public SwingColorEditor() {
        createComponents();
        addComponentListeners();
    }

    private void addComponentListeners(){
        rgbValue.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
	        try {
    	        setAsText(getAsText());
	        }
	        catch (IllegalArgumentException e2){
	            JOptionPane.showMessageDialog(panel.getParent(), 
					        e2.toString());
	            }
            }
        });

        colorChooserButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
		        color = JColorChooser.showDialog(panel.getParent(), "Color Chooser", color);
		        setValue(color);
            }
        });


    }

    public boolean isPaintable() {
        return true;
    }

    /** 
     * Paints a representation of the value into a given area of screen.
     */
    public void paintValue(Graphics g, Rectangle rect) {
	    Color oldColor = g.getColor();
	    g.setColor(Color.black);
	    g.drawRect(rect.x, rect.y, rect.width-3, rect.height-3);
	    g.setColor(color);
	    g.fillRect(rect.x+1, rect.y+1, rect.width-4, rect.height-4);
	    g.setColor(oldColor);
    }

    private void createComponents(){
        UIDefaults table = UIManager.getDefaults();
        table.put("beaninfo.ColorIcon",  LookAndFeel.makeIcon(getClass(), "icons/ColorIcon.gif"));
        table.put("beaninfo.ColorPressedIcon", LookAndFeel.makeIcon(getClass(), "icons/ColorPressedIcon.gif"));
        Icon colorIcon = UIManager.getIcon("beaninfo.ColorIcon"); 
        Icon colorPressedIcon = UIManager.getIcon("beaninfo.ColorPressedIcon");

        rgbValue = new JTextField();
        colorChooserCombo = new ChooserComboButton();
        colorChooserButton = new JButton(colorIcon);

        Dimension d = new Dimension(colorIcon.getIconWidth(), colorIcon.getIconHeight());
        rgbValue.setPreferredSize(SwingEditorSupport.MEDIUM_DIMENSION); 
        rgbValue.setMaximumSize(SwingEditorSupport.MEDIUM_DIMENSION); 
        rgbValue.setMinimumSize(SwingEditorSupport.MEDIUM_DIMENSION); 
        colorChooserCombo.setPreferredSize(SwingEditorSupport.SMALL_DIMENSION);
        colorChooserCombo.setMaximumSize(SwingEditorSupport.SMALL_DIMENSION);
        colorChooserCombo.setMinimumSize(SwingEditorSupport.SMALL_DIMENSION);

        colorChooserButton.setPressedIcon(colorPressedIcon);
        colorChooserButton.setToolTipText("press to bring up color chooser");
        colorChooserButton.setPreferredSize(d);
        colorChooserButton.setMinimumSize(d);
        colorChooserButton.setMaximumSize(d);
        colorChooserButton.setBorderPainted(false);
        colorChooserButton.setContentAreaFilled(false);

        setAlignment(rgbValue);
        setAlignment(colorChooserCombo);
        setAlignment(colorChooserButton);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
        panel.add(rgbValue);
        panel.add(Box.createRigidArea(new Dimension(5,0)));
        panel.add(colorChooserCombo);
        panel.add(Box.createRigidArea(new Dimension(5,0)));
        panel.add(colorChooserButton);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    }


    public String getJavaInitializationString(){
        return "new java.awt.Color(" + getAsText() + ")";
    }

    public String getAsText(){
        return rgbValue.getText();
    }

    public void setAsText(String s) throws java.lang.IllegalArgumentException {
        int c1 = s.indexOf(',');
        int c2 = s.indexOf(',', c1+1);
        if (c1 < 0 || c2 < 0) {
            // Invalid string.
            throw new IllegalArgumentException(s);
        }
        try {
            int r = Integer.parseInt(s.substring(0,c1));
            int g = Integer.parseInt(s.substring(c1+1, c2));
            int b = Integer.parseInt(s.substring(c2+1));
            setValue(new Color(r,g,b));
        } catch (Exception ex) {
            throw new IllegalArgumentException(s);
        }
    }

    public void setValue(Object value){
	    super.setValue(value);
	    editorChangeValue(value);
    }
  
    public void editorChangeValue(Object value){
        Color c = (Color)value;
        if (c == null) {
            rgbValue.setText("                  ");
            colorChooserCombo.setBackground(panel.getBackground());
            return;
        }
	    color = c;
        // set the combo rect foreground color
        // and the textfield to the rgb value
        rgbValue.setText("" + c.getRed() + "," + c.getGreen() + "," + c.getBlue());
        colorChooserCombo.setBackground(c); 
    }

    public SwingColorEditor getSwingColorEditor(){
        return this;
    }


    // for testing

    // custom combolike rect button
    class ChooserComboButton extends JButton {
        ChooserComboPopup popup;
        Icon comboIcon = new MetalComboBoxIcon();

        public ChooserComboButton(){
        super ("");
        popup = new ChooserComboPopup(getSwingColorEditor());
        addMouseListener(new PopupListener());
        }


        public void paintComponent(Graphics g){
        super.paintComponent( g );
        Insets insets = getInsets();

        int width = getWidth() - (insets.left + insets.right);
        int height = getHeight() - (insets.top + insets.bottom);

        if ( height <= 0 || width <= 0 ) {
	    return;
        }

        int left = insets.left;
        int top = insets.top;
        int right = left + (width - 1);
        int bottom = top + (height - 1);

        int iconWidth = 0;
        int iconLeft = right;

        // Paint the icon
        if ( comboIcon != null ) {
	    iconWidth = comboIcon.getIconWidth();
	    int iconHeight = comboIcon.getIconHeight();
	    int iconTop = 0;
	    iconTop = (top + ((bottom - top) / 2)) - (iconHeight / 2);
	    comboIcon.paintIcon( this, g, iconLeft, iconTop );
        }

        }

        class PopupListener extends MouseAdapter {
            public void mouseReleased(MouseEvent e){
	            // bring up ChooserComboPopup
	            // bring it up at the comopnent height location!
	            JComponent c = (JComponent)e.getComponent();
	            popup.show(c,0,0);  
            }
        }
    }

    public static void main(String argv[]){
        JFrame f = new JFrame();
        f.addWindowListener(new WindowAdapter(){
        public void windowClosing(WindowEvent e){
            JFrame frame = (JFrame)e.getSource();
            frame.dispose();
    	    System.exit(0);
            }
        });

        SwingColorEditor editor = new SwingColorEditor();
        f.getContentPane().add(editor.getCustomEditor());
        f.pack();
        f.show();
    }
}
