package bluej.graph;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.Config;
import bluej.pkgmgr.target.ReadmeTarget;

public class ReadmeTargetPainter
{
    static final int CORNER_SIZE = 10;
    static final Color shadowCol = Config.getItemColour("colour.target.shadow");
    static final int SHAD_SIZE = 4;
    ReadmeTarget readmeTarget;
    /**
     * 
     */
    public ReadmeTargetPainter() {
       
    }
    
    public void paint(Graphics2D g, ReadmeTarget readmeTarget)
    {
        this.readmeTarget = readmeTarget;
        g.translate(readmeTarget.getX(), readmeTarget.getY());
        drawUMLStyle(g);
        g.translate(-readmeTarget.getX(), -readmeTarget.getY());
    }
    
    private void drawUMLStyle(Graphics2D g)
    {
        int width = readmeTarget.getWidth(), height = readmeTarget.getHeight();

        // draw the shadow
        g.setColor(shadowCol);
        g.fillRect(SHAD_SIZE, height, width, SHAD_SIZE);
        g.fillRect(width, CORNER_SIZE + SHAD_SIZE, SHAD_SIZE, height - CORNER_SIZE);

        // draw folded paper edge
        int xpoints[] = { 1, width - CORNER_SIZE, width, width, 1 };
        int ypoints[] = { 1, 1, CORNER_SIZE + 1, height, height };

        Polygon p = new Polygon(xpoints, ypoints, 5);

        int thickness = (readmeTarget.isSelected()) ? 2 : 1;

        g.setColor(Color.white);
        g.fill(p);
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(thickness));
        g.draw(p);

        g.drawLine(width - CORNER_SIZE, 1,
                width - CORNER_SIZE, CORNER_SIZE);
        g.drawLine(width - CORNER_SIZE, CORNER_SIZE,
                width - 2, CORNER_SIZE);

        g.setStroke(new BasicStroke(1));
        for(int yPos = CORNER_SIZE+10; yPos <= height-10; yPos += 5)
            g.drawLine(10, yPos, width - 10, yPos);
    }
}
