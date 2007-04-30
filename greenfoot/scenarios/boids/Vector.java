/**
 * A 2D vector.
 * 
 * @author Poul Henriksen 
 * @version 2.0
 */
public class Vector
{
    double dx = 0;
    double dy = 0;
    double direction = 0; //in degrees
    double length;
   
    /**
     * Creates a vector with length=0
     */
    public Vector()
    {
    }
    
    /**
     * Creates a vector with the given x and y-components.
     */
    public Vector(double x, double y)
    {
        this.dx = x;
        this.dy = y;
        this.direction = Math.toDegrees(Math.atan2(dy, dx));
        this.length = Math.sqrt(dx*dx+dy*dy);
    }

    /**
     * Set the direction of this vector.
     */
    public Vector setDirection(double direction) {
        this.direction = direction;
        dx = length * Math.cos(Math.toRadians(direction));
        dy = length * Math.sin(Math.toRadians(direction));
        return this;
    }
   
    /**
     * Set the length of this vector. 
     */
    public Vector setLength(double l) 
    {
        this.length = l;
        dx = length * Math.cos(Math.toRadians(direction));
        dy = length * Math.sin(Math.toRadians(direction));   
        return this;
    }
    
    /**
     * Add other vector to this vector.
     */
    public Vector add(Vector other) {
        dx += other.dx;
        dy += other.dy;    
        this.direction = Math.toDegrees(Math.atan2(dy, dx));
        this.length = Math.sqrt(dx*dx+dy*dy);
        return this;
    }   
    
    /**
     * Subtract other vector to this vector.
     */
    public Vector subtract(Vector other) {
        dx -= other.dx;
        dy -= other.dy;    
        this.direction = Math.toDegrees(Math.atan2(dy, dx));
        this.length = Math.sqrt(dx*dx+dy*dy);
        return this;
    }   
    
    /**
     * Get the x-component of this vector.
     */
    public double getX() {
        return dx;
    }
     
    /**
     * Get the y-component of this vector.
     */
    public double getY() {
        return  dy;
    }
    
    /**
     * Set the x-component of this vector.
     */
    public void setX(double x) {
        dx = x;
        this.direction = Math.toDegrees(Math.atan2(dy, dx));
        this.length = Math.sqrt(dx*dx+dy*dy);
    }
        
    /**
     * Set the y-component of this vector.
     */
    public void setY(double y) {
        dy = y;
        this.direction = Math.toDegrees(Math.atan2(dy, dx));
        this.length = Math.sqrt(dx*dx+dy*dy);
    }
    
    /**
     * Get the direction of this vector (in degrees).
     */
    public double getDirection() {
        return direction;
    }
    
    /**
     * Get the length of this vector.
     */
    public double getLength() {
        return length;
    }
    
    /**
     * Divide the length of thes vector with the given value.
     */
    public Vector divide(double v) {    
        if(v != 0) {
            dx = dx / v;
            dy = dy / v;
            length = length / v;
        }
        return this;
    }
      
    /**
     * Multiply the length of thes vector with the given value.
     */
    public Vector multiply(double v) {
        dx = dx * v;
        dy = dy * v;
        length = length * v;
        return this;
    }        
    
    /**
     * Create a copy of this vector.
     */
    public Vector copy() {
        Vector copy = new Vector();
        copy.dx = dx;
        copy.dy = dy;
        copy.direction = direction;
        copy.length = length;
        return copy;
    }    
        
    public String toString() {
        return "" + dx + "," + dy ;
    }
}