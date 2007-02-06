import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
/**
 * Ghost Class
 * @author Joseph Lenton
 * @date 06/02/07
 * 
 * The Ghost class is for making Ghost instances which move around.
 * It has 3 different behaviour modes: normal, danger and dead.
 * 
 * When it is normal it will move randomly around and if it touches
 * Pac-Man, Pac-Man will die.
 * 
 * When in danger mode the ghost will move the same as in normal,
 * but slower and there will be a counter for counting down how long
 * the Ghost should remain in danger mode. If the Ghost touches Pac-Man
 * the Ghosts behaviour will change to dead.
 * 
 * When in dead mode the Ghost will try to head towards the GhostHealer instance
 * which marks the location of the Ghost's home. If the Ghost touches the GhostHealer
 * instance, his behaviour will change to normal.
 * 
 * The Ghost class also uses 3 animated images to store the different frames
 * for it's three modes. The Ghost is red when normal, blue when in danger and
 * simply a pair of white eyes when it is dead.
 */
public class Ghost extends Actor
{
    // the starting value of the danger timer
    private static final int DANGER_TIMER_START = 160;
    // the number of random numbers to pick from, when determining
    // if the Ghost should change direction or not, per time act is called.
    // i.e. 1 in RANDOM_DIRECTION chance of turning
    private static final int RANDOM_DIRECTION = 30;
    
    // movement types
    private static final int
        NO_DIRECTION = 0, // no direction
        RIGHT = 1, // move right
        DOWN = 2, // move left
        LEFT = 3, // move down
        UP = 4; // move up
        
    // behaviour types
    private static final int
        NORMAL = 0,
        DANGER = 1,
        DEAD = 2,
        DO_NOTHING = 3;
    
    // the speed's of the three behaviour types
    private static final int
        NORMAL_SPEED = 4,
        DANGER_SPEED = 3,
        DEAD_SPEED = 5;
    
    // the current behaviour of the Ghost
    private int behaviour = NORMAL;
    // the timer for how long the Ghost is in danger (when it is blue)
    private int dangerTimer;
    
    // before movement, the direction is backed up
    // incase the ghost cannot go in that direction
    private int originalDirection = UP;
    // the planned direction of the ghost, even if it
    // cannot go in that direction currently
    private int destinationDirection = NO_DIRECTION;
    // the amount of time the ghost should wait
    // before re-calculating it's destination direction
    private int destinationDirectionCounter = 0;
    // the direction the ghost will attempt to move in,
    // each time the 'move' method is called.
    private int moveDirection = UP;
    
    // images
    private AnimatedImage imageNormal;
    private AnimatedImage imageDanger;
    private AnimatedImage imageDead;
    
    // frame number
    private int frameNumber;
    
    // store if the ghost is walking into a wall or not
    private boolean collidingWall = false;
    
    /**
     * The Ghost Class's constructor.
     * It sets up the animated images for the Ghost
     * and sets it starting direction to up.
     */
    public Ghost()
    {
        // the frames for when the ghost is normal (red)
        imageNormal = new AnimatedImage("ghostAnimRed.png", 3, 18, 18);
        // the frames for when the ghost is in danger (blue)
        imageDanger = new AnimatedImage("ghostAnimDanger.png", 3, 18, 18);
        // the frames for when the ghost is dead (just a pair of eyes)
        imageDead = new AnimatedImage("ghostAnimDead.png", 3, 18, 18);
        
        moveDirection = UP;
    }

    /**
     * The ghost will check it's behaviour type, and based on this
     * will then behave in a different way, and then move at a
     * different speed.
     */
    public void act()
    {
        // when the ghost is normal
        if ( behaviour == NORMAL ) {
            AI();
            move(NORMAL_SPEED);
        }
        // when the ghost is in danger
        else if ( behaviour == DANGER ) {
            AI();
            move(DANGER_SPEED);
            tickDangerTimer();
        }
        // when the ghost is dead
        else if ( behaviour == DEAD ) {
            deadAI();
            move(DEAD_SPEED);
        }
        
        // update the image
        updateImage();
    }

