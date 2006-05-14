import greenfoot.World;
import greenfoot.Actor;

import java.awt.*;

public class Sky extends World
{
    private static int WIDTH = 800;
    private static int HEIGHT = 600;
    
   
    public Sky() {
        super(WIDTH,HEIGHT,1);
        getBackground().setColor(new Color(90,90,255));
        getBackground().fill();
        populateNaive(20);
      
      /*  Cohesion c = new Cohesion();
        c.setLocation(0,HEIGHT-1);
        addObject(c);
        Alignment a = new Alignment();
        a.setLocation(32,HEIGHT-1);
        addObject(a);
        Separation s = new Separation();
        s.setLocation(64,HEIGHT-1);

        addObject(s);*/
    }
    
    public void populateNaive(int number) {
        for(int i=0; i < number; i++) {
            
             int x = (int) (Math.random() * getWidth());          
             int y = (int) (Math.random() * getHeight());
             Boid b = new Naive();
             addObject(b, x, y);
        }
    }
    
}