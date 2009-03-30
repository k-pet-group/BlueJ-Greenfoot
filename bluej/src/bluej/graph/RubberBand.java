/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.graph;

import java.awt.Point;

/**
 * Class RubberBand describes a rubber line used during line dragging.
 * The line always extends from a source target to a given end point.
 */

public class RubberBand 
{
    /** The line's start point */
    public Point startPt;
    /** The line's end point */
    public Point endPt;

    /**
     * Create a rubber band description, giving coordinates of start
     * and end points.
     */
    public RubberBand(int x1, int y1, int x2, int y2)
    {
        startPt = new Point(x1, y1);
        endPt = new Point(x2, y2);
    }

    /**
     * Adjust the rubber band's current end point.
     * @param x  New end point x coordinate
     * @param y  New end point y coordinate
     */
    public void setEnd(int x, int y)
    {
        endPt.move(x, y);
    }
}
