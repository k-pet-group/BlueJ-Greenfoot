package greenfoot.util;


/**
 * 
 * Representation of a circle.
 * 
 * @author Poul Henriksen
 *
 */
public class Circle
{
    private int x;
    private int y;
    private int radius;
    private static long startTime;

    private static long instantiated;
    

    static {
     //   pool = new ObjectPool<Circle>();
        startTime = System.currentTimeMillis();
    }
    
    private static ObjectPool<Circle> pool = new ObjectPool<Circle>() {
        @Override
        protected Circle createNew()
        {
            return new Circle();
        }
    };
  
    
    public static Circle createCircle() {
        Circle c = (Circle) pool.get();
        c.init(0,0,0);
        return c;
    }    

    public static Circle createCircle(int x, int y, int r) {
        Circle c =  (Circle) pool.get();
        c.init(x,y,r);
        return c;
    }
    
    public void delete() {
        pool.add(this);
    }
    
    public static void clearPool() {
        pool.reset();
    }
    
    /**
     * @param x
     * @param y
     * @param r
     */
    private void init(int x, int y, int r)
    {
        if(r < 0 ) {
            throw new IllegalArgumentException("Radius must be larger than -1. It was: " + r);
        }
        this.setX(x);
        this.setY(y);
        setRadius(r);
    }
    
    public Circle() {
     /*   instantiated++;
        long now = System.currentTimeMillis();
        //   System.out.println("add: " +  (now - startTime ));
           
           if( (now - startTime )> 2005) {
               startTime = now;
               System.out.println("Instantiated circles: " + instantiated);
               
           }
    */
    }

    public double getVolume()
    {
        return Math.PI * radius * radius;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getX()
    {
        return x;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public int getY()
    {
        return y;
    }

    public void setRadius(int radius)
    {
        this.radius = radius;
    }

    public int getRadius()
    {
        return radius;
    }

    /**
     * Checks if this circles intersects the other circle.
     */
    public boolean intersects(Circle other)
    {
        int r1 = getRadius();
        int r2 = other.getRadius();
                    
        int dx = getX() - other.getX();
        int dy = getY() - other.getY();
        int dxSq = dx * dx;
        int dySq = dy * dy;
        
        int  circleDistSq = (dxSq + dySq);
        
        if( (r1 + r2) * (r1 + r2) >= circleDistSq) {
            return true;
        } else {
            return false;
        }            
    }

    /**
     * Calculates the circle that bounds this circle and the other.
     * 
     * @return An new circle bounding this and other.
     */
    public Circle merge(Circle other)
    {
        int dx = getX() - other.getX();
        int dy = getY() - other.getY();
        int dxSq = dx * dx;
        int dySq = dy * dy;        
        int circleDistSq = (dxSq + dySq);
        int r2 = (getRadius() - other.getRadius());
        //check if r1 encloses r2
        if( r2*r2 >= circleDistSq) {
            Circle biggest;
            if(getRadius() < other.getRadius()) {
                biggest = Circle.createCircle(other.getX(), other.getY(), other.getRadius());
            } else {
                biggest = Circle.createCircle(x, y, radius);
            }
            return biggest;
        }        
        double circleDist =  Math.sqrt(circleDistSq);
        double r =  (circleDist + getRadius() + other.getRadius()) / 2.;
        
        Circle newCircle = Circle.createCircle(getX(), getY(), (int) Math.ceil(r));
        if(circleDist > 0) {
            double f = ((r - getRadius()) / circleDist);                
            
            newCircle.setX(newCircle.getX() - ((int) Math.ceil(f * dx)));
            newCircle.setY(newCircle.getY() - ((int) Math.ceil(f * dy)));
        }
        
        return newCircle;
    }
    
    public String toString() {
        return "(" + x + "," + y + ") [" + radius +"]" + super.toString();
    }
}
