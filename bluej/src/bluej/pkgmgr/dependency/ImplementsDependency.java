package bluej.pkgmgr.dependency;

import java.awt.*;
import java.util.Properties;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;

/**
 * An "implements" dependency between two (class) targets in a package
 *
 * @author  Michael Cahill
 * @version $Id: ImplementsDependency.java 2198 2003-10-02 04:13:48Z ajp $
 */
public class ImplementsDependency extends Dependency
{
	static final Color normalColour = Config.getItemColour("colour.arrow.implements");
	static final Color bgGraph = Config.getItemColour("colour.graph.background");
	static final int ARROW_SIZE = 18;		// pixels
	static final double ARROW_ANGLE = Math.PI / 6;	// radians
	static final int SELECT_DIST = 4;
    private static final float  dash1[] = {5.0f,2.0f};
    private static final BasicStroke dashedUnselected = new BasicStroke(strokeWithDefault,
                                                           BasicStroke.CAP_BUTT,
                                                           BasicStroke.JOIN_MITER,
                                                           10.0f, dash1, 0.0f);
    private static final BasicStroke dashedSelected = new BasicStroke(strokeWithSelected,
                                                           BasicStroke.CAP_BUTT,
                                                           BasicStroke.JOIN_MITER,
                                                           10.0f, dash1, 0.0f);
    private static final BasicStroke normalSelected = new BasicStroke(strokeWithSelected);
    private static final BasicStroke normalUnselected = new BasicStroke(strokeWithDefault);
        

	public ImplementsDependency(Package pkg, DependentTarget from, DependentTarget to)
	{
		super(pkg, from, to);
	}

	public ImplementsDependency(Package pkg)
	{
		this(pkg, null, null);
	}

	void draw(Color colour, Graphics2D g)
	{
        Stroke dashedStroke, normalStroke;
        if (isSelected()) 
        {
            //g.setXORMode(Color.red);
            dashedStroke = dashedSelected;
            normalStroke = normalSelected;            
        } 
        else
        {
            //g.setPaintMode();
            dashedStroke = dashedUnselected;
            normalStroke = normalUnselected;
        }
        g.setStroke(normalStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(colour);

        // Start from the centre of the src class
        Point pFrom = new Point(from.getX() + (int)from.getWidth()/2, from.getY() + (int)from.getHeight()/2);
        Point pTo = new Point(to.getX() + (int)to.getWidth()/2, to.getY() + (int)to.getHeight()/2);

        // Get the angle of the line from src to dst.
        double angle = Math.atan2(-(pFrom.getY() - pTo.getY()), pFrom.getX() - pTo.getX());

        // Get the dest point
        pFrom = ((DependentTarget)from).getAttachment(angle + Math.PI);
        pTo = ((DependentTarget)to).getAttachment(angle);

        Point pArrow = new Point(pTo.x + (int)((ARROW_SIZE - 2) * Math.cos(angle)), pTo.y - (int)((ARROW_SIZE - 2) * Math.sin(angle)));

        // draw the arrow head
        int[] xPoints =  { pTo.x, pTo.x + (int)((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)), pTo.x + (int)(ARROW_SIZE * Math.cos(angle - ARROW_ANGLE)) };
        int[] yPoints =  { pTo.y, pTo.y - (int)((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)), pTo.y - (int)(ARROW_SIZE * Math.sin(angle - ARROW_ANGLE)) };

        g.drawPolygon(xPoints, yPoints, 3);
        g.setStroke(dashedStroke);
        g.drawLine(pFrom.x, pFrom.y, pArrow.x, pArrow.y);

	}

	public void draw(Graphics2D g)
	{
        draw(normalColour, g);
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

	static final int normDist(Point a, Point b)
	{
		return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
	}

	public void highlight(Graphics2D g)
	{
		g.setXORMode(Color.red);
		draw(normalColour, g);
		g.setPaintMode();
	}

	public void load(Properties props, String prefix)
	{
		super.load(props, prefix);
	}

	public void save(Properties props, String prefix)
	{
		super.save(props, prefix);

		props.put(prefix + ".type", "ImplementsDependency");
	}
    
    public void remove(){
        pkg.removeArrow(this);
    }
}
