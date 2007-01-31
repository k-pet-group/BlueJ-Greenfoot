import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * Pirate class
 * 
 * @author Joseph Lenton
 * @version 4/12/06
 */

public class Pirate extends Unit
{
    // the default counting value until laying a new treasure flag
    private static final int TreasureFlagDefault = 18;
    // the Pirates Speed
    private static final int SPEED = 3;
    // is the pirate holding treasure
    private boolean holdingTreasure = false;
    // the amount of time the Pirate waits before dropping a new flag
    private int treasureFlagCounter = 0;
    // pirates home
    private Home home;
    
    // check if the Pirate collided on his way home
    private boolean collidedOnWayHome = false;
    
    /**
     * Constructs the Pirate instance.
     * 
     * @param home The home of the Pirate.
     */
    public Pirate(Home home)
    {
        // sets the home for the pirate
        this.home = home;
    }

    /**
     * What the Pirate will do each frame.
     */
    public void act()
    {
        // if the pirate is holding treasure
        if (holdingTreasure) {
            moveHome();
            dropTreasureFlag();
            findHome();
        }
        // if the pirate isn't holding treasure
        else {
            walkingIntoEdge();
            move();
            findTreasure();
            findFlag();
        }
    }
    
    /**
     * Move the Pirate home
     */
    private void moveHome()
    {
        // check if there was a collision on it's way home
        if (collidedOnWayHome) {
            collisionTurnAntiClockwise();
        }
        else {
            // turn towards home
            turnDirection(findHomeDirection()-direction);
        }
        
        // check to turn off the home collision
        turnOffCollidedOnWayHome();
        moveForward();
        incrimentCollision();
    }
    
    private void moveForward()
    {
        // get the amount the Pirate
        // will move by on X nad Y axis
        int x = getXMovement();
        int y = getYMovement();
        
        while (findRock(x, y)) {
            collidedOnWayHome = true;
            collisionTurnClockwise();
            x = getXMovement();
            y = getYMovement();
        }
        
        setLocation( checkXBounds(x+getX()), checkYBounds(y+getY()) );
    }
    
    /**
     * Turn according to how it should
     */
    private void collidedOnWayHomeTurn()
    {
        // if so, it turns in the opposite direction it would if it had collided
        // it will try to turn into the rocks,
        // and so effectively follow their edge
        if (collisionTurnDirection) {
            turnDirection(-1);
        }
        else {
            turnDirection(1);
        }
    }
    
    /**
     * Check if collidedOnWayHome should be turned off
     * by comparing pirates direction to the direction of home
     */
    private void turnOffCollidedOnWayHome()
    {
        int homeDirection = findHomeDirection();
        if ((direction == homeDirection) || (directionBoundsCheck(direction+1) == homeDirection) || (directionBoundsCheck(direction-1) == homeDirection)) {
            collidedOnWayHome = false;
        }
    }
    
    /**
     * Find the direction of Home in relation to the Pirate
     */
    private int findHomeDirection()
    {
        int homeDirection = 0;
        
        // home is to the west
        if (home.getX()-getX() < -SPEED) {
            // and north (west)
            if (home.getY()-getY() < -SPEED) {
                homeDirection = NORTHWEST;
            }
            // and south (west)
            else if (home.getY()-getY() > SPEED) {
                homeDirection = SOUTHWEST;
            }
            // home is simply to the west
            else {
                homeDirection = WEST;
            }
        }
        // home is to the east
        else if (home.getX()-getX() > SPEED) {
            // and north (east)
            if (home.getY()-getY() < -SPEED) {
                homeDirection = NORTHEAST;
            }
            // and south (east)
            else if (home.getY()-getY() > SPEED) {
                homeDirection = SOUTHEAST;
            }
            // home is simply to the east
            else {
                homeDirection = EAST;
            }
        }
        // you are horizontally level with home
        else {
            // home is north
            if (home.getY()-getY() < -SPEED) {
                homeDirection = NORTH;
            }
            // home is south south
            else if (home.getY()-getY() > SPEED) {
                homeDirection = SOUTH;
            }
            else {
                // you are on home
            }
        }
        
        return homeDirection;
    }
    
    /**
     * 
     */
    private void dropTreasureFlag()
    {
        if (treasureFlagCounter <= 0) {
            getWorld().addObject(new TreasureFlag(direction), getX(), getY());
            treasureFlagCounter = TreasureFlagDefault;
        }
        else {
            treasureFlagCounter--;
        }
    }
    
    /**
     * 
     */
    private void findTreasure()
    {
        Treasure treasure = (Treasure) getOneIntersectingObject(Treasure.class);
        
        // if the treasure exists and the pirate isn't holding treasure and there is treasure,
        if (treasure != null && holdingTreasure == false && treasure.getTreasure() > 0) {
            // the pirate takes some treasure
            treasure.takeTreasure();
            holdingTreasure = true;
        }
    }
    
    /**
     * 
     */
    private void findHome()
    {
        Home home = (Home) getOneIntersectingObject(Home.class);
        
        if (home == this.home) {
            home.addTreasure();
            holdingTreasure = false;
        }
    }
    
    private void findFlag()
    {
        DirectionFlag flag = (DirectionFlag) getOneIntersectingObject(DirectionFlag.class);
        
        if (flag != null) {
            setDirection(flag.changeDirection(direction));
        }
    }
}