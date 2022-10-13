/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg
 
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
package bluej.utility.javafx;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.util.Duration;

import bluej.stride.generic.Frame;

/**
 * A class that is a Label with the added capability of scaling its vertical height
 * from a factor of 0.0 (i.e. hidden) up to 1.0 (i.e. full size for the label).
 *
 */
public class ScalableHeightTextField extends TextField
{
    private final SimpleDoubleProperty scale = new SimpleDoubleProperty(1.0);
    
    public ScalableHeightTextField(String text)
    {
        this(text, false);
    }
    
    /**
     * 
     * @param text
     * @param startHidden
     */
    public ScalableHeightTextField(String text, boolean startHidden)
    {
        super(text);
        setMinHeight(0);
        if (startHidden)
        {
            setPrefHeight(0);
            scale.set(0.0);
        }
        scale.addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> arg0,
                                Number arg1, Number newVal)
            {
                setPrefHeight(newVal.doubleValue() * computePrefHeight(9999));
            }
        });

        JavaFXUtil.workAroundFunctionKeyBug(this);
    }
    
    public void setToFullHeight()
    {
        scale.set(1.0);
    }
    
    public void setToNothing()
    {
        scale.set(0.0);
    }

    /**
     * Animates up to scale height of 1.0
     * @param dur Duration of transition.  Null means immediate
     * @return 
     */
    public Timeline getGrowToFullHeightTimeline(Duration dur)
    {
        return new Timeline(new KeyFrame(dur, new KeyValue(scale, 1.0)));
    }
    
    public Timeline getShrinkToNothingTimeline(Duration dur)
    {
        return new Timeline(new KeyFrame(dur, new KeyValue(scale, 0.0)));
    }

    public void animateToFromMonospace(boolean toMonospace, boolean zeroWidthWhenEmpty, SharedTransition animate)
    {
        if (toMonospace)
        {
            Font monospace = Font.font("monospace", getFont().getSize());
            double origWidth = getWidth();
            double targetWidth = zeroWidthWhenEmpty ? 0.0 : JavaFXUtil.measureString(this, getText(), monospace, true, true );
            minWidthProperty().bind(animate.getProgress().multiply(targetWidth - origWidth).add(origWidth));
        }
        else
        {
            double origWidth = getWidth();
            applyCss();
            // applyCss so that we get the original font, below:
            double targetWidth = JavaFXUtil.measureString(this, getText(), getFont(), true, true);
            minWidthProperty().unbind();
            minWidthProperty().bind(animate.getProgress().multiply(targetWidth - origWidth).add(origWidth));
            animate.addOnStopped(() -> {
                minWidthProperty().unbind();
                setMinWidth(0);
            });
        }
    }
}