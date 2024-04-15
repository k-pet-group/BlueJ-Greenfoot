/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2021,2024  Michael Kolling and John Rosenberg

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
package bluej.editor.base;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;

/**
 * A background item on a TextLine, usually a scope background, but can also be
 * a marker for the current step line or a breakpoint line.
 */
@OnThread(Tag.FX)
public class BackgroundItem extends Region
{
    final double x;
    final double width;
    private final BackgroundFill[] backgroundFills;

    /**
     * Create a background item with the given X position and width, and the given background fills.  This constructor sets the item to be unmanaged.
     */
    public BackgroundItem(double x, double width, BackgroundFill... backgroundFills)
    {
        this.x = x;
        this.width = width;
        setManaged(false);
        this.backgroundFills = backgroundFills.clone();
        setBackground(new Background(backgroundFills));
    }

    /**
     * Returns true if this background item is equivalent to the given parameter.
     * This is like .equals() but I don't want to override that in case it messes
     * with JavaFX (since we extend Region and go in the GUI).
     */
    public boolean sameAs(BackgroundItem o)
    {
        return o != null && x == o.x && width == o.width && Arrays.deepEquals(backgroundFills, o.backgroundFills);
    }

    /**
     * Make an equivalent copy with the same properties
     */
    public BackgroundItem makeCopy()
    {
        return new BackgroundItem(x, width, backgroundFills.clone());
    }
}
