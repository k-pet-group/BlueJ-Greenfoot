import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.awt.Image;
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
        setImage("button-up-down.jpg");
        imageUpDown = getImage().getImage();
        setImage("button-up.jpg");
        imageUp = getImage().getImage();
        setImage("button-down.jpg");
        imageDown = getImage().getImage();
        setImage("button.jpg");
        imageNone = getImage().getImage();
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
            setImage(new ImageIcon(imageUpDown));
        else if(up)
            setImage(new ImageIcon(imageUp));
        else if(down)
            setImage(new ImageIcon(imageDown));
        else
            setImage(new ImageIcon(imageNone));
    }
}