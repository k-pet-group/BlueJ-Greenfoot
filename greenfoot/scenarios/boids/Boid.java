import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.Iterator;
import java.util.List;

import java.util.Collection;
import java.util.ArrayList;
import java.awt.Point;

public abstract class Boid extends GreenfootObject
{

 
    
    private double startSpeed = 1.0;
    
    private double x, y;
    

    
    private boolean initialised = false;
    
    private Vector vector = new Vector((Math.random()-0.5)*startSpeed  ,(Math.random()-0.5)*startSpeed );
  
    protected Vector vectorPlan = (Vector) vector.clone();
    
    private boolean planning = true;
    
    public class Vector {
        private double x;
        private double y;
        
        public Vector(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public void setX(double x) {
            this.x = x;
        }
        
        
        public void setY(double y) {
            this.y = y;
        }
        
        public void setDirection(double angle) {
            double len = getLength();
            x = Math.cos(angle) * len;
            y = Math.sin(angle) * len ;
        }
        
        public double getX() {
            return x;
        }
        public double getY() {
            return y;
        }
        
        /**
         * Returns the direction. thisis a value between -Pi and Pi
         */
        public double getDirection() {
             return Math.atan2(y,x);
        }
        
        public double getLength() {
            if(x==0  || y==0) {
                return 0;
            }
            return Math.sqrt(x*x+y*y);   
        }
        
        public void add(Vector other) {
            this.x += other.getX();
            this.y += other.getY();
        }
        
        public void add(double dx, double dy) {
            this.x += dx;
            this.y += dy;
        }
        
        public void shorten(double factor) {
            if(factor != 0) {
                x = x/ factor;
                y = y/ factor;
            }
        }
        
        public void enlarge(double factor) {
            x = x * factor;
            y = y * factor;
        }
        
       
        
        public Object clone() {
            return new Vector(x,y);
        }
        
        public String toString() {
            return "" + x + "," +y;
        }
            
    }
    
    public Boid()  {
        setImage("bird.png");
        setRotation(90 + 180 * getVector().getDirection() / Math.PI);
    }
    
    public abstract void doStuff();
    
    public void act()
    {   
        if(!initialised) {
            x = getX();
            y = getY();
            initialised = true;
        }
        
         
       // Vector flockVector = getFlockVector(flock);
       // double flockDirection  = getFlockDirection(flock);
     
        if(planning) {
            
            doStuff();   
            
            planning = false;
        }
        else {
            
            vector.setX(vectorPlan.getX());            
            vector.setY(vectorPlan.getY());
            
            setRotation(90 + 180 * getVector().getDirection() / Math.PI);
            moveForward();
            planning = true;
        }
      
    }
    
    
    private void moveForward() {
        double speed = getVector().getLength();     
        
        if(speed > 0) {
            x += getVector().getX() ;
            y += getVector().getY() ;  
        }
        limit();
        
    //    System.out.println("" + x + " , " + y + " rot: " + getRotation());
        
        setLocation((int) Math.floor(x), (int) Math.floor(y));
    }
    
    public void avoidBorders() {
         //if we see a border, we mo
         if(x<=20) {
             vectorPlan.getDirection();
         //  vectorPlan.setX(10);// -10 * vectorPlan.getX());
           x=0;
        }
        if(x>=getWorld().getWidth()) {
         //  vectorPlan.setX(-10) ;//-10 * vectorPlan.getX());
           x=getWorld().getWidth()-2;
        }
        if(y<=0) {
            //vectorPlan.setY(10);//-10 * vectorPlan.getY());                
            y=0;
        }
        if(y>=getWorld().getHeight()) {
          //  vectorPlan.setY(-10);//-10 * vectorPlan.getY());                
            y=getWorld().getHeight() -2 ;
        }
    }
    
