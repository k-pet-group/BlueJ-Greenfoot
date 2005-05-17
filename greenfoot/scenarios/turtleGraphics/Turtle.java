
import greenfoot.GreenfootObject;
import greenfoot.Image;
import greenfoot.Utilities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class Turtle extends GreenfootObject
{

    private boolean penDown;
    private String color = "black";
    private double direction;
    private double x;
    private double y;
    private final static double MAX_ANGLE = 360;
    private boolean initialized = false;

    public Turtle()
    {
        setImage("turtle.gif");
    }
    
        
    public void act()
    {
        //here you can create the behaviour of your object
    }

    /**
     * Turns the turtle.
     * 
     */
    public void turn(double degrees)
    {
        if (!initialized) {
            initialize();
        }
        direction = direction + degrees;
        if (direction > MAX_ANGLE) {
            direction = direction % MAX_ANGLE;
        }
        draw();
    }
    
    /**
     * Moves the turtle to the given position.
     */
    public void moveTo(double newX, double newY)
    {
        if (!initialized) {
            initialize();
        }
        if (penDown) {
            drawLine(x, y, newX, newY);
        }
        x = newX;
        y = newY;
        setLocation((int) Math.floor(x), (int) Math.floor(y));
    }

    /**
     * Moves the turtle the given distance.
     */
    public void move(double distance)
    {
        if (!initialized) {
            initialize();
        }
        double directionRad = Math.toRadians(direction);
        double xDist = distance * Math.cos(directionRad);
        double yDist = distance * Math.sin(directionRad);

        moveTo(x + xDist, y + yDist);
    }

    public void penUp()
    {
        penDown = false;
        setImage("turtle.gif");
        draw();
    }

    public void penDown()
    {
        penDown = true;
        draw();
    }

    public void setColor(String newColor)
    {
        color = newColor;
        draw();
    }
    
    
    /**
     * We need to make sure that our own representaion of the location is the
     * same as the GreenfootWorld
     */
    private void initialize()
    {
        initialized = true;
        x = getX();
        y = getY();
    }

    private void drawLine(double x1, double y1, double x2, double y2)
    {
        Image image = getWorld().getBackground();
        Color awtColor = decode(color);

        image.setColor(awtColor);
        int xOffset = getImage().getWidth() / 2;
        int yOffset = getImage().getHeight() / 2;
        image.drawLine((int) Math.ceil(x1) + xOffset, (int) Math.ceil(y1) + yOffset, (int) Math.ceil(x2) + xOffset,
                (int) Math.ceil(y2) + yOffset);
    }
    
    private void draw()
    {
        if (penDown) {
            drawPen();
        }
        setRotation(direction);
        Utilities.repaint();
    }

    private void drawPen()
    {
        Image image = getImage();
        double halfWidth = image.getWidth() / 2.;
        double halfHeight = image.getHeight() / 2.;
        int penWidth = (int) halfWidth / 2;
        int penHeight = (int) halfHeight / 2;
        int penX = (int) (halfWidth - penWidth / 2);
        int penY = (int) (halfHeight - penHeight / 2);
        Color awtColor = decode(color);
        image.setColor(awtColor);
        image.fillOval(penX, penY, penWidth, penHeight);
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

}