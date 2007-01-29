import greenfoot.World;
import greenfoot.GreenfootImage;

import java.awt.Color;

public class Space extends World
{
    /**
     * Creates a new world with 20x20 cells and
     * with a cell size of 50x50 pixels
     */
    public Space() {
        super(400,400,1);
        getBackground().setColor(java.awt.Color.BLACK);
        getBackground().fill();
        createStars(500);
        createRocketAndSpeeder();
    }
    
    public void createRocketAndSpeeder() {
        Rocket rocket = new Rocket();    
        addObject(rocket,0,0);
        rocket.setLocation(getWidth() - rocket.getWidth() -1 , getHeight()- rocket.getHeight() -1 );
        Speeder speeder = new Speeder();
        addObject(speeder,20,200);
        
        speeder.setListener(rocket);
        
    }
    
    private void createStars(int number) {
        GreenfootImage background = getBackground();
             
        for(int i=0; i < number; i++) {            
             int x = (int) (Math.random() * getWidth());          
             int y = (int) (Math.random() * getHeight());
             int color = 255 - (int) (Math.random() * 200);
             background.setColor(new Color(color,color,color));
             background.fillOval(x,y,1,1);
        }
    }
}