import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * ... the final frontier
 * 
 * @author Poul Henriksen
 */
public class Space extends World
{
    public Space() {
        super(600, 400, 1);
        getBackground().setColor(Color.BLACK);
        getBackground().fill();
        createStars(300);
        
        addObject(new Rocket(), 300,200);
        addObject(new Asteroid(), 100 ,100);
        addObject(new Asteroid(), 500 ,300);        
    }
        
    private void createStars(int number) {
        GreenfootImage background = getBackground();             
        for(int i=0; i < number; i++) {            
             int x = (int) (Math.random() * getWidth());          
             int y = (int) (Math.random() * getHeight());
             int color = 255 - (int) (Math.random() * 220);
             background.setColor(new Color(color,color,color));
             background.fillOval(x,y,1,1);
        }
    }
}