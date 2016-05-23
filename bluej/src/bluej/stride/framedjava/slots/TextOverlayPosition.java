/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.slots;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * Represents a text location on the overlay pane.  You can think of it as representing the
 * graphical area of a caret -- it has one X position, but a y-range that matches the height of the
 * text.  This is useful for working out where to draw text selections.
 * 
 * The constructors are private to avoid confusion about scene vs node coordinates.
 * Use the explicit nodeToOverlay or fromScene static methods to construct.
 */
public class TextOverlayPosition
{
    /** The StructuredSlotField in which this location lies.  May be null. */
    private final StructuredSlotField src;
    // All coordinates are in terms of scene:
    private final double x;
    private final double topY;
    private final double baselineY;
    private final double bottomY;
    
    // src can be null
    private TextOverlayPosition(StructuredSlotField src, double x, double topY, double baselineY, double bottomY)
    {
        this.src = src;
        this.x = x;
        this.topY = topY;
        this.baselineY = baselineY;
        this.bottomY = bottomY;
    }
    
    private TextOverlayPosition(double x, double topY, double baselineY, double bottomY)
    {
        this(null, x, topY, baselineY, bottomY);
    }
    
    public double getSceneX()
    {
        return x;
    }
    
    public double getSceneTopY()
    {
        return topY;
    }
    
    public double getSceneBaselineY()
    {
        return baselineY;
    }
    
    public double getSceneBottomY()
    {
        return bottomY;
    }
    
    public String toString()
    {
        return "(" + x + ", " + topY + " -> " + bottomY + ")"; 
    }

    /**
     * Given a Node and some node-local coordinates, transforms them into scene
     * coordinates to form the returned TextOverlayPosition.
     */
    public static TextOverlayPosition nodeToOverlay(Node node, double x, double topY, double baselineY, double bottomY)
    {
        Point2D topLeft = node.localToScene(x, topY);
        Point2D baselineLeft = node.localToScene(x, baselineY);
        Point2D bottomLeft = node.localToScene(x, bottomY);
        return new TextOverlayPosition(topLeft.getX(), topLeft.getY(), baselineLeft.getY(), bottomLeft.getY());
        
    }

    /**
     * A Line is a set of TextOverlayPosition items which fall on the same horizontal
     * line.  So a very short expression slot would have one line of TextOverlayPosition
     * items, and once it becomes long enough to hit the right-hand edge of the pane
     * and wraps around, you'll get multiple lines.
     */
    public static class Line
    {
        /** The list of all positions in this horizontal line, in ascending X order */
        public final List<TextOverlayPosition> positions = new ArrayList<>();
        /** The starting X position of the line (in scene) */
        public double startX;
        /** The ending X position of the line (in scene) */
        public double endX;
        /** The highest Y position on the line (i.e. lowest numeric value of Y) */
        public double topY;
        /** The lowest/bottom Y position on the line (i.e. highest numeric value of Y) */
        public double bottomY;

        /** Transforms this line's points by applying the given transformation function
         *  to the top-left and bottom-right */
        public void transform(Function<Point2D, Point2D> trans)
        {
            Point2D topLeft = trans.apply(new Point2D(startX, topY));
            Point2D bottomRight = trans.apply(new Point2D(endX, bottomY));
            
            startX = topLeft.getX();
            endX = bottomRight.getX();
            topY = topLeft.getY();
            bottomY = bottomRight.getY();
        }

        /**
         * Tries to add the given TextOverlayPosition to the line.
         * Returns true if the position fitted on this line,
         * either because the line was currently empty (in which case
         * any single position would fit), or because
         * the topY of this position was less than (i.e. graphically
         * above the bottom Y of the line as it stood.  This makes
         * sense if you are adding the components in order as you go
         * through the flow pane, from top left to bottom right.
         */
        public boolean add(TextOverlayPosition p)
        {
            if (positions.size() == 0)
            {
                topY = p.topY;
                bottomY = p.bottomY;
                startX = p.x;
                endX = p.x;
                positions.add(p);
                return true;
            }
            else
            {
                if (p.topY < bottomY)
                {
                    topY = Math.min(topY, p.topY);
                    bottomY = Math.max(bottomY, p.bottomY);
                    endX = p.x;
                    positions.add(p);
                    return true;
                }
                else
                    return false;
            }
        }

        public TextOverlayPosition getStart()
        {
            return positions.get(0);
        }

        public TextOverlayPosition getEnd()
        {
            return positions.get(positions.size() - 1);
        }
    }

    /**
     * Groups the list of text overlay positions (assumed to be in order from a set
     * of flow pane components) into a list of lines (@see {@link Line}).
     * 
     * The lines will be in increasing Y order (i.e. graphically highest line with lowest
     * Y first, down to lowest line with highest Y).
     */
    public static LinkedList<Line> groupIntoLines(List<TextOverlayPosition> positions)
    {
        LinkedList<Line> r = new LinkedList<>();
        if (positions.size() == 0)
            return r;
        r.add(new Line());
        // Go through rest of list:
        for (TextOverlayPosition p : positions)
        {
            if (!r.getLast().add(p))
            {
                r.add(new Line());
                r.getLast().add(p);
            }
        }
        return r;
    }

    /**
     * Creates a TextOverlayPosition using the given scene coordinates.
     */
    public static TextOverlayPosition fromScene(double x,
            double topY, double baselineY, double bottomY,
            StructuredSlotField expressionSlotField)
    {
        return new TextOverlayPosition(expressionSlotField, x, topY, baselineY, bottomY);
    }

    public StructuredSlotField getSource()
    {
        return src;
    }
    
}