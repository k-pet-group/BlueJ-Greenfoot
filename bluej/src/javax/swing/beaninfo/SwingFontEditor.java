/*
 * @(#)SwingFontEditor.java	1.3 99/10/27
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * A Font property editor. Mostly designed by Chris Ryan.
 *
 * @version 1.3 10/27/99
 * @author Tom Santos
 * @author Mark Davidson
 */
public class SwingFontEditor extends SwingEditorSupport implements ActionListener {

    private static int BUTTON_WIDTH = 20;
    private static int BUTTON_HEIGHT = 30;
    
    private static Dimension buttonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
    
    private String fonts[];
    private static int[] pointSizes = { 3, 5, 8, 10, 12, 14, 18, 24, 36, 48 };
    
    private int selectedStyle = Font.PLAIN;
    private final static String sampleText = "Abcde...";

    // Controls 
    private JComboBox familyNameCombo;
    private JComboBox fontSizeCombo;

    private JToggleButton pButton, iButton, bButton;
    private FontDisplay iDisplay, pDisplay, bDisplay;
    
    private JLabel labelDisplay;
	
    public SwingFontEditor() {
        fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    
        pDisplay = new FontDisplay(Font.PLAIN);
        pButton = new JToggleButton(pDisplay);
        pButton.setToolTipText("plain style");

        iDisplay = new FontDisplay(Font.ITALIC);
        iButton = new JToggleButton(iDisplay);
        iButton.setToolTipText("italic style");

        bDisplay = new FontDisplay(Font.BOLD);
        bButton = new JToggleButton(bDisplay);
        bButton.setToolTipText("bold style");

        initializeButton(pButton);
        initializeButton(iButton);
        initializeButton(bButton);

        ButtonGroup group = new ButtonGroup();
        group.add(pButton);
        group.add(iButton);
        group.add(bButton);
    
        // ComboBoxes
        familyNameCombo = new JComboBox();
        fontSizeCombo = new JComboBox();
    
        labelDisplay = new JLabel(fonts[0]);
        labelDisplay.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelDisplay.setPreferredSize(new Dimension(250,30));
        labelDisplay.setMinimumSize(new Dimension(250,30));

        initializeComboBoxes();

		// Assemble the panel.
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
        p.add(familyNameCombo);
        p.add(Box.createRigidArea(new Dimension(5,0)));    
        p.add(fontSizeCombo);
        p.add(Box.createRigidArea(new Dimension(5,0)));    

        p.add(pButton);
        p.add(iButton);
        p.add(bButton);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
    
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(p);
        panel.add(labelDisplay);
    }

    private void initializeButton(JToggleButton b) {
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        
        b.setPreferredSize(buttonSize);
        b.setMaximumSize(buttonSize);
        b.setMinimumSize(buttonSize);
        b.addActionListener(this);
        setAlignment(b);
    }

    private void initializeComboBoxes() {
        for (int i = 0; i < fonts.length; i++)
            familyNameCombo.addItem(fonts[i]);

        familyNameCombo.setPreferredSize(SwingEditorSupport.MEDIUM_DIMENSION);
        familyNameCombo.setMinimumSize(SwingEditorSupport.MEDIUM_DIMENSION);
        familyNameCombo.setMaximumSize(SwingEditorSupport.MEDIUM_DIMENSION);
        familyNameCombo.addActionListener(this);
        setAlignment(familyNameCombo);
    
        for (int i = 0; i < pointSizes.length; i++)
            fontSizeCombo.addItem("" + pointSizes[i]);
        
        fontSizeCombo.setPreferredSize(SwingEditorSupport.SMALL_DIMENSION);
        fontSizeCombo.setMaximumSize(SwingEditorSupport.SMALL_DIMENSION);
        fontSizeCombo.setMinimumSize(SwingEditorSupport.SMALL_DIMENSION);
        fontSizeCombo.addActionListener(this);
        setAlignment(fontSizeCombo);

    }
    
