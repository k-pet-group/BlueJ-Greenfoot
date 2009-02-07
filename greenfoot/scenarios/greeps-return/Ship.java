import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
 
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.awt.Font;

/**
 * A space ship. It comes from space, lands, and releases some Greeps into the world.
 * 
 * @author Michael Kolling
 * @version 1.0
 */
public class Ship extends Actor
{
    
    private int totalPassengers = 20;     // Total number of passengers in this ship.
    private int passengersReleased = 0;   // Number of passengers that left so far.
    private Counter foodCounter;          // Tomato counter 
    private int targetPosition;           // The vertical position for landing
    private int stepCount = 0;
    private boolean hatchOpen = false;    // Whether the greeps can deploy yet
    
    private Class inhabitantClass;        // What type of inhabitant this ship carries.
    private int[] dataBank = new int[1000];    // Ship's databank. Holds a large amount of information.
    
    
    /**
     * Create a space ship. The parameter specifies at what height to land.
     */
    public Ship(String imageName, int position, Class inhabitant)
    {
        targetPosition = position;
        inhabitantClass = inhabitant;
        GreenfootImage im = new GreenfootImage(imageName);
        setImage(im);
    }

    /**
     * Get the class of inhabitants of this ship.
     */
    public Class getInhabitantClass()
    {
        return inhabitantClass;
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
     * Move the ship down (for movement before landing).
     */
    public void move()
    {
        int dist = Math.min((targetPosition - getY()) / 8, 8) + 1;
        setLocation(getX(), getY() + dist);
    }
    
    /**
     * True if we have reached the intended landing position.
     */
    public boolean inPosition()
    {
        return getY() >= targetPosition;
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
                try {
                    Greep newGreep = (Greep) inhabitantClass.newInstance();
                    newGreep.setShip(this);
                    getWorld().addObject(newGreep, getX(), getY() + 30);
                    passengersReleased++;
                    stepCount = 0;
                }
                catch (InstantiationException ie) {
                    throw new Error(ie);
                }
                catch (IllegalAccessException iae) {
                    throw new Error(iae);
                }
            }
        }
    }

    /**
     * Record that we have collected another tomato.
     */
    public void storeTomato(Greep cr)
    {
        if(cr.removeTomato() == false) 
            return; // did not have a tomato
            
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
    public String getAuthor() 
    {        
        Class greepClass = getInhabitantClass();
        try {
            Method authorMethod = greepClass.getMethod("getAuthorName", new Class[]{});              
            return (String) authorMethod.invoke((Object) null, new Object[]{});
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return "";
    }
}
