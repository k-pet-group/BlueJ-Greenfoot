// WARNING: This file is auto-generated and any changes to it will be overwritten
import java.util.*;
import greenfoot.*;
import java.util.List;
import java.util.Set;

/**
 * This class defines a turtle. Turtles like lettuce 
 * (very yummy, especially the green ones).
 */
public class Turtle extends Actor
{
    private int lettucesEaten;

    /**
     * Initialise the turtle
     */
    public Turtle()
    {
        lettucesEaten = 0;
    }

    /**
     * Act - do whatever the turtle wants to do. This method is called whenever
     * the 'Act' or 'Run' button gets pressed in the environment. 
     */
    public void act()
    {
        move(5);
        checkKeyPress();
        lookForLettuce();
    }

    /**
     * Check whether a control key on the keyboard has been pressed.
     * If it has, react accordingly.
     */
    public void checkKeyPress()
    {
        if (Greenfoot.isKeyDown("right")) {
            turn(4);
        }
        if (Greenfoot.isKeyDown("left")) {
            turn(-4);
        }
    }

    /**
     * Check whether we have stumbled upon a lettuce.
     * If we have, eat it. If not, do nothing. If we have eaten eight lettuces, we win.
     */
    public void lookForLettuce()
    {
        if (isTouching(Lettuce.class)) {
            removeTouching(Lettuce.class);
            Greenfoot.playSound("slurp.wav");
            
            lettucesEaten = lettucesEaten + 1;
            getWorld().showText("Lettuces: " + lettucesEaten, 100, 30);
            if (lettucesEaten == 8) {
                Greenfoot.playSound("fanfare.wav");
                Greenfoot.stop();
            }
        }
    }
}
