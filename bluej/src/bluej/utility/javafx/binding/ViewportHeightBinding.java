/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx.binding;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;

/**
 * An observable binding to the height of a ScrollPane's viewport height.
 */
public class ViewportHeightBinding extends DoubleBinding
{
    private ScrollPane scroll;

    public ViewportHeightBinding(ScrollPane scroll)
    {
        this.scroll = scroll;
        bind(scroll.viewportBoundsProperty());
    }

    @Override
    protected double computeValue()
    {
        Bounds viewBound = scroll.getViewportBounds();
        return viewBound == null ? 0.0 : viewBound.getHeight();
    }
}
