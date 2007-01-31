import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * Unit class
 * 
 * Handles various aspects for all units,
 * including direction, collision detection and movement,
 * of the unit.
 * 
 * @author Joseph Lenton
 * @version 4/11/06
 */
public class Unit extends Actor
{
    // directions
    protected static final int
        NORTH = 0, NORTHEAST = 1, EAST = 2,
        SOUTHEAST = 3, SOUTH = 4, SOUTHWEST = 5,
        WEST = 6, NORTHWEST = 7;
    
    // the default speed of the unit
    protected static final int SPEED = 3;
    // the random chance to turning
    protected static final int NoOfDirections = 8;
    // the value collisionCount will be set to when it resets
    private static final int CollisionCountMaximum = 1000;
    
    // how many collisions have happened
    protected int collisionCount = 0;
    // which direction the pirate will turn when it collides
    protected boolean collisionTurnDirection = false;
    // a collision was detected on the last movement
//    protected boolean collidedLast = false;
    // pirates direction
    protected int direction;
    
    /**
     * Constructs the Pirate instance.
     * 
     * @param home The home of the Pirate.
     */
    public Unit()
    {
        // gives the unit a random direction
        direction = Greenfoot.getRandomNumber(NoOfDirections);
    }

    /**
     * What the Pirate will do each frame.
     */
    public void act()
    {
    }
    
    /**
     * The Pirates normal movement code when he isn't holding treasure
     */
    protected void move()
    {
        // turn the unit
        turnUnit();
        moveForward();
    }
    
    /**
     * Move the Pirate forward, check for collision,
     * and place the Pirate in it's new position
     */
    private void moveForward()
    {
        // get the amount the Pirate
        // will move by on X nad Y axis
        int x = getXMovement();
        int y = getYMovement();
        
        while (findRock(x, y)) {
            collisionTurnClockwise();
            x = getXMovement();
            y = getYMovement();
        }
        
        setLocation( checkXBounds(x+getX()), checkYBounds(y+getY()) );
    }
    
    /**
     * Change the Unit direction for when he moves
     * by a random amount
     */
    protected void turnUnit()
    {
        // check if the pirate should turn?
        if (Greenfoot.getRandomNumber(NoOfDirections) == 0) {
            // if so, should he turn left or right?
            if (Greenfoot.getRandomNumber(2) == 0) {
                // turn clockwise
                turnDirection(1);
            }
            else {
                // turn anti-clockwise
                turnDirection(-1);
            }
        }
    }
    
    /**
     * Turns the Unit clockwise if collisionTurn is on,
     * and anti-clockwise if it isn't.
     */
    protected void collisionTurnClockwise()
    {
        if (collisionTurnDirection) {
            turnDirection(1);
        }
        else {
            turnDirection(-1);
        }
    }
    
    /**
     * Turns the Unit anti-clockwise if collisionTurn is on,
     * and clockwise if it isn't.
     */
    protected void collisionTurnAntiClockwise()
    {
        if (collisionTurnDirection) {
            turnDirection(-1);
        }
        else {
            turnDirection(1);
        }
    }
    
