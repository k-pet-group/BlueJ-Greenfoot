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

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A background item on a TextLine, usually a scope background, but can also be
 * a marker for the current step line or a breakpoint line.
 */
@OnThread(Tag.FX)
class BackgroundItem extends Region
{
    final double x;
    final double width;

    /**
     * Create a background item with the given X position and width, and the given background fills.  This constructor sets the item to be unmanaged.
     */
    BackgroundItem(double x, double width, BackgroundFill... backgroundFills)
    {
        this.x = x;
        this.width = width;
        setManaged(false);
        setBackground(new Background(backgroundFills));
    }
}