    /**
     * For checking if the player will move,
     * and if so works out what should happen.
     * 
     * @param speed the speed the Ghost will move at
     */
    private void move(int speed)
    {
        // get the x and y position of the player
        int x = getX();
        int y = getY();
        
        // change it according to the movement
        if (moveDirection == RIGHT) {
            x = x+speed;
        }
        else if (moveDirection == LEFT) {
            x = x-speed;
        }
        else if (moveDirection == DOWN) {
            y = y+speed;
        }
        else if (moveDirection == UP) {
            y = y-speed;
        }
        
        // get the width and height of the world
        int worldWidth = getWorld().getWidth();
        int worldHeight = getWorld().getHeight();
        
        // check if the x co-ordinate is within the world
        if (x < 0) {
            x += worldWidth;
        }
        else if (x > worldWidth-1) {
            x -= worldWidth;
        }
        
        // check if the y co-ordinate is within the world
        if (y < 0) {
            y += worldHeight;
        }
        else if (y > worldHeight-1) {
            y -= worldHeight;
        }
        
        // unless there is a wall at the next position
        if (!checkForWall(x,y)) {
            if (moveDirection == destinationDirection) {
                destinationDirection = NO_DIRECTION;
            }
            // move the player to there
            setLocation(x, y);
        }
        else {
            // 
            if (moveDirection == originalDirection) {
                if (Greenfoot.getRandomNumber(2) == 0) {
                    moveDirection = turnRight();
                }
                else {
                    moveDirection = turnLeft();
                }
            }
            else {
                moveDirection = originalDirection;
                move(speed);
            }
        }
    }
    
    /**
     * Checks to find a wall at the given co-ordinates.
     * 
     * @param x the x co-ordinate of the wall
     * @param y the y co-ordinate of the wall
     * @return true if a wall is found, false if a wall is not found
     */
    private boolean checkForWall(int x, int y)
    {
        int origX = getX();
        int origY = getY();
        
        // Finds a wall object at the offset to the player.
        setLocation(x , y);
        Wall wall = (Wall) getOneIntersectingObject( Wall.class );
        setLocation(origX, origY);
        
        return wall != null;
    }
    
    /**
     * Checks to find a GhostHealer object at the Ghosts
     * current location.
     * @return true if the a GhostHealer instance is touching the Ghost.
     */
    private boolean checkForGhostHealer()
    {
        GhostHealer ghostHealer = (GhostHealer) getOneIntersectingObject( GhostHealer.class );
        return ghostHealer != null;
    }
    
    /**
     * Updates the images to be used by the Ghost.
     */
    private void updateImage()
    {
        if (behaviour == NORMAL) {
            setImage(imageNormal.getFrame( frameNumber % imageNormal.getNumberOfFrames() ));
        }
        else if (behaviour == DANGER) {
            setImage(imageDanger.getFrame( frameNumber % imageDanger.getNumberOfFrames() ));
        }
        else if (behaviour == DEAD) {
            setImage(imageDead.getFrame( frameNumber % imageDead.getNumberOfFrames() ));
        }
        
        frameNumber++;
    }
    
    /**
     * Lowers the danger timer of the Ghost for when it is in danger,
     * and if it reaches 0 will turn the ghost normal.
     */
    private void tickDangerTimer()
    {
        // lower the dangerTimer
        dangerTimer--;
        // and check if the Ghost should now turn to normal
        if (dangerTimer <= 0 && behaviour == DANGER) {
            behaviour = NORMAL;
        }
    }
    
