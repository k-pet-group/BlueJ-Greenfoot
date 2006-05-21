package greenfoot.collision.ibsp;

public class Rect
{
    private int x, y, width, height;
    
    public Rect(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        // DAV
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException();
        }
    }
    
    public String toString()
    {
        return ("rect (" + x + "," + y + ")-(" + (x + width) + "," + (y + height) + ")");
    }
    
    public int getX()
    {
        return x;
    }
    
    public int getMiddleX()
    {
        return x + width / 2;
    }
    
    public int getRight()
    {
        return x + width;
    }
    
    public int getY()
    {
        return y;
    }
        
    public int getMiddleY()
    {
        return y + height / 2;
    }
    
    public int getTop()
    {
        return y + height;
    }

    public int getWidth()
    {
        return width;
    }
    
    public int getHeight()
    {
        return height;
    }
    
    public boolean contains(Rect other)
    {
        return (x <= other.x &&
                y <= other.y &&
                getTop() >= other.getTop() &&
                getRight() >= other.getRight());
    }

    public static Rect getIntersection(Rect a, Rect b)
    {
        int a_x = a.getX();
        int a_r = a.getRight();
        int a_y = a.getY();
        int a_t = a.getTop();
        
        int b_x = b.getX();
        int b_r = b.getRight();
        int b_y = b.getY();
        int b_t = b.getTop();
        
        // Calculate intersection
        int i_x = Math.max(a_x, b_x);
        int i_r = Math.min(a_r, b_r);
        int i_y = Math.max(a_y, b_y);
        int i_t = Math.min(a_t, b_t);
        if (i_x >= i_r || i_y >= i_t) {
            return null;
        }
        else {
            return new Rect(i_x, i_y, i_r - i_x, i_t - i_y);
        }
    }
    
    public static boolean equals(Rect a, Rect b)
    {
        return a.x == b.x && a.y == b.y &&
            a.width == b.width && a.height == b.height;
    }
}
