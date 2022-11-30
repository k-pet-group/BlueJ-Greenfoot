/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.guifx.classes;

import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * I tried many different variants before using this design:
 *  - If you have each L piece in the diagram as separate items, it is difficult
 *    to get the join to line up
 *  - The same applies if you have the triangle as a separate piece
 *  - If you use Polyline, it's awkward to draw multiple dangling "side-arms"
 *    as you have to redraw parts of the line.
 *  Hence the finished solution: A Path that does all the inheritance arrows for
 *  a particular class (down to its subclasses) as a single item, using a mixture
 *  of LineTo and MoveTo.
 */
@OnThread(Tag.FXPlatform)
public class InheritArrow extends Path
{
    private final double ARROWHEAD_WIDTH = 10;
    private final double ARROWHEAD_HEIGHT = 10;
    
    public InheritArrow()
    {
        getStyleClass().add("inherit-arrow");
    }

    /**
     * Set the locations of the subclass arms (the pieces going from the main vertical
     * line across to the right to touch the classes).
     * @param width The width of each subclass arm from the vertical line
     * @param yPositions The Y positions (relative to the top of this inherit arrow) of
     *                   the arms to draw.
     */
    public void setArmLocations(double width, List<Double> yPositions)
    {
        // If no arms, then no subclasses: clear all elements to effectively hide this arrow:
        if (yPositions.isEmpty())
        {
            getElements().clear();
            return;
        }
        
        // Draw arrow head and end at the bottom middle:
        getElements().setAll(
                new MoveTo(ARROWHEAD_WIDTH / 2.0, ARROWHEAD_HEIGHT), // Mid bottom of arrow
                new LineTo(0.0, ARROWHEAD_HEIGHT), // bottom left of arrow
                new LineTo(ARROWHEAD_WIDTH / 2.0, 0.0), // Top of arrow
                new LineTo(ARROWHEAD_WIDTH, ARROWHEAD_HEIGHT), // bottom right of arrow
                new LineTo(ARROWHEAD_WIDTH / 2.0, ARROWHEAD_HEIGHT) // Mid bottom of arrow
        );

        double indent = ARROWHEAD_WIDTH / 2.0;
        for (Double yPosition : yPositions)
        {
            // Draw down, then draw right, then move back to the bottom left of the current line:
            getElements().addAll(
                new LineTo(indent, yPosition),
                new LineTo(width, yPosition),
                new MoveTo(indent, yPosition)
            );
        }
    }
}
