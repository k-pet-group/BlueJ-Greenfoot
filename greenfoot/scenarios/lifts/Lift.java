import greenfoot.Actor;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;

import javax.swing.ImageIcon;

public class Lift extends Actor
{
    private static final Random randomizer = Building.getRandomizer();

    private static final int ST_UP = 0;
    private static final int ST_DOWN = 1;
    private static final int ST_STOPPED = 2;
    private static final int ST_OPEN = 3;

    private int status;
    private int people;
    private GreenfootImage openImage;
    private GreenfootImage emptyImage;
    private GreenfootImage closedImage;
    private GreenfootImage personImage;
    
    public Lift()
    {
        personImage = new GreenfootImage("images/person.gif");
        openImage =  new GreenfootImage("images/lift-open.jpg");
        emptyImage =  new GreenfootImage("images/lift-open.jpg");
        closedImage =  new GreenfootImage("images/lift-closed.jpg");
        setImage(closedImage);
        status = ST_STOPPED;
        people = 0;
    }

    public void act()
    {
        switch (status) {
        case ST_UP:     goingUp();
                        break;
        case ST_DOWN:   goingDown();
                        break;
        case ST_STOPPED:standingClosed();
                        break;
        case ST_OPEN:   standingOpen();
                        break;
        }
    }

    /**
     * We are currently going up - perform the next step.
     */
    private void goingUp()
    {
        moveUp();
        Floor floor = atFloor();
        if(floor != null) {
            openDoors();
            floor.liftArrived(Button.UP);
            people++;
            updateImage();
        }
    }

    /**
     * We are currently going down - perform the next step.
     */
    private void goingDown()
    {
        moveDown();
        Floor floor = atFloor();
        if(floor != null) {
            openDoors();
            floor.liftArrived(Button.DOWN);
            people--;
            updateImage();
        }
    }

    /**
     * We are currently standing with doors closed - perform the next step.
     */
    private void standingClosed()
    {
        if(randomizer.nextInt(100) > 95) {
            Floor floor = atFloor();
            if(floor.getFloorNumber() == 0) {
                goUp();
            }
            else if(floor.getFloorNumber() == ((Building)getWorld()).getTopFloor()) {
                goDown();
            }
            else if(randomizer.nextInt(100) >= 50) {
                goUp();
            }
            else {
                goDown();
            }
        }
    }

    /**
     * We are currently standing with doors open - perform the next step.
     */
    private void standingOpen()
    {
        if(randomizer.nextInt(100) > 98) {
            closeDoors();
        }
    }

    /**
     * Open the lift doors.
     */
    public void openDoors()
    {
        setImage(openImage);
        status = ST_OPEN;
    }
    
    /**
     * Close the lift doors.
     */
    public void closeDoors()
    {
        setImage(closedImage);
        status = ST_STOPPED;
    }
    
    /**
     * Start going upwards.
     */
    public void goUp()
    {
        status = ST_UP;
    }
    
    /**
     * Start going downwards.
     */
    public void goDown()
    {
        status = ST_DOWN;
    }
    
    /**
     * Move a bit up.
     */
    public void moveUp()
    {
        setLocation(getX(), getY() - 1);
    }
    
    /**
     * Move a bit down.
     */
    public void moveDown()
    {
        setLocation(getX(), getY() + 1);
    }
    
    /**
     * Are we at a floor? Return floor or null.
     */
    public Floor atFloor()
    {
        return ((Building)getWorld()).getFloorAt(getY());
    }

    /**
     * Update this lift's images (open and closed) according to it's state.
     */
    private void updateImage()
    {
        openImage.drawImage(emptyImage, 0, 0);
        if(people > 3)
            openImage.drawImage(personImage, 3, 14);
        if(people > 0)
            openImage.drawImage(personImage, 12, 15);
        if(people > 1)
            openImage.drawImage(personImage, 5, 22);
        if(people > 2)
            openImage.drawImage(personImage, 17, 20);
        paintNumber(openImage);
        paintNumber(closedImage);
    }
    
    /**
     * Paint the number of passengers onto the lift's image.
     */
    private void paintNumber(GreenfootImage img)
    {
        img.setColor(Color.WHITE);
        img.fillRect(20, 4, 24, 16);
        img.setColor(Color.BLACK);
        img.drawRect(20, 4, 24, 16);
        img.drawString(Integer.toString(people), 22, 17);
    }
}