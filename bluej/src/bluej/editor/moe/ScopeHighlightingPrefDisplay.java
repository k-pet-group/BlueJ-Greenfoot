/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2016  Michael Kolling and John Rosenberg 

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

import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A manager for the components used to manipulate the scope highlighting strength
 * preference.
 * 
 * @author Marion Zalk
 */
@OnThread(Tag.FX)
public class ScopeHighlightingPrefDisplay
{
    public static final int MIN=0;
    public static final int MAX=20;
    Slider slider;
    Pane colorPanel;
    Rectangle greenPanelArea;
    Rectangle yellowPanelArea;
    Rectangle pinkPanelArea;
    Rectangle bluePanelArea;
    Color greenArea = toFX(BlueJSyntaxView.GREEN_BASE);
    Color pinkArea = toFX(BlueJSyntaxView.PINK_BASE);
    Color yellowArea = toFX(BlueJSyntaxView.YELLOW_BASE);
    Color blueArea = toFX(BlueJSyntaxView.BLUE_BASE);
    Color greenBorder = toFX(BlueJSyntaxView.GREEN_OUTER_BASE);
    Color pinkBorder = toFX(BlueJSyntaxView.PINK_OUTER_BASE);
    Color yellowBorder = toFX(BlueJSyntaxView.YELLOW_OUTER_BASE);
    Color blueBorder = toFX(BlueJSyntaxView.BLUE_OUTER_BASE);
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
        //bg = MoeSyntaxDocument.getBackgroundColor();
        //initialises the slider functionality
        {
            //set the transparency value from the prefMgr
            slider=new Slider(MIN, MAX, PrefMgr.getScopeHighlightStrength());
            //labels
            slider.setMajorTickUnit(MAX - MIN);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setLabelFormatter(new StringConverter<Double>()
            {
                @Override
                public String toString(Double d)
                {
                    if (d == MIN)
                        return Config.getString("prefmgr.edit.highlightLighter");
                    else if (d == MAX)
                        return Config.getString("prefmgr.edit.highlightDarker");
                    else
                        return "";
                }

                @Override
                public Double fromString(String string)
                {
                    return null;
                }
            });
            JavaFXUtil.addChangeListener(slider.valueProperty(), v -> setPaletteValues());
        }
        //initialises the color palette
        {
            colorPanel = new VBox();
            JavaFXUtil.addStyleClass(colorPanel, "prefmgr-scope-colour-container");
            VBox inner = JavaFXUtil.withStyleClass(new VBox(), "prefmgr-scope-colour-rectangles");
            greenPanelArea = makeRectangle();
            yellowPanelArea = makeRectangle();
            pinkPanelArea = makeRectangle();
            bluePanelArea = makeRectangle();
            setPaletteValues();
            inner.getChildren().add(greenPanelArea);
            inner.getChildren().add(yellowPanelArea);
            inner.getChildren().add(pinkPanelArea);
            inner.getChildren().add(bluePanelArea);
            colorPanel.getChildren().add(inner);
        }
    }

    private Rectangle makeRectangle()
    {
        return new Rectangle(100, 20);
    }

    /**
     * Returns the highlighter slider
     */
    protected Node getHighlightStrengthSlider()
    {  
        return slider;
    }

    /**
     * Returns the color palette
     */
    protected Node getColourPalette()
    {
        return colorPanel;
    }

    /**
     * The value of the slider
     */
    protected int getStrengthValue()
    {
        return (int)Math.round(slider.getValue());   
    }

    /**
     * Setting the colour relative to the background colour of the document
     * and using the value of the slider to calculate strength
     */
    private Color getReducedColor(Color c)
    {
        return toFX(BlueJSyntaxView.getReducedColor((int)(255.0 * c.getRed()), (int)(255.0 * c.getGreen()),
            (int)(255.0 * c.getBlue()), getStrengthValue()));
    }

    /**
     * Sets the green palette
     */
    protected void setGreenPalette()
    {
        greenSetting= getReducedColor(greenArea);
        greenPanelArea.setFill(greenSetting);
        greenSettingBorder=getReducedColor(greenBorder);
        greenPanelArea.setStroke(greenSettingBorder);
    }

    /**
     * Sets the yellow palette
     */
    protected void setYellowPalette()
    {
        yellowSetting= getReducedColor(yellowArea);
        yellowPanelArea.setFill(yellowSetting);
        yellowSettingBorder=getReducedColor(yellowBorder);
        yellowPanelArea.setStroke(yellowSettingBorder);
    }

    /**
     * Sets the pink palette
     */
    protected void setPinkPalette()
    {
        pinkSetting= getReducedColor(pinkArea);
        pinkPanelArea.setFill(pinkSetting);
        pinkSettingBorder=getReducedColor(pinkBorder);
        pinkPanelArea.setStroke(pinkSettingBorder);
    }

    /**
     * Sets the blue palette
     */
    protected void setBluePalette()
    {
        blueSetting = getReducedColor(blueArea);
        bluePanelArea.setFill(blueSetting);
        blueSettingBorder=getReducedColor(blueBorder);
        bluePanelArea.setStroke(blueSettingBorder);
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
    
    // From http://stackoverflow.com/questions/30466405/java-convert-java-awt-color-to-javafx-scene-paint-color
    private static Color toFX(java.awt.Color awtColor)
    {
        int r = awtColor.getRed();
        int g = awtColor.getGreen();
        int b = awtColor.getBlue();
        int a = awtColor.getAlpha();
        double opacity = a / 255.0 ;
        return javafx.scene.paint.Color.rgb(r, g, b, opacity);
    }
}
