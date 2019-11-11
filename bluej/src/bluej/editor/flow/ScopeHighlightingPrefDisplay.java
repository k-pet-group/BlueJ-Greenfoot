/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2016,2019  Michael Kolling and John Rosenberg 

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
package bluej.editor.flow;

import bluej.editor.flow.ScopeColorsBorderPane;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
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
@OnThread(Tag.FXPlatform)
public class ScopeHighlightingPrefDisplay
{
    public static final int MIN=0;
    public static final int MAX=20;
    Slider slider;
    ScopeColorsBorderPane colorPanel;

    /**
     * Constructor that sets up the look and feel for the scope highlighting color slider
     * and the panel displaying the affects of changing the value of the slider
     */
    public ScopeHighlightingPrefDisplay()
    {
        //bg = MoeSyntaxDocument.getBackgroundColor();
        //initialises the slider functionality
        {
            //set the transparency value from the prefMgr
            slider=new Slider(MIN, MAX, PrefMgr.getScopeHighlightStrength().get());
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
        }
        //initialises the color palette
        {
            colorPanel = new ScopeColorsBorderPane();
            colorPanel.setBackground(new Background(new BackgroundFill(colorPanel.scopeBackgroundColorProperty().get(), null, null)));
            JavaFXUtil.addChangeListener(colorPanel.scopeBackgroundColorProperty(), bkColor -> {
                colorPanel.setBackground(new Background(new BackgroundFill(bkColor, null, null)));
            });
            JavaFXUtil.addStyleClass(colorPanel, "prefmgr-scope-colour-container");
            VBox inner = JavaFXUtil.withStyleClass(new VBox(), "prefmgr-scope-colour-rectangles");
            inner.getChildren().addAll(
                makeRectangle(colorPanel.scopeClassColorProperty(), colorPanel.scopeClassOuterColorProperty()),
                makeRectangle(colorPanel.scopeMethodColorProperty(), colorPanel.scopeMethodOuterColorProperty()),
                makeRectangle(colorPanel.scopeIterationColorProperty(), colorPanel.scopeIterationOuterColorProperty()),
                makeRectangle(colorPanel.scopeSelectionColorProperty(), colorPanel.scopeSelectionOuterColorProperty())
            );
            colorPanel.setCenter(inner);
        }
    }

    private Rectangle makeRectangle(ObjectExpression<Color> body, ObjectExpression<Color> border)
    {
        Rectangle r = new Rectangle(100, 20);
        IntegerBinding sliderValue = Bindings.createIntegerBinding(this::getStrengthValue, slider.valueProperty());
        r.fillProperty().bind(colorPanel.getReducedColor(body, sliderValue));
        r.strokeProperty().bind(colorPanel.getReducedColor(border, sliderValue));
        return r;
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
}
