package bluej.guibuilder;

import java.awt.*;
import java.awt.event.*;

import bluej.guibuilder.graphics.ColorCanvas;
import bluej.guibuilder.graphics.IntegerField;


/**
 * ColorDialog.java
 * A Class for showing a Dialog to edit a color.
 *
 * Created: Wed Sep  2 11:19:22 1998
 *
 * @author Morten Knudsen
 * @version 1.0
 */
public class ColorDialog extends Dialog {
    private GridBagLayout gb = new GridBagLayout();
    
    private Panel p = new Panel();
    
    private Choice chName = new Choice();
    private Button okButton = new Button("OK");
    private Button cancelButton = new Button("Cancel");
    private ColorCanvas colorCanvas = new ColorCanvas();
    private Button thecolor = new Button();
    private Choice systemColorsChoice = new Choice();

    
    private Color[]  systemColors = { SystemColor.activeCaption,SystemColor.activeCaptionBorder,SystemColor.activeCaptionText, SystemColor.control, SystemColor.controlDkShadow, SystemColor.controlHighlight, SystemColor.controlLtHighlight, SystemColor.controlShadow, SystemColor.controlText, SystemColor.desktop, SystemColor.inactiveCaption, SystemColor.inactiveCaptionBorder, SystemColor.inactiveCaptionText, SystemColor.info, SystemColor.infoText, SystemColor.menu, SystemColor.menuText, SystemColor.scrollbar, SystemColor.text, SystemColor.textHighlight, SystemColor.textHighlightText, SystemColor.textInactiveText, SystemColor.textText, SystemColor.window, SystemColor.windowBorder, SystemColor.windowText }
    ;
    private String[] stringArray = { "activeCaption","activeCaptionBorder","activeCaptionText","control","controlDkShadow","controlHighlight","controlLtHighlight","controlShadow","controlText","desktop","inactiveCaption","inactiveCaptionBorder","inactiveCaptionText","info","infoText","menu","menuText","scrollbar","text","textHighlight","textHighlightText","textInactiveText","textText","window","windowBorder","windowText" 
    }
    ;
    
    private Scrollbar redScrollbar = new Scrollbar(Scrollbar.HORIZONTAL);
    private Scrollbar greenScrollbar = new Scrollbar(Scrollbar.HORIZONTAL);
    private Scrollbar blueScrollbar = new Scrollbar(Scrollbar.HORIZONTAL);

    private TextField redTextField = new IntegerField(4);
    private TextField greenTextField = new IntegerField(4);
    private TextField blueTextField = new IntegerField(4);
    private Color color = new Color(0,0,0);
    private ColorCap colorCap;
    
    private StringBuffer text = new StringBuffer();
    
    private GUIComponent component;

    // Listeners

    private TextFocusListener textFocusListener = new TextFocusListener();
    private ScrollbarListener scrollbarListener = new ScrollbarListener();
    private CanvasListener canvasListener = new CanvasListener();
    private ChoiceListener choiceListener = new ChoiceListener();
    private ButtonListener buttonListener = new ButtonListener();
    

