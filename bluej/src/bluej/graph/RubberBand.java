package bluej.graph;

import java.awt.Point;

/**
 * Class RubberBand describes a rubber line used during line dragging.
 * The line always extends from a source target to a given end point.
 */

public class RubberBand 
{
    public Point startPt;
    public Point endPt;

    public RubberBand(int x1, int y1, int x2, int y2)
    {
        startPt = new Point(x1, y1);
        endPt = new Point(x2, y2);
    }
    
    public void setEnd(int x, int y)
    {
        endPt.move(x, y);
    }
}
