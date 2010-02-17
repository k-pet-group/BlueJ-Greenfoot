/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.moe;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bluej.prefmgr.PrefMgr;

/**
 * ScopeHighlightingPrefSlider is the slider that controls the 
 * strength of the scope highlighting colors
 * 
 * @author Marion Zalk
 */
public class ScopeHighlightingPrefDisplay extends JPanel implements ChangeListener
{
    public static final int MIN=0;
    public static final int MAX=20;
    JSlider slider;
    JPanel colorPanel;
    JPanel greenTextArea;
    JPanel yellowTextArea;
    JPanel pinkTextArea;
    JPanel blueTextArea;
    Color greenArea=new Color(235, 250, 235);
    Color pinkArea=new Color(250, 240, 250);
    Color yellowArea=new Color(250, 250, 225);
    Color blueArea=new Color(240, 240, 250);
    Color greenSetting;
    Color yellowSetting;
    Color pinkSetting;
    Color blueSetting;
    Color bg;

    /**
     * Constructor that sets up the look and feel for the scope highlighting color slider
     * and the panel displaying the affects of changing the value of the slider
     */
    public ScopeHighlightingPrefDisplay()
    {
        MoeSyntaxDocument.getColors();
        bg = MoeSyntaxDocument.getBackgroundColor();
        {
            //initialises the slider functionality
            slider=new JSlider(MIN, MAX);
            //set the transparency value from the prefMgr
            slider.setValue(PrefMgr.getScopeHighlightStrength());
            //labels
            Hashtable<Integer, JLabel>labelTable = new Hashtable<Integer, JLabel>();
            labelTable.put(new Integer(MIN), new JLabel("Transparent"));
            labelTable.put(new Integer(MAX), new JLabel("Highlighted"));
            slider.setLabelTable( labelTable );
            slider.setPaintLabels(true);
            slider.addChangeListener(this);
        }
        //initialises the color palette
        {
            colorPanel=new JPanel(new GridLayout(4,1,0,0));     
            colorPanel.setBorder(BorderFactory.createLineBorder(bg,5));
            //colorPanel.get
            greenTextArea=new JPanel();
            setGreenPalette();
            yellowTextArea=new JPanel();
            setYellowPalette();
            pinkTextArea=new JPanel();
            setPinkPalette();
            blueTextArea=new JPanel();
            setBluePalette();
            colorPanel.add(greenTextArea);
            colorPanel.add(yellowTextArea);
            colorPanel.add(pinkTextArea);  
            colorPanel.add(blueTextArea);
        }
    }

    /*
     * Returns the highlighter slider
     */
    protected JSlider getHighlightStrengthSlider(){  
        return slider;

    }
    
    /*
     * Returns the color palette
     */
    protected JPanel getColourPalette(){
        return colorPanel;

    }

    /*
     * The value of the slider
     */
    protected int getStrengthValue()
    {
        return slider.getValue();   
    }

    /*
     * Setting the colour relative to the background colour of the document
     */
    private Color getReducedColor(Color thisColor)
    {
        colorPanel.setBackground(bg);
        double factor = getStrengthValue() / 20.0;
        double other = 1 - factor;
        int nr = Math.min((int)(thisColor.getRed() * factor + bg.getRed() * other), 255);
        int ng = Math.min((int)(thisColor.getGreen() * factor + bg.getGreen() * other), 255);
        int nb = Math.min((int)(thisColor.getBlue() * factor + bg.getBlue() * other), 255);
        return new Color(nr, ng, nb);
    }

    /*
     * Sets the green palette
     */
    protected void setGreenPalette()
    {
        greenSetting= getReducedColor(greenArea);
        greenTextArea.setBackground(greenSetting);
    }
    
    /*
     * Sets the yellow palette
     */
    protected void setYellowPalette()
    {
        yellowSetting= getReducedColor(yellowArea);
        yellowTextArea.setBackground(yellowSetting);
    }
    
    /*
     * Sets the pink palette
     */
    protected void setPinkPalette()
    {
        pinkSetting= getReducedColor(pinkArea);
        pinkTextArea.setBackground(pinkSetting);
    }
    
    /*
     * Sets the blue palette
     */
    protected void setBluePalette()
    {
        blueSetting= getReducedColor(blueArea);
        blueTextArea.setBackground(blueSetting);
    }

    /*
     * When the slider is moved, the color palette updates immediately 
     */
    public void stateChanged(ChangeEvent e) {
        setGreenPalette();
        setYellowPalette();
        setBluePalette();
        setPinkPalette();
    }
}
