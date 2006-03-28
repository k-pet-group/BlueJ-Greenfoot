import greenfoot.World;
import greenfoot.Actor;
public class Naive extends Boid
{

    private static final double MAX_SPEED = 5;
    private static final int FLOCK_DISTANCE = 100;
    
    private static double separationFactor = 1;
    private static double alignmentFactor = 0.5;
    private static double cohesionFactor = 8;
    
    public Naive()
    {
        //setImage("name of the image file");
    }
  
    
    public void doStuff() {
        separate();
        cohesion();
        align();            
            
        double speed = vectorPlan.getLength(); 
        
        if(speed > MAX_SPEED) {            
           vectorPlan.shorten(speed/MAX_SPEED);
        }           
    }
  
   private void separate() {
        Vector flockVector = getFlockRepulsion(FLOCK_DISTANCE);  
        flockVector.enlarge(separationFactor);
        vectorPlan.add(flockVector);
    }    
    
    private void cohesion() {
        Vector attraction = getFlockAttraction(FLOCK_DISTANCE);
        attraction.enlarge(cohesionFactor);
         vectorPlan.add(attraction);
    }
    
    private void align() {
        Vector flockDirection = getFlockDirection(FLOCK_DISTANCE);
          
        flockDirection.enlarge(alignmentFactor);
   
        vectorPlan.add(flockDirection);
    }
    
     public static void setSeparationFactor(double factor) {
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
    }
    

}