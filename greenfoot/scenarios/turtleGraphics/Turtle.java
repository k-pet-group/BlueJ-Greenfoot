import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class Turtle extends GreenfootObject
{
    
    private boolean penIsDown;
    private String color ="black";
    
    private double direction;
    
    private double x;
    private double y;
    
    private final static double MAX_ANGLE = 360; 
    private boolean initialized = false;
    
    public Turtle()
    {
        setImage("turtle.gif");     
    }
    
    /**
     * We need to make sure that our own representaion 
     * of the location is the same as the GreenfootWorld
     */ 
    private void  initialize() {
        initialized=true;
        x = getX();
        y = getY();
    }
    
    /**
     * Turns the turtle.
     * 
     * @param degrees Degrees to turn the turtle.
     */ 
    public void turn(double degrees) {
        if(!initialized) {
            initialize();
        }
        direction = direction + degrees; 
        if(direction > MAX_ANGLE) {
            direction = direction % MAX_ANGLE;
        }
        draw();
    }
    
    private void draw() {           
        if(penIsDown) {          
          drawPen();
        }
        setRotation(direction);
        update();
    }
    
    private void drawPen() {
        ImageIcon image = getImage();
        Graphics g2 = image.getImage().getGraphics();
        double halfWidth = image.getIconWidth()/2.;
        double halfHeight = image.getIconHeight()/2.;    
    
        int penWidth = (int) halfWidth/2;
        int penHeight = (int) halfHeight/2;
        int penX = (int) (halfWidth - halfWidth/4);
        int penY = (int) (halfHeight - halfHeight/4);
        Color awtColor = decode(color);
        g2.setColor(awtColor);
        g2.fillOval(penX,
                    penY,
                    penWidth,
                    penHeight);                 
    }
    
    
    /**
     * Moves the turtle to the given position.
     */ 
    public void moveTo(double newX, double newY) {
        if(!initialized) {
            initialize();
        }
        if(penIsDown) {
            drawLine(x,y,newX, newY);
        }
        x = newX;
        y = newY;
        setLocation((int) Math.floor(x), (int) Math.floor(y));
    }
    
    public void drawLine(double x1, double y1, double x2, double y2) {
        Graphics2D g = getWorld().getCanvas();
        Color awtColor = decode(color);
        
            g.setColor(awtColor);
            int xOffset = getImage().getIconWidth()/2;
            int yOffset = getImage().getIconHeight()/2;
            g.drawLine((int) Math.ceil(x1) + xOffset,
                    (int) Math.ceil(y1) + yOffset,
                    (int) Math.ceil(x2) + xOffset,
                    (int) Math.ceil(y2) + yOffset);
    }
    
    /**
     * Moves the turtle to the given distance.
     */  
    public void move(double distance) {
        if(!initialized) {
            initialize();
        }
        double directionRad = Math.toRadians(direction);
        double xDist = distance*Math.cos(directionRad);
        double yDist = distance*Math.sin(directionRad);     
        
        moveTo(x+xDist, y+yDist);
    }
    
    
    
    public void penUp() {
        penIsDown = false;
        setImage("turtle.gif");
        draw();
    }
    
    public void penDown() {
        penIsDown = true;
        draw();
    }
    
    public void setColor(String newColor) {
        color = newColor;
        draw();
    }
    
    private Color decode(String colorString) {
        if(colorString.equals("red"))
            return Color.red;
        else if(colorString.equals("black"))
            return  Color.black;
        else if(colorString.equals("blue"))
            return Color.blue;
        else if(colorString.equals("yellow"))
            return  Color.yellow;
        else if(colorString.equals("green"))
            return Color.green;
        else if(colorString.equals("magenta"))
            return Color.magenta;
        else if(colorString.equals("white"))
            return Color.white;
        else
            return Color.black;
    }
    
    public void act()
    {
        //here you can create the behaviour of your object
    }
    
}
