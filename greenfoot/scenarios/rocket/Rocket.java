import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
public class Rocket extends GreenfootObject
{
    private double velocity;
    private double power;
    
    private static double G = 9.82;
    private static double MAX_POWER = 2.*G;
    
    private static double MAX_VELOCITY = 10;
    
    public Rocket()
    {
        setImage("rocket.gif");
    }

    public void act()
    {        
        velocity = velocity + (-G + power)/10;
        if(velocity > MAX_VELOCITY) {
            velocity = MAX_VELOCITY;    
        }
            
        setLocation(getX(), (int) (getY() - velocity));        
    }
  
    public void setPower(double power) {
        this.power = power;
        if(power > MAX_POWER) {
            power = MAX_POWER;
        }
    }
    

}