    /**
     * The Ghost's AI to be run when he is normal or in danger.
     */
    private void AI()
    {
        // detrmine if the Ghost should turn it's
        // destinationDirection or not
        if (Greenfoot.getRandomNumber(RANDOM_DIRECTION) == 0) {
            // check if the Ghost should turn right
            if (Greenfoot.getRandomNumber(2) == 0) {
                destinationDirection = turnRight();
            }
            // otherwise turn left
            else {
                destinationDirection = turnLeft();
            }
        }
        
        // if the ghost is in the ghost home,
        if (checkForGhostHealer()) {
            // move up (to leave)
            destinationDirection = UP;
        }
        
        // if there is a destinationDirection,
        // I first backup the movementDirection,
        // then set movementDirection to the destinationDirection
        if (destinationDirection != NO_DIRECTION) {
            originalDirection = moveDirection;
            moveDirection = destinationDirection;
        }
    }
    
    /**
     * For finding the direction the Ghost should head in
     * to go to the co-ordinates supplied.
     * 
     * @param x the x co-ordinate of the position it wants to go to
     * @param y the y co-ordinate of the position it wants to go to
     * @return the best direction the Ghost should move in to get to those co-ordinates
     */
    private int calculateDirection(int x, int y)
    {
        int x_distance = x - getX();
        int y_distance = y - getY();
        
        if (Math.abs(x_distance) > Math.abs(y_distance)) {
            if (x_distance < 0) {
                return LEFT;
            }
            else {
                return RIGHT;
            }
        }
        else {
            if (y_distance > 0) {
                return DOWN;
            }
            else {
                return UP;
            }
        }
    }
    
    /**
     * Returns the direction right of the Ghosts current moving direction.
     * @return the direction right of the Ghosts current moving direction.
     */
    private int turnRight()
    {
        int direction = moveDirection;
        direction++;
        
        if (direction > UP) {
            direction = RIGHT;
        }
        
        return direction;
    }
    
    /**
     * Returns the direction left of the Ghosts current moving direction.
     * @return the direction left of the Ghosts current moving direction.
     */
    private int turnLeft()
    {
        int direction = moveDirection;
        direction--;
        
        if (direction < RIGHT) {
            direction = UP;
        }
        
        return direction;
    }
    
    /**
     * The AI of the Ghost to be run, if the Ghost is dead.
     * This is for getting the ghost to it's home where a
     * GhostHealer instance is sitting.
     */
    private void deadAI()
    {
        if (checkForGhostHealer()) {
            behaviour = NORMAL;
        }
        else {
            if (destinationDirection == NO_DIRECTION || Greenfoot.getRandomNumber(RANDOM_DIRECTION) == 0) {
                Level level = (Level) getWorld();
                GhostHealer ghostHealer = level.getGhostHealer();
                destinationDirection = calculateDirection( ghostHealer.getX(), ghostHealer.getY() );
            }
            originalDirection = moveDirection;
            moveDirection = destinationDirection;
        }
    }
    
    /**
     * Sets the Ghost's behaviour to danger mode and
     * starts the Ghosts danger counter, for how
     * long the Ghost will be in danger.
     */
    public void setDanger()
    {
        if (behaviour != DEAD) {
            behaviour = DANGER;
            dangerTimer = DANGER_TIMER_START;
        }
    }
    
    /**
     * Sets the Ghost's behaviour to be dead.
     */
    public void setDead()
    {
        behaviour = DEAD;
        destinationDirection = NO_DIRECTION;
    }
    
    /**
     * Sets the Ghost to do nothing.
     */
    public void setDoNothing()
    {
        behaviour = DO_NOTHING;
    }
    
    /**
     * To find out if the Ghost is in normal behaviour mode.
     * @return true if the Ghost's behavour is normal, false if it isn't.
     */
    public boolean isNormal()
    {
        return behaviour == NORMAL;
    }
    
    /**
     * To find out if the Ghost is in danger behaviour mode.
     * @return true if the Ghost's behavour is danger, false if it isn't.
     */
    public boolean isDanger()
    {
        return behaviour == DANGER;
    }
}