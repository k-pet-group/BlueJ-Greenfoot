import greenfoot.World;
import greenfoot.Actor;

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;


/**
 * A shape world
 */
public class ShapeWorld extends World
{
    /**
     * Creates a world of size 600*400, 
     * with a cell size of one pixel.
     */
    public ShapeWorld() {
        super(600,400, 1);
        getBackground().setColor(Color.white);
        getBackground().fill();
    }
    
    /**
     * Builds a house.
     */
    public void buildHouse() {
        Triangle roof = new Triangle();
        roof.changeColor(Color.RED);
        roof.changeSize(200,100);      
        addObject(roof,100,100);
        
        
        Square wall = new Square();
        wall.changeColor(Color.BLUE);
        wall.changeSize(140);   
        addObject(wall,130,200);
        
        
        Square window = new Square();      
        addObject(window,150,220);
        
    }
    
}