    /**
     * Returns how much the X co-ordinate should move based
     * on the current unit's direction.
     * 
     * @return the amount the unit should move across the X axis under that direction
     */
    protected int getXMovement()
    {
        if (direction == NORTHEAST || direction == EAST || direction == SOUTHEAST) {
            return SPEED;
        }
        else if (direction == SOUTHWEST || direction == WEST || direction == NORTHWEST) {
            return - SPEED;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Returns how much the X co-ordinate should move based
     * on the inputted unit direction.
     * 
     * @param direction the custom direction
     * @return the amount the unit should move across the X axis under that direction
     */
    protected int getXMovement(int direction)
    {
        if (direction == NORTHEAST || direction == EAST || direction == SOUTHEAST) {
            return SPEED;
        }
        else if (direction == SOUTHWEST || direction == WEST || direction == NORTHWEST) {
            return - SPEED;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Returns how much the Y co-ordinate should move based
     * on the current unit direction.
     * 
     * @return the amount the unit should move across the y axis under that direction
     */
    protected int getYMovement()
    {
        if (direction == SOUTHWEST || direction == SOUTH || direction == SOUTHEAST) {
            return SPEED;
        }
        else if (direction == NORTHWEST || direction == NORTH || direction == NORTHEAST) {
            return - SPEED;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Returns how much the Y co-ordinate should move based
     * on the inputted unit direction.
     * 
     * @param direction the custom direction
     * @return the amount the unit should move across the y axis under that direction
     */
    protected int getYMovement(int direction)
    {
        if (direction == SOUTHWEST || direction == SOUTH || direction == SOUTHEAST) {
            return SPEED;
        }
        else if (direction == NORTHWEST || direction == NORTH || direction == NORTHEAST) {
            return - SPEED;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Checks the X co-ordinate aginst the worlds bounds
     * 
     * @param x the x co-ordinate being checked
     * @return the acceptable x co-ordinate
     */
    protected int checkXBounds(int x)
    {
        if (x < 0) {
            x = 0;
        }
        else if (x >= getWorld().getWidth()-1) {
            x = getWorld().getWidth()-1;
        }
        
        return x;
    }
    
    /**
     * Checks the Y co-ordinate against the worlds bounds
     * 
     * @param y the y co-ordinate being checked
     * @return the acceptable Y co-ordinate
     */
    protected int checkYBounds(int y)
    {
        if (y < 0) {
            y = 0;
        }
        else if (y >= getWorld().getHeight()-1) {
            y = getWorld().getHeight()-1;
        }
        
        return y;
    }
    
    /**
     * Check if the unit is at the edge,
     * and if so changes the direcion of the unit.
     */
    protected void walkingIntoEdge()
    {
        // if their on the west side and facing west
        if (direction == WEST && getX() == 0) {
            // they will face east
            setDirection(EAST);
        }
        // if he is on the east side and facing east
        else if (direction == EAST && getX() == getWorld().getWidth()-1) {
            // they will not face west
            setDirection(WEST);
        }
        // if he is on the north side and facing north
        else if (direction == NORTH && getY() == 0) {
            // he will now face north
            setDirection(SOUTH);
        }
        // if he is on the south side and facing south
        else if (direction == SOUTH && getY() == getWorld().getHeight()-1) {
            // he is now facing south
            setDirection(NORTH);
        }
    }
    
    /**
     * Incriments the collision counter
     * When the collision counter reaches the maximum,
     * it will reset and the collision turn direction
     * will swap between true and false
     */
    protected void incrimentCollision()
    {
        collisionCount++;
        if (collisionCount > CollisionCountMaximum) {
            collisionTurnDirection = !collisionTurnDirection;
            collisionCount = 0;
        }
    }
    
    /**
     * Turns the unit a certain amount of points around.
     * Their are 8 different directions for the unit,
     * so turning 8 will do a 360, 4 a 180.
     * 
     * @param directionTurn the amount to turn the unit by.
     */
    protected void turnDirection(int directionTurn)
    {
        // change the direction
        direction += directionTurn;
        directionBoundsCheck();
    }
    
    /**
     * Sets the units direction to a new direction
     * 
     * @param direction the new direction of the unit
     */
    protected void setDirection(int direction)
    {
        // set the new direction
        this.direction = direction;
        directionBoundsCheck();
    }
    
    /**
     * a direction bounds check on the Pirates direction,
     * corrects the direction if outside NORTH to NORTH-WEST (0 to 7)
     */
    protected void directionBoundsCheck()
    {
        // while the direction is out of bounds,
        // it is moved back into bounds
        while (direction < NORTH) {
            direction += 8;
        }
        while (direction > NORTHWEST) {
            direction -= 8;
        }
    }
    
    /**
     * a direction bounds check on a custom direction,
     * returns the corrected direction if outside NORTH to NORTH-WEST (0 to 7)
     * 
     * @param Direction the direction you want to check
     * @return the correct direction
     */
    protected int directionBoundsCheck(int direction)
    {
        // while the direction is out of bounds,
        // it is moved back into bounds
        while (direction < NORTH) {
            direction += 8;
        }
        while (direction > NORTHWEST) {
            direction -= 8;
        }
        
        return direction;
    }
    
    /**
     * returns if it finds a rock at the units position
     * 
     * @return true if the rock is found, false if it is not found
     */
    protected boolean findRock()
    {
        Rock rock = (Rock) getOneIntersectingObject(Rock.class);
        return rock != null;
    }
    
    /**
     * For finding a rock at an offset
     * 
     * @param x the x offset
     * @param y the y offset
     * @return true if the rock is found, false if it is not found
     */
    protected boolean findRock(int x, int y)
    {
        Rock rock = (Rock) getOneObjectAtOffset(x, y, Rock.class);
        return rock != null;
    }
}