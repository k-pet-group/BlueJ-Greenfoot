import greenfoot.GreenfootObject;
import greenfoot.Image;

import javax.swing.ImageIcon;

public class Button extends GreenfootObject
{
    public static final int UP = 0;
    public static final int DOWN = 1;
    
    private Image imageNone;
    private Image imageUp;
    private Image imageDown;
    private Image imageUpDown;
    
    private boolean up;
    private boolean down;
    
    public Button()
    {
        imageUpDown = new Image("button-up-down.jpg");
        imageUp = new Image("button-up.jpg");
        imageDown =new Image("button-down.jpg");
        imageNone = new Image("button.jpg");
        
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