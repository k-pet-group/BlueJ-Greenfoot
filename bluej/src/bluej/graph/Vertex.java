package bluej.graph;

import java.awt.*;

/**
 * General graph vertices
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Vertex.java 2044 2003-06-23 11:52:54Z fisker $
 */
public abstract class Vertex extends GraphElement
{
    private int x, y;            // position
    private int width, height;   // size

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

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }
   

    public int getHeight()
    {
        return this.height;
    }

    /*package*/ void drawUntranslated(Graphics g)
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
    
    /**
     * The default shape for a vertex is a rectangle. Child classes can
     * override this method to define more complex shapes.
     */
    public boolean contains(int x, int y){
        return (getX() <= x) && (x < getX() + getWidth()) &&
               (getY() <= y) && (y < getY() + getHeight());
    }

    
}