    /**
     * Constructs a ColorDialog and shows it.
     *
     * @param f	            Frame.
     * @param colorCap      An colorCap containing a color.
     * @param text          Text to describe the color.
     * @param system	    if true, the color is a system color.
     */
    public ColorDialog(Frame f,ColorCap colorCap,StringBuffer text,boolean system) {
        super(f,true);
        this.component = component;
        this.color = colorCap.getColor();
        this.colorCap = colorCap;
        
        this.text = text;
        
        thecolor.setEnabled(false);

        redTextField.setText(""+color.getRed());
        greenTextField.setText(""+color.getGreen());
        blueTextField.setText(""+color.getBlue());

        redScrollbar.setValues(color.getRed(),1,0,255);
        greenScrollbar.setValues(color.getGreen(),1,0,255);
        blueScrollbar.setValues(color.getBlue(),1,0,255);
        
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 0;
        gbc1.weightx = 1.0;
        gbc1.weighty = 1.0;
        gbc1.fill = GridBagConstraints.BOTH;
        
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 1;
        gbcLabel.gridwidth = 2;
        
        gbcLabel.weightx = 1.0;
        gbcLabel.weighty = 1.0;
        gbcLabel.fill = GridBagConstraints.BOTH;
        
        GridBagConstraints gbcText = new GridBagConstraints();
        gbcText.gridx = 2;
        gbcText.gridy = 3;
        
        gbcText.weightx = 1.0;
        gbcText.weighty = 1.0;
        gbcText.fill = GridBagConstraints.HORIZONTAL;
        
            
        GridBagConstraints gbc4 = new GridBagConstraints();
        gbc4.gridx = 0;
        gbc4.gridheight = 7;
        gbc4.weightx = 1.0;
        gbc4.weighty = 1.0;
        gbc4.fill = GridBagConstraints.BOTH;

        GridBagConstraints gbcScroll = new GridBagConstraints();
        gbcScroll.gridx = 1;
        gbcScroll.gridwidth = 1;
        
        gbcScroll.weightx = 1.0;
        gbcScroll.weighty = 1.0;
        gbcScroll.fill = GridBagConstraints.HORIZONTAL;

        GridBagConstraints gbcCancel = new GridBagConstraints();
        gbcCancel.gridy = 8;
        gbcCancel.gridx = GridBagConstraints.RELATIVE;
        gbcCancel.gridwidth = 2;
        
        
        gbcCancel.weightx = 1.0;
        gbcCancel.weighty = 1.0;
        gbcCancel.fill = GridBagConstraints.BOTH;
        
        systemColorsChoice.add("none");
        
        setLayout(gb);
        add(colorCanvas,gbc4);
        gbcText.gridy = 2;
        add(thecolor,gbcLabel);
        add(new Label("Red"),gbcLabel);
        add(redTextField,gbcText);
        add(redScrollbar,gbcScroll);
        gbcText.gridy = 4;
        add(new Label("Green"),gbcLabel);
        add(greenTextField,gbcText);        
        add(greenScrollbar,gbcScroll);
        gbcText.gridy = 6;
        add(new Label("Blue"),gbcLabel);
        add(blueTextField,gbcText);        
        add(blueScrollbar,gbcScroll);
        add(systemColorsChoice,gbc1);
        add(okButton,gbc1);
        add(cancelButton,gbcCancel);
        for(int i=0 ; i < stringArray.length ; i++)
            systemColorsChoice.add(stringArray[i]);
        
        // add listeners
        
        redTextField.addFocusListener(textFocusListener);
        greenTextField.addFocusListener(textFocusListener);
        blueTextField.addFocusListener(textFocusListener);
        redScrollbar.addAdjustmentListener(scrollbarListener);
        greenScrollbar.addAdjustmentListener(scrollbarListener);
        blueScrollbar.addAdjustmentListener(scrollbarListener);
        colorCanvas.addMouseListener(canvasListener);
        systemColorsChoice.addItemListener(choiceListener);
        okButton.addActionListener(buttonListener);
        cancelButton.addActionListener(buttonListener);

        pack();
        updateColor(color,system);
        if(system==true)
            {
                String str = new String(((text.toString()).substring(12)));

                systemColorsChoice.select(str);
            
            }
        show();
    }


    
    private void updateColor(Color newColor,boolean system)
    {
        color = newColor;
            
        thecolor.setBackground(color);
        redScrollbar.setValue(color.getRed());
        greenScrollbar.setValue(color.getGreen());
        blueScrollbar.setValue(color.getBlue());
        if(system==false) {
            systemColorsChoice.select(0);
        }
            
        redTextField.setText(""+color.getRed());
        greenTextField.setText(""+color.getGreen());
        blueTextField.setText(""+color.getBlue());
    }



    private class TextFocusListener implements FocusListener
    {
        public void focusGained(FocusEvent e)
        {
        }
        public void focusLost(FocusEvent e)
        {

            int red = Integer.parseInt(redTextField.getText());
            if(red>255)
                red = 255;
            int green = Integer.parseInt(greenTextField.getText());
            if(green>255)
                green = 255;
            int blue = Integer.parseInt(blueTextField.getText());
            if(blue>255)
                blue = 255;
            Color  newColor = new Color(red,green,blue);
            updateColor(newColor,false);
        }
    }


    
    private class ScrollbarListener implements AdjustmentListener
    {
        public void adjustmentValueChanged(AdjustmentEvent e)
        {
            Color newColor = new Color(redScrollbar.getValue(),greenScrollbar.getValue(),blueScrollbar.getValue());
                
                
            updateColor(newColor,false);
        }
    }


    
    private class CanvasListener extends MouseAdapter
    {
        public void mouseReleased(MouseEvent e)
        {
            Color newColor = new Color (((ColorCanvas)e.getComponent()).getColorAtPoint(e.getPoint()).getRGB());
            updateColor(newColor,false);
                
        }
    }


    
    private class ChoiceListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            int index = systemColorsChoice.getSelectedIndex();
            if(index>0){
                Color newColor = new Color((systemColors[index-1]).getRGB());
                updateColor(newColor,true);
            }
            
        }
    }


    
    private class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            if(e.getActionCommand().equals("Cancel"))
                {
                    dispose();
                }
            else {
                    
                if(systemColorsChoice.getSelectedIndex() > 0)
                    {
                        text.setLength(0);
                        text.append("SystemColor."+systemColorsChoice.getSelectedItem());
                        colorCap.setColor(color);
                        
                        color = null;
                    }
                    
                    
                else
                    {
                        text.setLength(0);
                        text.append("new Color("+color.getRed()+","+color.getGreen()+","+color.getBlue()+")");
                        colorCap.setColor(color);
                        
                        
                    }
                dispose();
                    
                    
                    
            }
        }
    }

} // ColorDialog
