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
