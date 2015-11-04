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
 * text.  This is useful for working out where to draw text selections
 * 
 */
public class TextOverlayPosition
{
    private final ExpressionSlotField src;
    // All coordinates are in terms of scene:
    private final double x;
    private final double topY;
    private final double baselineY;
    private final double bottomY;
    
    // src can be null
    private TextOverlayPosition(ExpressionSlotField src, double x, double topY, double baselineY, double bottomY)
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

    public static TextOverlayPosition nodeToOverlay(Node node, double x, double topY, double baselineY, double bottomY)
    {
        Point2D topLeft = node.localToScene(x, topY);
        Point2D baselineLeft = node.localToScene(x, baselineY);
        Point2D bottomLeft = node.localToScene(x, bottomY);
        return new TextOverlayPosition(topLeft.getX(), topLeft.getY(), baselineLeft.getY(), bottomLeft.getY());
        
    }
    
    public static class Line
    {
        public final List<TextOverlayPosition> positions = new ArrayList<>();
        public double startX;
        public double endX;
        public double topY;
        public double bottomY;
        
        public void transform(Function<Point2D, Point2D> trans)
        {
            Point2D topLeft = trans.apply(new Point2D(startX, topY));
            Point2D bottomRight = trans.apply(new Point2D(endX, bottomY));
            
            startX = topLeft.getX();
            endX = bottomRight.getX();
            topY = topLeft.getY();
            bottomY = bottomRight.getY();
        }
        
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

    public static TextOverlayPosition fromScene(double x,
            double topY, double baselineY, double bottomY,
            ExpressionSlotField expressionSlotField)
    {
        return new TextOverlayPosition(expressionSlotField, x, topY, baselineY, bottomY);
    }

    public ExpressionSlotField getSource()
    {
        return src;
    }
    
}