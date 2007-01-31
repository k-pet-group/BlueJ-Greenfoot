import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

public class DirectionFlag extends Actor
{
    protected int random = 12;
    protected int direction = 0;
    
    public DirectionFlag()
    {
    }

    public void act()
    {
        //here you can create the behaviour of your object
    }
    
    protected boolean randomCheck()
    {
        return Greenfoot.getRandomNumber(random) == 0;
    }
    
    public int changeDirection(int direction)
    {
        if (randomCheck()) {
            return direction + (this.direction-direction);
        }
        else {
            return direction;
        }
    }
}