import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import java.awt.*;

public class Space extends GreenfootWorld
{
    /**
     * Creates a new world with 20x20 cells and
     * with a cell size of 50x50 pixels
     */
    public Space() {
        super(400,400,1,1);
        setBackgroundColor(java.awt.Color.BLACK);
        createStars(500);
        createRocketAndSpeeder();
    }
    
    public void createRocketAndSpeeder() {
        Rocket rocket = new Rocket();        
        rocket.setLocation(getWorldWidth() - rocket.getImage().getIconWidth(), getWorldHeight()- rocket.getImage().getIconHeight());
        addObject(rocket);
        
        Speeder speeder = new Speeder();
        speeder.setLocation(0,0);
        addObject(speeder);
        
        speeder.setListener(rocket);
        
    }
    
    private void createStars(int number) {
        Graphics2D canvas = getCanvas();
             
        for(int i=0; i < number; i++) {
            
             int x = (int) (Math.random() * getWorldWidth());          
             int y = (int) (Math.random() * getWorldHeight());
             int color = 255 - (int) (Math.random() * 200);
             canvas.setColor(new Color(color,color,color));
             canvas.fillOval(x,y,1,1);
        }
    }
}