    /** 
     * ActionListener handler for all component events.
     */
    public void actionPerformed(ActionEvent evt)  {
        Object obj = evt.getSource();

        if (obj instanceof AbstractButton) {
            AbstractButton button = (AbstractButton)obj;
	        if (obj == pButton)  {
                selectedStyle = Font.PLAIN;
	        } else if (obj == iButton) {
	            selectedStyle = Font.ITALIC;
	        } else if (obj == bButton) {
	            selectedStyle = Font.BOLD;
	        }

	        String family = (String)familyNameCombo.getSelectedItem();
	        int size = pointSizes[fontSizeCombo.getSelectedIndex()];
            
	        setValue(new Font(family, selectedStyle, size));
        }

        if (obj instanceof JComboBox)  {
            String family = (String)familyNameCombo.getSelectedItem();
            int size = pointSizes[fontSizeCombo.getSelectedIndex()];
            
            setValue(new Font(family, selectedStyle, size)); 
        }
    }

    /** 
     * Reconfigure the controls to reflect the current font.
     */
    private void editorChangeValue(Font font){
        for (int i = 0; i < fonts.length; i++) {
            if (fonts[i].equals(font.getName())) {
	            familyNameCombo.setSelectedIndex(i);
        	    break;
            }
        }

        for (int i = 0; i < pointSizes.length; i++) {
            if (font.getSize() <= pointSizes[i]){
	            fontSizeCombo.setSelectedIndex(i);
	            break;
            }
        }

        selectedStyle = font.getStyle();
        String style = "";
        switch (selectedStyle) {
            case Font.PLAIN:
                pButton.setSelected(true);
                style = "Plain";
                break;
            case Font.ITALIC:
                iButton.setSelected(true);
                style = "Italic";
                break;
            case Font.BOLD:
                bButton.setSelected(true);
                style = "Bold";
                break;
        }
        String family = font.getFamily();
        
        iDisplay.setFamily(family);
        pDisplay.setFamily(family);
        bDisplay.setFamily(family);
        
        labelDisplay.setFont(font);
        labelDisplay.setText(family + ", " + style + ", " + font.getSize());

        panel.revalidate();
        panel.repaint();
    }

    //
    // PropertyEditor interface definitions
    //

    public void setValue(Object value) {
    	super.setValue(value);
	    editorChangeValue((Font)value);
    }

    public boolean isPaintable() {
    	return true;
    }

    public void paintValue(Graphics g, Rectangle rect) {
	    // Silent noop.
	    Font oldFont = g.getFont();
	    g.setFont((Font)getValue());
	    FontMetrics fm = g.getFontMetrics();
	    int vpad = (rect.height - fm.getAscent())/2;
	    g.drawString(sampleText, 0, rect.height-vpad);
	    g.setFont(oldFont);
    }

    public String getJavaInitializationString() {
    	Font font = (Font)getValue();
    
        return "new java.awt.Font(\"" + font.getFamily() + "\", " +
        font.getStyle() + ", " + font.getSize() + ")";
    }

    /** 
     * Implementation of a Icon button.
     */
    private class FontDisplay implements Icon {
        private Font font;
        
        private int style = Font.PLAIN;
        private int size = 24;
        
        private String label = "A";

        private int iconWidth = 20;
        private int iconHeight = 30;

        public FontDisplay(int style) {
            this.style = style;
            this.font = new Font("Dialog", this.style, this.size);
        }

        public FontDisplay() {
            this.font = new Font("Dialog", this.style, this.size);
        }

        public void setFamily(String family) {
            this.font = new Font(family, this.style, this.size);
        }
		
        public void paintIcon(Component c, Graphics g, int x, int y) {
            JComponent component = (JComponent)c;
            
            Font oldFont = g.getFont();
            g.setFont(this.font);
            if (component instanceof JToggleButton) {
	            AbstractButton b= (AbstractButton)component;
	            ButtonModel model = b.getModel();
	            if (model.isPressed() || model.isSelected())
    	            g.setColor(Color.black); // xxx: foreground
	            else
	                g.setColor(Color.gray); // xxx: foreground light
            }
            g.drawString(label, x, (y + iconHeight) - 7);
            g.setFont(oldFont);
        }

        public int getIconWidth() {
            return iconWidth;
        }
        
        public int getIconHeight() {
            return iconHeight;
        }
    } // end class FontDisplay
}
