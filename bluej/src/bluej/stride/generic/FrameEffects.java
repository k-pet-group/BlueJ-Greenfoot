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
package bluej.stride.generic;

import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;

public class FrameEffects
{
    private static Effect disabledEffect;
    private static Effect dragSourceEffect;
    private static Effect dragSourceAndDisabledEffect;
    
    public static Effect getDisabledEffect()
    {
        if (disabledEffect == null)
        {
            ColorAdjust adj = new ColorAdjust(0, -0.5, 0.3, 0.0);
            adj.setInput(new BoxBlur(2, 2, 5));
            disabledEffect = adj;
        }
        return disabledEffect;
    }
    
    private static Effect getDragSourceEffect(Effect input)
    {
        BoxBlur bb = new BoxBlur();
        bb.setWidth(4);
        bb.setHeight(4);
        bb.setIterations(2);
        bb.setInput(input);
        return bb;
    }
    
    public static Effect getDragSourceEffect()
    {
        if (dragSourceEffect == null)
            dragSourceEffect = getDragSourceEffect(null);
        return dragSourceEffect;
    }
    
    public static Effect getDragSourceAndDisabledEffect()
    {
        if (dragSourceAndDisabledEffect == null)
            dragSourceAndDisabledEffect = getDragSourceEffect(getDisabledEffect());
        return dragSourceAndDisabledEffect;
    }
}
