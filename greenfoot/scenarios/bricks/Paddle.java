import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import greenfoot.Image;

import java.awt.Color;

public class Paddle extends GreenfootObject
{
    public Paddle()
    {
        //setImage("name of the image file");
        Image pic = new Image(60, 10);
        pic.fill(Color.GREEN);
        setImage(pic);
    }
    
    public Paddle(int x, int y)
    {
        this();
        setLocation(x,y);
    }

    public void setLocation(int x, int y)
    {
        y = BrickWorld.SIZEY - 20;
        super.setLocation(x,y);
    }

    public void act()
    {
        //here you can create the behaviour of your object
    }
}
