import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;


/**
 * A shape world
 */
public class ShapeWorld extends GreenfootWorld
{
    /**
     * Creates a world of size 600*400, 
     * with a cell size of one pixel.
     */
    public ShapeWorld() {
        super(600,400);
        getBackground().fill(Color.white);
    }
    
    /**
     * Builds a house.
     */
    public void buildHouse() {
        Triangle roof = new Triangle();
        roof.changeColor(Color.RED);
        roof.changeSize(200,100);
        roof.setLocation(100,100);        
        addObject(roof);
        
        
        Square wall = new Square();
        wall.changeColor(Color.BLUE);
        wall.changeSize(140);
        wall.setLocation(130,200);        
        addObject(wall);
        
        
        Square window = new Square();
        window.setLocation(150,220);        
        addObject(window);
        
    }
    
}
