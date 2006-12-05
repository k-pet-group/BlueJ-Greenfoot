import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;
/**
 * An agent can move in eight different directions, one step at a time. An agent usually has a home base.
 * 
 * @author Poul Henriksen
 * @version 0.5
 */
public class Agent extends Actor
{
    private Direction direction = Direction.EAST;
    private Base base;
    private boolean gotTreasure = false; //could be a proper "treasure" object that is extracted from the target.
    
    /** Radius of the color that is painted on the world image */
    private final static int COLOR_RADIUS = 9;
    
    /**
     * Should normally not be called, since agents need a base.
     */
    public Agent()
    {
    }

    /**
     * Create a new agent.
     */
    public Agent(Direction direction, Base base) {
        setDirection(direction);
        this.base = base;
    }   
    
    /**
     * Get this agents home base.
     */
    public Base getBase() {
        return base;
    }
    
    /**
     * Turns this actor towards another actor. 
     *
     * @returns True if the actor turned, false if it is already pointing towards the other actor.
     */
    public boolean turnToward(Actor actor) {
        int dx = actor.getX() - getX();
        int dy = actor.getY() - getY();
        Direction newDirection = Direction.getDirection(dx, dy);
        if(newDirection.equals(direction)) {
            return false;
        }
        else {
            setDirection(newDirection);
            return true;
        }
    }
    
    /**
     * Search for the target at the agents current location.
     */
    public Target searchForTarget() {
        Target target = (Target) getOneObjectAtOffset(0,0, Target.class);
        return target;
    }
    
    /**
     * Picks up the a target, if it is at the agent's current location.
     */
    public void pickUpTarget(Target treasure) {
        if(this.intersects(treasure)) {
            gotTreasure = true;
        }
    }    
    
    /**
     * Do we currently carry any treasure?
     */
    public boolean gotTreasure() {
        return gotTreasure;
    } 
    
    /**
     * If we are at the base, this method will drop the treasure at the base. 
     * If not at the base or we are not carrying any treasure, nothing will be done.
     */
    protected void dropTreasure() {
        if(gotTreasure && intersects(base)) {
            gotTreasure = false;
            base.increaseTreasure();
        }
    }    
    
    /**
     * Sets the direction in which this agent is pointing.
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
        setRotation(direction.getAngle());
    }
    
    /**
     * Put a color on the current location of the actor. This will override
     * any color already there.
     */
    public void putColor(AbstractColor color) {
        //Currently, the colors are printed directly on the background image
        //of the world instead of adding the color object to the world.
        //This speeds up execution speed a lot (at least on my linux machine). 
        //Poul
        Color awtColor = color.getColor();
        int cellSize = getWorld().getCellSize();
        int xPixel = getX() * cellSize;        
        int yPixel = getY() * cellSize;
        
        GreenfootImage bg = getWorld().getBackground();
        bg.setColor(awtColor);
        bg.fillOval(xPixel, yPixel, COLOR_RADIUS * 2, COLOR_RADIUS * 2);
    }
    
    /**
     * Get the color on the current location of the actor.
     * 
     * @returns The color, or null if no color.
     */
    public AbstractColor getColor() {
        Color awtColor = getWorld().getColorAt(getX(), getY());
        if(awtColor.equals(Color.PINK)) {
            return new Pink();
        } 
        else if(awtColor.equals(Color.GREEN)) {
            return new Green();
        }
        else if(awtColor.equals(Color.BLUE)) {
            return new Blue();
        }
        else if(awtColor.equals(Color.RED)) {
            return new Red();
        }
        else if(awtColor.equals(Color.YELLOW)) {
            return new Yellow();
        }
        return null;
    }
    
    
    /**
     * Put a color on the current location of the actor. This will override any
     * color already there.
     */
    public void putColorSLOW(AbstractColor color) {
        Actor currentColor = getOneObjectAtOffset(0,0, AbstractColor.class);
        if(currentColor != null) {
            getWorld().removeObject(currentColor);
        }
        getWorld().addObject(color, getX(), getY());
    }
    
    /**
     * Get the color on the current location of the actor.
     * 
     * @returns The color, or null if no color.
     */
    public AbstractColor getColorSLOW() {
        Actor currentColor = getOneObjectAtOffset(0,0, AbstractColor.class);
        return (AbstractColor) currentColor;
    }
    
    /**
     * Moves on step forward in the current direction.
     */
    public void moveForward() {
        int newX = direction.getDeltaX() + getX();
        int newY = direction.getDeltaY() + getY();
        setLocation(newX, newY);             
    }
    
    /**
     * Turns right.
     */
    public void turnRight() {
        setDirection(direction.getRight());
    }
    
    /**
     * Turns left.
     */
    public void turnLeft() {
        setDirection(direction.getLeft());
    }
    
    /**
     * Returns true if we can move forward without hitting any obstacles.
     */
    public boolean canMove() {       
        int dx = direction.getDeltaX();
        int dy = direction.getDeltaY() ;
        int newX = dx + getX();
        int newY = dy + getY();
        if (newY < 0 ) {
            return false;
        }
        else if (newY >= getWorld().getHeight()) {
            return false;
        }
        else if (newX < 0) {
            return false;
        }
        else if (newX >= getWorld().getWidth()) {
            return false;
        }
        else if ( getOneObjectAtOffset(dx, dy, Obstacle.class) != null) {
            return false;
        }
        else {
            return true;
        }
    }
}