// WARNING: This file is auto-generated and any changes to it will be overwritten
import java.util.*;
import greenfoot.*;

/**
 * A snake in my game that can eat turtles.
 */
public class Snake extends Actor
{

    /**
     * Do whatever snakes do.
     */
    public void act()
    {
        move(5);
        turnAtEdge();
        randomTurn();
        tryToEatTurtle();
    }

    /**
     * Check whether we are at the edge of the world. If we are, turn a bit.
     * If not, do nothing.
     */
    public void turnAtEdge()
    {
        if (isAtEdge()) {
            turn(17);
        }
    }

    /**
     * Randomly decide to turn from the current direction, or not. If we turn
     * turn a bit left or right by a random degree.
     */
    public void randomTurn()
    {
        if (Greenfoot.getRandomNumber(100) > 90) {
            turn(Greenfoot.getRandomNumber(90) - 45);
        }
    }

    /**
     * Try to pinch a turtle. That is: check whether we have stumbled upon a turtle.
     * If we have, remove the turtle from the game, and stop the program running.
     */
    public void tryToEatTurtle()
    {
        if (isTouching(Turtle.class)) {
            removeTouching(Turtle.class);
            Greenfoot.playSound("au.wav");
            Greenfoot.stop();
        }
    }
}
