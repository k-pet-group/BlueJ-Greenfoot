package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.pkgmgr.target.*;

/**
 * Paints a ReadmeTarget
 * @author fisker
 * @version $Id: ReadmeTargetPainter.java 2907 2004-08-18 19:00:38Z mik $
 */
public class ReadmeTargetPainter
{
    private static final int CORNER_SIZE = 11;

    /**
     * Create the painter.
     */
    public ReadmeTargetPainter()
    {
    }
    
    /**
     * Paint the given target on the specified graphics context.
     * @param g  The graphics context to paint on.
     * @param target  The target to paint.
     */
    public void paint(Graphics2D g, Target target, boolean hasFocus)
    {
        g.translate(target.getX(), target.getY());

        int width = target.getWidth();
        int height = target.getHeight();

        drawShadow(g, width, height);

        // draw folded paper edge
        int[] xpoints = { 1, width - CORNER_SIZE, width, width, 1 };
        int[] ypoints = { 1, 1, CORNER_SIZE + 1, height, height };

        Polygon p = new Polygon(xpoints, ypoints, 5);

        boolean isSelected = target.isSelected() && hasFocus;
        int thickness = (isSelected) ? 2 : 1;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.WHITE);
        g.fill(p);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(thickness));
        g.draw(p);

        g.drawLine(width - CORNER_SIZE, 1,
                width - CORNER_SIZE, CORNER_SIZE);
        g.drawLine(width - CORNER_SIZE, CORNER_SIZE,
                width - 2, CORNER_SIZE);

        g.setStroke(new BasicStroke(1));
        
        //draw the lines in the paper
        for(int yPos = CORNER_SIZE+10; yPos <= height-10; yPos += 5)
            g.drawLine(10, yPos, width - 10, yPos);
        
        g.translate(-target.getX(), -target.getY());
    }
    
    /**
     * Draw the drop shadow to the right and below the icon.
     */
    private void drawShadow(Graphics2D g, int width, int height)
    {
	    g.setColor(GraphPainterStdImpl.shadowColours[3]);
	    g.drawLine(3, height, width , height); //bottom
	    
	    g.setColor(GraphPainterStdImpl.shadowColours[2]);
	    g.drawLine(4, height + 1, width , height + 1); //bottom
	    g.drawLine(width + 1, height + 2, width + 1, 3 + CORNER_SIZE); //left
	    
	    g.setColor(GraphPainterStdImpl.shadowColours[1]);
	    g.drawLine(5, height + 2, width + 1, height + 2); //bottom
	    g.drawLine(width + 2, height + 3, width + 2, 4 + CORNER_SIZE); //left
	    
	    g.setColor(GraphPainterStdImpl.shadowColours[0]);
	    g.drawLine(6, height + 3, width + 2, height + 3 ); //bottom
	    g.drawLine(width + 3, height + 3, width + 3, 5 + CORNER_SIZE); //left    
    }
}
