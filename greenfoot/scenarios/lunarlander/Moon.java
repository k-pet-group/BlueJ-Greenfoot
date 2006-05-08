import greenfoot.World;
import greenfoot.Actor;
import java.awt.Color;

public class Moon extends World
{

    private double gravity = 1.6;
    private Color landingColor = Color.WHITE;
    private Color spaceColor = Color.BLACK;
    
    /**
     * Creates a new world with 20x20 cells and
     * with a cell size of 10x10 pixels
     */
    public Moon() {
        super(600,600,1);
        setBackground("images/moon.png");
        addObject(new Lander(), 326, 100);
    }
    
    public double getGravity()  {
        return gravity;
    }
    
    public Color getLandingColor() {
        return landingColor;
    }
    
    
    public Color getSpaceColor() {
        return spaceColor;
    }
 
}