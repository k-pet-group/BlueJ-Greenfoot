import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
public class Speeder extends GreenfootObject
{
    private Rocket rocket;
    
    public Speeder()
    {
        setImage("speeder.gif");
    }
    
    public void act()
    {
    //here you can create the behaviour of your object
    }
    
    public void setListener(Rocket rocket) {
        this.rocket = rocket;
    }
    
    public void setLocation(int x, int y) {
        if(rocket!=null) {
            rocket.setPower(getY() / 10.);   
        }
        super.setLocation(getX(),y);   
    }
    
    
}