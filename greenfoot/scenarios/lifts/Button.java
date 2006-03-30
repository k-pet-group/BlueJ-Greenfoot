import greenfoot.Actor;
import greenfoot.GreenfootImage;

import javax.swing.ImageIcon;

public class Button extends Actor
{
    public static final int UP = 0;
    public static final int DOWN = 1;
    
    private GreenfootImage imageNone;
    private GreenfootImage imageUp;
    private GreenfootImage imageDown;
    private GreenfootImage imageUpDown;
    
    private boolean up;
    private boolean down;
    
    public Button()
    {
        imageUpDown = new GreenfootImage("images/button-up-down.jpg");
        imageUp = new GreenfootImage("images/button-up.jpg");
        imageDown =new GreenfootImage("images/button-down.jpg");
        imageNone = new GreenfootImage("images/button.jpg");
        
        setImage(imageNone);
        
        up = false;
        down = false;
    }

    public void act()
    {
        //here you can create the behaviour of your object
    }

    public void press(int direction)
    {
        change(direction, true);
    }
    
    public void clear(int direction)
    {
        change(direction, false);
    }
    
    public void change(int direction, boolean onOff)
    {
        if(direction == UP) {
            up = onOff;
        }
        else if(direction == DOWN) {
            down = onOff;
        }
        updateImage();
    }
    
    private void updateImage()
    {
        if(up && down) 
            setImage(imageUpDown);
        else if(up)
            setImage(imageUp);
        else if(down)
            setImage(imageDown);
        else
            setImage(imageNone);
    }
}