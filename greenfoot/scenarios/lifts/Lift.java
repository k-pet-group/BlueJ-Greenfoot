import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.Random;
import java.awt.*;
import javax.swing.*;

public class Lift extends GreenfootObject
{
    private static final Random randomizer = Building.getRandomizer();

    private static final int ST_UP = 0;
    private static final int ST_DOWN = 1;
    private static final int ST_STOPPED = 2;
    private static final int ST_OPEN = 3;

    private int status;
    private int people;
    private Image openImage;
    private Image emptyImage;
    private Image closedImage;
    private Image personImage;
    
    public Lift()
    {
        setImage("person.gif");
        personImage = getImage().getImage();
        setImage("lift-open.jpg");
        openImage = getImage().getImage();
        setImage("lift-open.jpg");
        emptyImage = getImage().getImage();
        setImage("lift-closed.jpg");
        closedImage = getImage().getImage();
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
        int floor = atFloor();
        if(floor != -1) {
            openDoors();
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
        int floor = atFloor();
        if(floor != -1) {
            openDoors();
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
            int floor = atFloor();
            if(floor == 0) {
                goUp();
            }
            else if(floor == ((Building)getWorld()).getTopFloor()) {
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
        setImage(new ImageIcon(openImage));
        status = ST_OPEN;
    }
    
    /**
     * Close the lift doors.
     */
    public void closeDoors()
    {
        setImage(new ImageIcon(closedImage));
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
     * Are we at a floor? Return floor number or -1.
     */
    public int atFloor()
    {
        return ((Building)getWorld()).getFloorAt(getY());
    }

    /**
     * Update this lift's images (open and closed) according to it's state.
     */
    private void updateImage()
    {
        Graphics g = openImage.getGraphics();
        g.drawImage(emptyImage, 0, 0, null);
        if(people > 3)
            g.drawImage(personImage, 3, 14, null);
        if(people > 0)
            g.drawImage(personImage, 12, 15, null);
        if(people > 1)
            g.drawImage(personImage, 5, 22, null);
        if(people > 2)
            g.drawImage(personImage, 17, 20, null);
        paintNumber(openImage);
        paintNumber(closedImage);
    }
    
    /**
     * Paint the number of passengers onto the lift's image.
     */
    private void paintNumber(Image img)
    {
        Graphics g = img.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(20, 4, 24, 16);
        g.setColor(Color.BLACK);
        g.drawRect(20, 4, 24, 16);
        g.drawString(Integer.toString(people), 22, 17);
    }
}