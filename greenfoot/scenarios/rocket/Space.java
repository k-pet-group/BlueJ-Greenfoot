import greenfoot.GreenfootWorld;
import greenfoot.Image;

import java.awt.Color;

public class Space extends GreenfootWorld
{
    /**
     * Creates a new world with 20x20 cells and
     * with a cell size of 50x50 pixels
     */
    public Space() {
        super(400,400);
        getBackground().fill(java.awt.Color.BLACK);
        createStars(500);
        createRocketAndSpeeder();
    }
    
    public void createRocketAndSpeeder() {
        Rocket rocket = new Rocket();        
        rocket.setLocation(getWidth() - rocket.getWidth(), getHeight()- rocket.getHeight());
        addObject(rocket);
        
        Speeder speeder = new Speeder();
        speeder.setLocation(0,0);
        addObject(speeder);
        
        speeder.setListener(rocket);
        
    }
    
    private void createStars(int number) {
        Image background = getBackground();
             
        for(int i=0; i < number; i++) {            
             int x = (int) (Math.random() * getWidth());          
             int y = (int) (Math.random() * getHeight());
             int color = 255 - (int) (Math.random() * 200);
             background.setColor(new Color(color,color,color));
             background.fillOval(x,y,1,1);
        }
    }
}