package bluej.graph;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * General graph vertices
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Vertex.java 1459 2002-10-23 12:13:12Z jckm $
 */
public abstract class Vertex
{
    public int x, y;            // position
    public int width, height;   // size

    public Vertex(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setPos(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public Rectangle getRectangle()
    {
        return new Rectangle(x,y,width,height);
    }

    public int getWidth()
    {
        return this.width;
    }

    void drawUntranslated(Graphics g)
    {
        /* the shadows of the targets is rendered outside their actual
           bounding box. Most of the rendering code is not designed with
           clipping regions in mind and hence unless we artificially
           extend our clipping region, we chop of the shadows.. no big
           deal but if we ever redesign the drawing code we should look at
           it again */
        Graphics2D newg = (Graphics2D) g.create(x,y,width+16,height+16);

        draw(newg);
    }

    public abstract void draw(Graphics2D g);

    public void mousePressed(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseReleased(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseDragged(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseMoved(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor) {}
    public void singleClick(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void popupMenu(int x, int y, GraphEditor editor) {}
}
