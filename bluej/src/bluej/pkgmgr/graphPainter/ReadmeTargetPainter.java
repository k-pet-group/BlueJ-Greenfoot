package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.pkgmgr.target.*;
import bluej.pkgmgr.target.ReadmeTarget;

/**
 * Paints a ReadmeTarget
 * @author fisker
 * @version $Id: ReadmeTargetPainter.java 2610 2004-06-14 12:46:06Z fisker $
 */
public class ReadmeTargetPainter
{
    static final int CORNER_SIZE = 10;
    ReadmeTarget readmeTarget;
    Graphics2D g;
    private GraphPainterStdImpl graphPainterStdImpl;
    /**
     * 
     */
    public ReadmeTargetPainter(GraphPainterStdImpl graphPainterStdImpl){
    	this.graphPainterStdImpl = graphPainterStdImpl;
    }
    
    public void paint(Graphics2D g, Target target)
    {

        this.readmeTarget = (ReadmeTarget) target;
        this.g = g;
        g.translate(readmeTarget.getX(), readmeTarget.getY());
        drawUMLStyle();
        g.translate(-readmeTarget.getX(), -readmeTarget.getY());
    }
    
    private void drawUMLStyle()
    {
        int width = readmeTarget.getWidth();
        int height = readmeTarget.getHeight();

        drawShadow();

        // draw folded paper edge
        int xpoints[] = { 1, width - CORNER_SIZE, width, width, 1 };
        int ypoints[] = { 1, 1, CORNER_SIZE + 1, height, height };

        Polygon p = new Polygon(xpoints, ypoints, 5);

        boolean isSelected = readmeTarget.isSelected() && graphPainterStdImpl.isGraphEditorInFocus();
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
    }
    
    private void drawShadow(){
	    int height = readmeTarget.getHeight();
	    int width = readmeTarget.getWidth();
	
	    g.setColor(TargetPainterConstants.colours[3]);
	    g.drawLine(3, height + 1, width , height + 1);//bottom
	    
	    g.setColor(TargetPainterConstants.colours[2]);
	    g.drawLine(4, height + 2, width , height + 2);//bottom
	    g.drawLine(width + 1, height + 2, width + 1, 3 + CORNER_SIZE);//left
	    
	    g.setColor(TargetPainterConstants.colours[1]);
	    g.drawLine(5, height + 3, width + 1, height + 3);//bottom
	    g.drawLine(width + 2, height + 3, width + 2, 4 + CORNER_SIZE);//left
	    
	    g.setColor(TargetPainterConstants.colours[0]);
	    g.drawLine(6, height + 4, width + 2, height + 4 ); //bottom
	    g.drawLine(width + 3, height + 3, width + 3, 5 + CORNER_SIZE);//left    
    }
}
