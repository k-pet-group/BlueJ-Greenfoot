package bluej.pkgmgr.graphPainter;

import java.awt.*;

import bluej.Config;
import bluej.graph.GraphElementController;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ExtendsDependency;
import bluej.pkgmgr.dependency.Dependency.Line;
import bluej.pkgmgr.target.DependentTarget;

/**
 * Paints a ClassTarget
 * @author fisker
 * @author  Michael Kolling
 * @version $Id: ExtendsDependencyPainter.java 2775 2004-07-09 15:07:12Z mik $
 */
public class ExtendsDependencyPainter implements DependencyPainter
{
    protected static final float strokeWidthDefault = 1.0f;
    protected static final float strokeWidthSelected = 2.0f;
    
    static final Color normalColour = Config.getItemColour("colour.arrow.extends");
    private static final BasicStroke normalSelected = new BasicStroke(strokeWidthSelected);
    private static final BasicStroke normalUnselected = new BasicStroke(strokeWidthDefault);
    static final int ARROW_SIZE = 18;		// pixels
    static final double ARROW_ANGLE = Math.PI / 6;	// radians
    
    public ExtendsDependencyPainter()
    {
    }
    
    public void paint(Graphics2D g, Dependency dependency, boolean hasFocus) 
    {
        if (!(dependency instanceof ExtendsDependency)) {
            throw new IllegalArgumentException("Not a ExtendsDependency");
        }
        Stroke oldStroke = g.getStroke();
        ExtendsDependency d = (ExtendsDependency) dependency;
        
        boolean isSelected = d.isSelected() && hasFocus;
        g.setStroke((isSelected? normalSelected : normalUnselected));

        Line line = d.computeLine();
        
        paintArrow(g, line.from, line.to);
        g.setStroke(oldStroke);
    }

    
    /**
     * Draw an ExtendsDependency from DependTarget d to the mouse position
     */
    public void paintIntermediateDependency(Graphics2D g, DependentTarget d) {
        g.setStroke(normalUnselected);

        Point pFrom = new Point(d.getX() + d.getWidth()/2,
                				d.getY() + d.getHeight()/2);
        Point pTo = new Point(GraphElementController.dependencyArrowX, 
                			  GraphElementController.dependencyArrowY);
        paintArrow(g, pFrom, pTo);
    }

    
    private void paintArrow(Graphics2D g, Point pFrom, Point pTo) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(normalColour);
        double angle = Math.atan2(-(pFrom.y - pTo.y), pFrom.x - pTo.x);
        
        Point pArrow = new Point(pTo.x + (int)((ARROW_SIZE - 2) * Math.cos(angle)),
                				 pTo.y - (int)((ARROW_SIZE - 2) * Math.sin(angle)));

        // draw the arrow head
        int[] xPoints =  { pTo.x, pTo.x + (int)((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)), pTo.x + (int)(ARROW_SIZE * Math.cos(angle - ARROW_ANGLE)) };
        int[] yPoints =  { pTo.y, pTo.y - (int)((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)), pTo.y - (int)(ARROW_SIZE * Math.sin(angle - ARROW_ANGLE)) };

        g.drawPolygon(xPoints, yPoints, 3);
        g.drawLine(pFrom.x, pFrom.y, pArrow.x, pArrow.y);
    }


    /* (non-Javadoc)
     * @see bluej.pkgmgr.graphPainter.DependencyPainter#getPopupMenuPosition(bluej.pkgmgr.dependency.Dependency)
     */
    public Point getPopupMenuPosition(Dependency dependency) {
        
            if (! (dependency instanceof ExtendsDependency)){
                throw new IllegalArgumentException("Not a ExtendsDependency");
            }
            Point pFrom = new Point(dependency.getFrom().getX() + dependency.getFrom().getWidth()/2,
                    				dependency.getFrom().getY() + dependency.getFrom().getHeight()/2);
            Point pTo = new Point(dependency.getTo().getX() + dependency.getTo().getWidth()/2,
                    			  dependency.getTo().getY() + dependency.getTo().getHeight()/2);
            // Get the angle of the line from src to dst.
            double angle = Math.atan2(-(pFrom.y - pTo.y), pFrom.x - pTo.x);
            pTo = dependency.getTo().getAttachment(angle);
//          draw the arrow head
            int[] xPoints =  { pTo.x, pTo.x + (int)((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)), pTo.x + (int)(ARROW_SIZE * Math.cos(angle - ARROW_ANGLE)) };
            int[] yPoints =  { pTo.y, pTo.y - (int)((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)), pTo.y - (int)(ARROW_SIZE * Math.sin(angle - ARROW_ANGLE)) };
            return new Point((xPoints[0] + xPoints[1] + xPoints[2])/3, 
           		 			 (yPoints[0] + yPoints[1] + yPoints[2])/3);
            
    }
    
    
}
