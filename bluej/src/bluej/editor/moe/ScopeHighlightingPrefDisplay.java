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

import bluej.Config;
import bluej.prefmgr.PrefMgr;

/**
 * A manager for the components used to manipulate the scope highlighting strength
 * preference.
 * 
 * @author Marion Zalk
 */
public class ScopeHighlightingPrefDisplay implements ChangeListener
{
    public static final int MIN=0;
    public static final int MAX=20;
    JSlider slider;
    JPanel colorPanel;
    JPanel greenPanelArea;
    JPanel yellowPanelArea;
    JPanel pinkPanelArea;
    JPanel bluePanelArea;
    Color greenArea = BlueJSyntaxView.GREEN_BASE;
    Color pinkArea = BlueJSyntaxView.PINK_BASE;
    Color yellowArea = BlueJSyntaxView.YELLOW_BASE;
    Color blueArea = BlueJSyntaxView.BLUE_BASE;
    Color greenBorder = BlueJSyntaxView.GREEN_OUTER_BASE;
    Color pinkBorder = BlueJSyntaxView.PINK_OUTER_BASE;
    Color yellowBorder = BlueJSyntaxView.YELLOW_OUTER_BASE;
    Color blueBorder = BlueJSyntaxView.BLUE_OUTER_BASE;
    Color greenSetting, greenSettingBorder;
    Color yellowSetting, yellowSettingBorder;
    Color pinkSetting, pinkSettingBorder;
    Color blueSetting, blueSettingBorder;
    Color bg;

    /**
     * Constructor that sets up the look and feel for the scope highlighting color slider
     * and the panel displaying the affects of changing the value of the slider
     */
    public ScopeHighlightingPrefDisplay()
    {
        MoeSyntaxDocument.getColors();
        bg = MoeSyntaxDocument.getBackgroundColor();
        //initialises the slider functionality
        {
            slider=new JSlider(MIN, MAX);
            //set the transparency value from the prefMgr
            slider.setValue(PrefMgr.getScopeHighlightStrength());
            //labels
            Hashtable<Integer, JLabel>labelTable = new Hashtable<Integer, JLabel>();
            labelTable.put(new Integer(MIN), new JLabel(Config.getString
                    ("prefmgr.edit.highlightLighter")));
            labelTable.put(new Integer(MAX), new JLabel(Config.getString
                    ("prefmgr.edit.highlightDarker")));
            slider.setLabelTable( labelTable );
            slider.setPaintLabels(true);
            slider.addChangeListener(this);
        }
        //initialises the color palette
        {
            colorPanel=new JPanel(new GridLayout(4,1,0,0));     
            colorPanel.setBorder(BorderFactory.createLineBorder(bg, 10));
            colorPanel.setBackground(bg);
            greenPanelArea=new JPanel();
            yellowPanelArea=new JPanel();
            pinkPanelArea=new JPanel();
            bluePanelArea=new JPanel();
            setPaletteValues();
            colorPanel.add(greenPanelArea);
            colorPanel.add(yellowPanelArea);
            colorPanel.add(pinkPanelArea);  
            colorPanel.add(bluePanelArea);
        }
    }

    /**
     * Returns the highlighter slider
     */
    protected JSlider getHighlightStrengthSlider()
    {  
        return slider;
    }

    /**
     * Returns the color palette
     */
    protected JPanel getColourPalette()
    {
        return colorPanel;
    }

    /**
     * The value of the slider
     */
    protected int getStrengthValue()
    {
        return slider.getValue();   
    }

    /**
     * Setting the colour relative to the background colour of the document
     * and using the value of the slider to calculate strength
     */
    private Color getReducedColor(Color c)
    {
        return BlueJSyntaxView.getReducedColor(c.getRed(), c.getGreen(),
                c.getBlue(), getStrengthValue());
    }

    /**
     * Sets the green palette
     */
    protected void setGreenPalette()
    {
        greenSetting= getReducedColor(greenArea);
        greenPanelArea.setBackground(greenSetting);
        greenSettingBorder=getReducedColor(greenBorder);
        greenPanelArea.setBorder(BorderFactory.createLineBorder(greenSettingBorder));
    }

    /**
     * Sets the yellow palette
     */
    protected void setYellowPalette()
    {
        yellowSetting= getReducedColor(yellowArea);
        yellowPanelArea.setBackground(yellowSetting);
        yellowSettingBorder=getReducedColor(yellowBorder);
        yellowPanelArea.setBorder(BorderFactory.createLineBorder(yellowSettingBorder));
    }

    /**
     * Sets the pink palette
     */
    protected void setPinkPalette()
    {
        pinkSetting= getReducedColor(pinkArea);
        pinkPanelArea.setBackground(pinkSetting);
        pinkSettingBorder=getReducedColor(pinkBorder);
        pinkPanelArea.setBorder(BorderFactory.createLineBorder(pinkSettingBorder));
    }

    /**
     * Sets the blue palette
     */
    protected void setBluePalette()
    {
        blueSetting = getReducedColor(blueArea);
        bluePanelArea.setBackground(blueSetting);
        blueSettingBorder=getReducedColor(blueBorder);
        bluePanelArea.setBorder(BorderFactory.createLineBorder(blueSettingBorder));
    }

    /**
     * When the slider is moved, the color palette updates immediately
     */
    public void stateChanged(ChangeEvent e)
    {
        setPaletteValues();
    }

    /**
     * Updates the palette with the color of the wash and border
     */
    private void setPaletteValues()
    {
        setGreenPalette();
        setYellowPalette();
        setBluePalette();
        setPinkPalette();
    }
}
