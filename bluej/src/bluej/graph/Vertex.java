package bluej.graph;

import java.awt.*;

/**
 * General graph vertices
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: Vertex.java 2789 2004-07-12 18:08:11Z mik $
 */
public abstract class Vertex extends SelectableGraphElement
{
    private int x, y; // position
    private int width, height; // size

    /**
     * Create this vertex with given specific position.
     */
    public Vertex(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Set the position to the specified coordinates.
     */
    public void setPos(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Set the size to the specified height and width.
     */
    public void setSize(int width, int height)
    {
        this.width = (width > 0 ? width : 10);
        this.height = (height > 0 ? height : 10);
    }

    /**
     * Get this vertex's enclosing rectangle.
     */
    public Rectangle getRectangle()
    {
        return new Rectangle(x, y, width, height);
    }

    /**
     * Get this vertex's x position.
     */
    public int getX()
    {
        return this.x;
    }

    /**
     * Get this vertex's y position.
     */
    public int getY()
    {
        return this.y;
    }

    /**
     * Get this vertex's width.
     */
    public int getWidth()
    {
        return this.width;
    }

    /**
     * Get this vertex's height.
     */
    public int getHeight()
    {
        return this.height;
    }

    /**
     * The default shape for a vertex is a rectangle. Child classes can override
     * this method to define more complex shapes.
     */
    public boolean contains(int x, int y)
    {
        return (getX() <= x) && (x < getX() + getWidth()) && 
               (getY() <= y) && (y < getY() + getHeight());
    }
}