import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * The Portal class is for creating instances of portals.
 * Portals check if a Pirate instance is ontop of it,
 * and if it has a corresponding exit portal,
 * will move the Pirate to the exit portal.
 * 
 * When a Portal recieves a Pirate it will start a counter,
 * and the Portal can no longer move Pirates to the other Portal
 * for a certain duration. The counter tracks and shows
 * this duration visually. This ensures Pirates do not get caught
 * teleporting between two Portal instances, because this Portal
 * will effectively be turned off from sending Pirates for the duration.
 * It can still recieve Pirates during this time.
 * 
 * A Portal can exist without an end, for example it could be
 * the end Portal to a different Portal instance, allowing only
 * one way travel through the portals.
 * 
 * @author Joseph Lenton
 * @version 16/01/07
 */
public class Portal extends Actor
{
    // the default countdown time
    static final int PORTAL_OFF_TIME = 90;
    
    // the Portals' counter
    private Counter counter;
    // the corresponding portal units will leave to
    private Portal exitPortal;
    
    /**
     * Portal Constructor for when there is not
     * a portal to be placed at it's end.
     */
    public Portal()
    {
        exitPortal = null;
        counter = null;
    }
    
    /**
     * Portal Constructor for when there is a portal
     * to be placed at it's end.
     * 
     * @param exitPortal the exit portal for this portal instance
     */
    public Portal(Portal exitPortal)
    {
        this.exitPortal = exitPortal;
        counter = null;
    }
    
    /**
     * For setting the exit Portal for this Portal.
     * @param exitPortal the exit for this Portal.
     */
    public void setExitPortal(Portal exitPortal)
    {
        this.exitPortal = exitPortal;
    }
    
    /**
     * Start off timer starts the counter for stating
     * that the Portal is turned off. When off it does
     * not move Pirates to the other Portal.
     */
    public void startOffTimer()
    {
        if (counter == null) {
            counter = new Counter(getImage().getWidth(), 12, PORTAL_OFF_TIME);
            getWorld().addObject(counter, getX(), getY() + getImage().getHeight()/2);
        }
        else {
            counter.reset();
        }
    }
    
    /**
     * When the Portal is acting,
     * it checks if there are any Pirates at the same location.
     * If so and the portal is active, they are teleported.
     * If Portal is counting, counter is incrimented.
     */
    public void act()
    {
        // check if a counter doesn't exists
        if (counter == null) {
            teleportPirate();
        }
        // check if the counter is at 0
        else if (counter.getCount() == 0) {
            teleportPirate();
        }
        // otherwise we incriment that counter
        else {
            counter.incriment();
        }
    }
    
    /**
     * Checks if there is a Pirate at the Portal's
     * location, if so it is moved to the exit Portal's
     * location.
     */
    public void teleportPirate()
    {
        Pirate pirate = (Pirate) getOneIntersectingObject(Pirate.class);
        if (pirate != null && exitPortal != null) {
            exitPortal.startOffTimer();
            pirate.setLocation(exitPortal.getX(), exitPortal.getY());
        }   
    }
}