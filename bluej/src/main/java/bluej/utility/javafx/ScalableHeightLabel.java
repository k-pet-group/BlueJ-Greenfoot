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

import bluej.utility.Debug;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A class that is a Label with the added capability of scaling its vertical height
 * from a factor of 0.0 (i.e. hidden) up to 1.0 (i.e. full size for the label).
 *
 */
public class ScalableHeightLabel extends Label
{
    private final SimpleDoubleProperty scale = new SimpleDoubleProperty(1.0);
    
    /**
     * 
     * @param text
     * @param assumeNoWrap If true, we use the control's width at 
     * @param startHidden
     */
    public ScalableHeightLabel(String text, boolean startHidden)
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

    public void growToFullHeightWith(SharedTransition t, boolean fade)
    {
        scale.bind(t.getProgress());
        t.addOnStopped(scale::unbind);
        if (fade)
        {
            opacityProperty().bind(t.getProgress());
            t.addOnStopped(opacityProperty()::unbind);
        }
        // Note: Tried supporting squishing transform, a la FrameCanvas,
        // but had no luck in getting it to work.
    }

    public void shrinkToNothingWith(SharedTransition t, boolean fade)
    {
        scale.bind(t.getOppositeProgress());
        t.addOnStopped(scale::unbind);
        if (fade)
        {
            opacityProperty().bind(t.getOppositeProgress());
            t.addOnStopped(opacityProperty()::unbind);
        }
    }

    @Override
    public double getBaselineOffset()
    {
        // We must scale the baseline offset.  Otherwise, even though our height scales down to 0,
        // the baseline offset remains on e.g. 20 pixels, and thus our height inside a HangingFlowPane
        // always stays at e.g. 20 pixels, meaning our containing flow pane won't shrink when we do:
        return super.getBaselineOffset() * scale.get();
    }
}