    private void limit() {
        if(x<=0) {
         //  vectorPlan.setX(10);// -10 * vectorPlan.getX());
           x=0;
        }
        if(x>=getWorld().getWidth()) {
         //  vectorPlan.setX(-10) ;//-10 * vectorPlan.getX());
           x=getWorld().getWidth()-2;
        }
        if(y<=0) {
            //vectorPlan.setY(10);//-10 * vectorPlan.getY());                
            y=0;
        }
        if(y>=getWorld().getHeight()) {
          //  vectorPlan.setY(-10);//-10 * vectorPlan.getY());                
            y=getWorld().getHeight() -2 ;
        }
        
    }
    
   /* public static void setSeparationFactor(double factor) {
        separationFactor = factor;
    }
    
    public static void setCohesionFactor(double factor) {
        cohesionFactor = factor;
    }
        
    public static void setAlignmentFactor(double factor) {
        alignmentFactor = factor;
    }
    
    public static double getSeparationFactor() {
        return separationFactor;
    }
    
    public static double getCohesionFactor() {
        return cohesionFactor;
    }
        
    public static double getAlignmentFactor() {
        return alignmentFactor;
    }*/
    
    
    public Vector getVector() {
        return vector;
    }
    
    public Vector getFlockRepulsion(double distance) {
        Iterator flock = getNeighbours(distance).iterator();       
        int nBirds = 0;
        
        Vector flockVector = new Vector(0,0);
        while(flock.hasNext()) {
             Boid other = (Boid) flock.next();
             if(other != this) {
                 double dx = getX() - other.getX();
                 if(dx!=0) {
                     dx = distance/dx;        
                 } else {
                     dx = distance;
                 }
                 double dy = getY() - other.getY();                 
                 if(dy!=0) {
                     dy = distance/dy;        
                 } else {
                     dy = distance;
                 }   
                 
                 flockVector.add(dx , dy);
                 nBirds++;
             }
        }
        flockVector.shorten(nBirds); 
        return flockVector;
    }
   
    /**
     * Borders are considered obstacles, and exerts a very strong force when a boid is getting close to a border.
     * This should be used to avoid getting stuck at the borders
     */
  /*  public Vector getObstacleRepulsion() {
        
    }*/
    
   
    public Vector getFlockAttraction(double distance) {
        Iterator flock = getNeighbours(distance).iterator();
       
        int nBirds = 1; //this bird is the first one
        double xCenter = x;        
        double yCenter = y;
        
        while(flock.hasNext()) {
             Boid other = (Boid) flock.next();
             if(other != this) {
                xCenter += other.getX();
                yCenter += other.getY();
                nBirds++;
             }
        }
       
        xCenter /= nBirds;        
        yCenter /= nBirds;
        
        Vector attraction = new Vector(xCenter - getX(), yCenter -getY());    
        attraction.shorten(distance);
        return attraction;
    }
    
    
    public Vector getFlockDirection(double distance) {
        Iterator flock = getNeighbours(distance).iterator();
       
        int nBirds = 1;
        Vector flockDirection = (Vector) getVector().clone();
         
        while(flock.hasNext()) {
             Boid other = (Boid) flock.next();
             if(other != this) {
                flockDirection.add(other.getVector());
                nBirds++;
             }
        }
        flockDirection.shorten(nBirds);
        return flockDirection;
    }
    
 
    private Collection getNeighbours(double distance) {
        return getWorld().getObjectsInRange(getX(),getY(),distance, Boid.class);
        //TODO for greenfoot it would be nice to be able to get obecjts within a certain radius
        //TODO and to get all obejcts of a certain type in the world.
      /*  Iterator objects = getWorld().getObjects();
         

        List neighbours = new ArrayList();
        while(objects.hasNext()) {
            Object o = objects.next();
            if(o instanceof Boid && o != this) {
                Boid b = (Boid) o;
                if(distance(b) < distance) {
                    neighbours.add(b);
                }
            }
        }
        return neighbours;*/

    }
    
    
    
    private double distance(Boid other) {
        int dx = other.getX() - getX();
        int dy = other.getY() - getY();
        return Math.sqrt(dx*dx+dy*dy);
       
    }


}