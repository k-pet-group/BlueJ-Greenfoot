/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg 

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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.value.ObservableIntegerValue;
import javafx.scene.paint.Color;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 25/04/2017.
 */
@OnThread(Tag.FX)
public interface ScopeColors
{
    public ObjectExpression<Color> scopeClassColorProperty();
    public ObjectExpression<Color> scopeClassInnerColorProperty();
    public ObjectExpression<Color> scopeClassOuterColorProperty();
    public ObjectExpression<Color> scopeMethodColorProperty();
    public ObjectExpression<Color> scopeMethodOuterColorProperty();
    public ObjectExpression<Color> scopeSelectionColorProperty();
    public ObjectExpression<Color> scopeSelectionOuterColorProperty();
    public ObjectExpression<Color> scopeIterationColorProperty();
    public ObjectExpression<Color> scopeIterationOuterColorProperty();
    public ObjectExpression<Color> scopeBackgroundColorProperty();

    public ObjectExpression<Color> breakpointOverlayColorProperty();
    public ObjectExpression<Color> stepMarkOverlayColorProperty();

    /**
     * Get a colour which has been faded toward the background according to the
     * given strength value. The higher the strength value, the less the colour
     * is faded.
     */
    public default ObjectExpression<Color> getReducedColor(ObjectExpression<Color> original, ObservableIntegerValue colorStrength)
    {
        return Bindings.createObjectBinding(() ->
        {
            Color bg = scopeBackgroundColorProperty().getValue();
            return bg.interpolate(original.getValue(), (double) colorStrength.get() / (double) ScopeHighlightingPrefDisplay.MAX);
        }, scopeBackgroundColorProperty(), colorStrength, original);
    }

    // Used for testing:
    @OnThread(Tag.FXPlatform)
    public static ScopeColors dummy()
    {
        // Simplest thing to do is make an off-screen ScopeColorsBorderPane:
        return new ScopeColorsBorderPane();
    }
}
