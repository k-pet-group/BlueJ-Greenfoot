package bluej.pkgmgr.dependency;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;

import java.util.Properties;
import java.awt.Point;
import java.awt.*;

/**
 * An "extends" dependency between two (class) targets in a package
 *
 * @author Michael Cahill
 * @version $Id: ExtendsDependency.java 2472 2004-02-09 13:00:47Z fisker $
 */
public class ExtendsDependency extends Dependency
{

    static final Color normalColour = Config.getItemColour("colour.arrow.extends");
    static final Color bgGraph = Config.getItemColour("colour.graph.background");
    static final int ARROW_SIZE = 18;		// pixels
    static final double ARROW_ANGLE = Math.PI / 6;	// radians
    static final int SELECT_DIST = 4;


    public ExtendsDependency(Package pkg, DependentTarget from, DependentTarget to)
    {
        super(pkg, from, to);
    }

    public ExtendsDependency(Package pkg)
    {
        this(pkg, null, null);
    }


    public boolean contains(int x, int y)
    {
        // Start from the centre of the src class
        Point pFrom = new Point(from.getX() + from.getWidth()/2, from.getY() + from.getHeight()/2);
        Point pTo = new Point(to.getX() + to.getWidth()/2, to.getY() + to.getHeight()/2);

        // Get the angle of the line from pFrom to pTo.
        double angle = Math.atan2(-(pFrom.y - pTo.y), pFrom.x - pTo.x);

        // Get the dest point
        pFrom = ((DependentTarget)from).getAttachment(angle + Math.PI);
        pTo = ((DependentTarget)to).getAttachment(angle);

        // Now check if <p> is in the rectangle
        if(x < Math.min(pFrom.x, pTo.x) - SELECT_DIST
           || x > Math.max(pFrom.x, pTo.x) + SELECT_DIST
           || y < Math.min(pFrom.y, pTo.y) - SELECT_DIST
           || y > Math.max(pFrom.y, pTo.y) + SELECT_DIST)
            {
                // Debug.message("ExtendsDependency.contains: (" + x + ", " + y + ") is outside the rectangle");
                return false;
            }

        // Get the angle of the line from pFrom to p
        double theta = Math.atan2(-(pFrom.y - y), pFrom.x - x);

        double norm = normDist(pFrom.x, pFrom.y, x, y, Math.sin(angle - theta));
        // Debug.message("ExtendsDependency.contains: (" + x + ", " + y + ") is at distance " + norm);
        return (norm < SELECT_DIST * SELECT_DIST);
    }

    static final double normDist(int ax, int ay, int bx, int by, double scale)
    {
        return ((ax - bx) * (ax - bx) + (ay - by) * (ay - by)) * scale * scale;
    }


    public void load(Properties props, String prefix)
    {
        super.load(props, prefix);
    }

    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        props.put(prefix + ".type", "ExtendsDependency");
    }
    
    public void remove(){
        pkg.removeArrow(this);
    }

}

