package greenfoot.util;

/**
 * A location in integers
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: Location.java 3124 2004-11-18 16:08:48Z polle $
 */
public class Location
    implements Cloneable
{
    private int x;
    private int y;

    public Location()
    {
        x = 0;
        y = 0;
    }

    public Location(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    /**
     * Multiplies the location with the parameters.
     * 
     * @param xMappingScale
     * @param yMappingScale
     */
    public void scale(int xMappingScale, int yMappingScale)
    {
        x *= xMappingScale;
        y *= yMappingScale;
    }

    public Object clone()
    {
        Object o = null;
        try {
            o = super.clone();
        }
        catch (CloneNotSupportedException e) {}
        return o;
    }

    /**
     * Adds the parameters to the location.
     * 
     * @param dx
     * @param dy
     */
    public void add(int dx, int dy)
    {
        x += dx;
        y += dy;
    }

    public String toString()
    {
        String s = super.toString();
        s = s + " (" + getX() + ", " + getY() + ")";
        return s;
    }

}