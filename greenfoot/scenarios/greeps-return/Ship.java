import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
 
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.awt.Font;

/**
 * A space ship. It comes from space, lands, and releases some Greeps into the world.
 * 
 * @author Michael Kolling
 * @author Davin McCall
 * @author Poul Henriksen
 * @version 2.0
 */
public class Ship extends Actor
{
    
    /**
     * Method that creates the Greeps. 
     * You can change the class that objects are created from here.
     */
    private Greep createGreep() 
    {
        if(teamNumber == 1) {
            return new MyGreep(this);
        }
        else {
            return new SimpleGreep(this);
        }        
    }
    
    private int totalPassengers = 20;     // Total number of passengers in this ship.
    private int passengersReleased = 0;   // Number of passengers that left so far.
    private Counter foodCounter;          // Tomato counter 
    private int targetPosition;           // The vertical position for landing
    private int stepCount = 0;
    private boolean hatchOpen = false;    // Whether the greeps can deploy yet    
    private int[] dataBank = new int[1000];    // Ship's databank. Holds a large amount of information.    
    private int teamNumber; // Team number. Should be 1 or 2.    
    private int direction = 1; // 1 is positive y-direction, -1 is negative.    
    private String greepName; // Name of the Greeps produced by this ship.    
    
    /**
     * Create a space ship. The parameter specifies at what height to land.
     */
    public Ship(String imageName, int position, int teamNumber)
    {
        targetPosition = position;
        this.teamNumber = teamNumber;
        GreenfootImage im = new GreenfootImage(imageName);
        greepName = createGreep().getName();
        setImage(im);
    }
    
    /**
     * Find out which direction we are moving in.
     */
    public void addedToWorld(World w) {
        if(getY() > targetPosition) {
            direction = -1;
        }
        else {
            direction = 1;
        }
    }
    
    /**
     * Let the ship act: move or release greeps.
     */
    public void act()
    {
        if(inPosition() && hatchOpen) {
            if(! isEmpty()) {
                releasePassenger();
            }
        }
        else {
            move();
        }
    }
    
    /**
     * True if all passengers are out.
     */
    public boolean isEmpty()
    {
        return passengersReleased == totalPassengers;
    }
    
    /**
     * Move the ship down or up (for movement before landing).
     */
    public void move()
    {      
        int dist = (targetPosition - getY())  / 16;
        
        if(dist == 0) {
            dist = direction;
        }
        
        setLocation(getX(), getY() + dist);        
        if(inPosition()) {
            // Make sure we are at exactly the right target position
            setLocation(getX(), targetPosition);
        }
    }
    
    /**
     * True if we have reached the intended landing position.
     */
    public boolean inPosition()
    {
        int diff = (getY() - targetPosition) * direction ;
        return diff >= 0;
    }
    
    /**
     * Open the ship's hatch. This allows the greeps to come out.
     */
    public void openHatch()
    {
        hatchOpen = true;
    }
    
    /**
     * Possibly: Let one of the passengers out. Passengers appear at intervals, 
     * so this may or may not release the passenger.
     */
    private void releasePassenger()
    {
        if(passengersReleased < totalPassengers) {
            stepCount++;
            if(stepCount == 10) {
                Greep newGreep = createGreep();
                getWorld().addObject(newGreep, getX(), getY() + 30);
                passengersReleased++;
                stepCount = 0;               
            }
        }
    }

    /**
     * Record that we have collected another tomato.
     */
    public void storeTomato()
    {       
        if(foodCounter == null) {
            foodCounter = new Counter("Tomatoes: ");
            int x = getX();
            int y = getY() + getImage().getHeight() / 2 + 10;
            if(y >= getWorld().getHeight()) {
                y = getWorld().getHeight();    
            }

            getWorld().addObject(foodCounter, x, y);
        }        
        foodCounter.increment();
    }
    
    /**
     * Return the current count of tomatos collected.
     */
    public int getTomatoCount()
    {
        if(foodCounter == null)
            return 0;
        else
            return foodCounter.getValue();
    }
    
    /**
     * Get the ship's data bank array. 
     */
    public int[] getData()
    {
        return dataBank;
    }
    
    /**
     * Return the author name of this ship's Greeps.
     */
    public String getGreepName() 
    {               
        return greepName;
    }
    
    public boolean isTeamTwo() 
    {
        return teamNumber == 2;    
    }   
}
