import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

public class BackwardsFlag extends DirectionFlag
{
    private int direction = 4;
    
    public BackwardsFlag()
    {
    }

    public void act()
    {
        //here you can create the behaviour of your object
    }
    
    public int changeDirection(int direction)
    {
        if (randomCheck()) {
            return direction += this.direction;
        }
        else {
            return 0;
        }
    }
}