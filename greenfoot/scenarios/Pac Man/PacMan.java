import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.Iterator;

/**
 * PacMan Class
 * @author Joseph Lenton
 * @date 06/02/07
 * 
 * The PacMan Class is for the user to play PacMan.
 * It has controls to move PacMan with either the arrow
 * or w,a,s,d keys.
 * 
 * PacMan will eat the food and Energizer pills when he moves
 * over them and will die if he touches a Ghost when the Ghost
 * is normal. If PacMan eats an Energizer pill the Ghosts
 * will change into their danger behaviour mode, and if PacMan
 * touches them then they will change into their dead mode.
 * 
 * PacMan also cannot move across the light blue PacManWall
 * instances which the Ghosts can.
 */
public class PacMan extends Actor
{
    // the amount of points you recieve for eating things
    private static final int
        EAT_FOOD_POINTS = 10, // when eating food
        EAT_ENERGIZER_POINTS = 50, // when eating an energizer
        EAT_GHOST_POINTS = 100, // when eating a ghost
        EAT_CHERRY_POINTS = 80; // when eating a cherry
        
    // the player's speed
    private static final int SPEED = 5;
    
    // movement types
    private static final int
        NONE = 0, // movement is allowed
        RIGHT = 1, // move right
        LEFT = 2, // move left
        DOWN = 3, // move down
        UP = 4; // move up
        
    // frame number
    private static final int
        FULL = 3,
        CLOSED = 2,
        SEMI_OPEN = 1,
        OPEN = 0;
    
    // if the player is dead or not
    private boolean dead = false;
    
    // the direction the player is moving in
    // first one to keep track of what the direction is before the control input
    private int originalDirection = NONE;
    // second to keep track of the direction inputted by the controls
    private int moveDirection = NONE;
    
    // to check if the mouth should be opening or closing
    private boolean mouthOpening = true;
    // the frame to use currently
    private int frameType = FULL;
    // animatedImages
    private GreenfootImage imageFull;
    private AnimatedImage imageUp;
    private AnimatedImage imageDown;
    private AnimatedImage imageLeft;
    private AnimatedImage imageRight;
    private AnimatedImage imageDeath;
    
    /**
     * PacMan Constructor
     * It created all the animated images,
     * and one for when pacman is a full circle.
     * 
     * It also sets the image,
     * and states the frameType to match.
     */
    public PacMan()
    {
        imageFull = new GreenfootImage("pacmanFull.png");
        imageUp = new AnimatedImage("pacmanAnimUp.png", 3, 16, 16);
        imageDown = new AnimatedImage("pacmanAnimDown.png", 3, 16, 16);
        imageLeft = new AnimatedImage("pacmanAnimLeft.png", 3, 16, 16);
        imageRight = new AnimatedImage("pacmanAnimRight.png", 3, 16, 16);
        imageDeath = new AnimatedImage("pacmanAnimDeath.png", 7, 16, 16);
        
        setImage( imageUp.getFrame(SEMI_OPEN) );
        frameType = SEMI_OPEN;
    }
    
    /**
     * Method to be run on each frame.
     * 
     * If the player isn't dead the controls will be taken.
     * The player will then move in the required direction,
     * update the player's frame
     * and check for food, ghosts, cherrys and energizers.
     * 
     * If the player is dead it will simply run the animation
     * of dying.
     */
    public void act()
    {
        if (!dead) {
            // make a second reference to the direction of the player
            originalDirection = moveDirection;
            controls();
            
            // Move the player.
            move(SPEED);
            updateFrame();
            // Check for food.
            checkForFood();
            checkForEnergizer();
            checkForGhost();
        }
        else {
            updateFrame();
        }
    }
    
    /**
     * Checks if the key's 'right', 'left', 'up',
     * 'down', 'w', 'a', 's' of 'd' are pressed, and updates
     * the movementDirection of the player, so they will
     * now attempt to move in that direction.
     */
    private void controls()
    {
        if ( Greenfoot.isKeyDown("right") || Greenfoot.isKeyDown("D") ) {
            moveDirection = RIGHT;
        }
        else if ( Greenfoot.isKeyDown("left") || Greenfoot.isKeyDown("A") ) {
            moveDirection = LEFT;
        }
        else if ( Greenfoot.isKeyDown("up") || Greenfoot.isKeyDown("W") ) {
            moveDirection = UP;
        }
        else if ( Greenfoot.isKeyDown("down") || Greenfoot.isKeyDown("S") ) {
            moveDirection = DOWN;
        }
    }
    
    /**
     * Works out where the player will move to, then
     * checks if the player can move there, and if so will
     * move there.
     * If they cannot move there it will run again with the
     * direction from before the controls were taken.
     * 
     * @param speed the speed the player will travel at
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
            // if the new direction is different to the previous direction
            if (moveDirection != originalDirection) {
                // I change the image to match
                updateImage();
            }
            
            // move the player to there
            setLocation(x, y);
        }
        else {
            // check if the original direction was different to the direction just used
            if (moveDirection != originalDirection) {
                // as the player cannot move there, I restore the direction
                moveDirection = originalDirection;
                // and move the player in the direction they were travelling in
                move(speed);
            }
        }
    }
    
    /**
     * Works out which frame should be used. This depends
     * on if the player is dead, the direction the player is
     * travelling in and the frame of the image.
     */
    private void updateImage()
    {
        // check if the player is dead
        if (dead) {
            setImage(imageDeath.getFrame(frameType) );
        }
        // check if the player is a full pacman
        else if (frameType == FULL) {
            setImage(imageFull);
        }
        // check if the player is moving right
        else if (moveDirection == RIGHT) {
            setImage( imageRight.getFrame(frameType) );
        }
        // check if the player is moving left
        else if (moveDirection == LEFT) {
            setImage( imageLeft.getFrame(frameType) );
        }
        // check if the player is moving up
        else if (moveDirection == UP) {
            setImage( imageUp.getFrame(frameType) );
        }
        // check if the player is moving down
        else if (moveDirection == DOWN) {
            setImage( imageDown.getFrame(frameType) );
        }
    }
    
    /**
     * Updates the frame number to be used when drawing.
     * The frame type will either be counting up or down depending
     * on if the player's mouth is opening or closing,
     * unless the player is dead.
     */
    private void updateFrame()
    {
        if (dead) {
            if (frameType < imageDeath.getNumberOfFrames()-1) {
                frameType++;
            }
            else {
                // end the method
                return;
            }
        }
        else if (mouthOpening) {
            if (frameType < FULL) {
                frameType++;
            }
            else {
                mouthOpening = false;
                frameType--;
            }
        }
        else {
            if (frameType > OPEN) {
                frameType--;
            }
            else {
                mouthOpening = true;
                frameType++;
            }
        }
        
        updateImage();
    }
    
    /**
     * Checks for a wall and a PacManWall
     * at the given co-ordinates.
     * 
     * @param x the x co-ordinate
     * @param y the y co-ordinate
     * @return true if there is a wall or PacMan wall, false if not.
     */
    private boolean checkForWall(int x, int y)
    {
        int origX = getX();
        int origY = getY();
        
        // Finds a wall object at the offset to the player.
        setLocation(x , y);
        Actor wall = getOneIntersectingObject( Wall.class );
        Actor pacManWall = getOneIntersectingObject( PacManWall.class );
        setLocation(origX, origY);
        
        return (wall != null || pacManWall != null);
    }
    
    /**
     * Checks for a Food instance at PacMan's location,
     * and if there is one the Food will be removed from
     * the world.
     */
    private void checkForFood()
    {
        // Finds a food object in the same tile.
        Food food = (Food) getOneIntersectingObject(Food.class);
        
        // If a food object has been found.
        if (food != null) {
            // Ups the score,
//            addScore( EAT_FOOD_POINTS );
            // and removes the food.
            getWorld().removeObject(food);
        }
    }
    
    /**
     * Checks for an Energizer instance at PacMan's location,
     * and if there is one the Energizer will be removed from
     * the world and the Ghosts will enter their danger behaviour
     * mode.
     */
    private void checkForEnergizer()
    {
        // find the energizer
        Energizer energizer = (Energizer) getOneIntersectingObject(Energizer.class);
        
        if (energizer != null) {
            Iterator ghostIterator = getWorld().getObjects(Ghost.class).iterator();
            while (ghostIterator.hasNext()) {
                Ghost ghost = (Ghost) ghostIterator.next();
                ghost.setDanger();
            }
            
            getWorld().removeObject(energizer);
//            addScore( EAT_ENERGIZER_POINTS );
        }
    }
    
    /**
     * Checks for a Ghost instance at PacMan's
     * current location. If one is found it's current
     * behaviour state will be checked.
     * If the Ghost is normal then PacMan will die.
     * If the Ghost is in danger then the Ghost will
     * be set to dead behaviour mode.
     * If the Ghost is dead, nothing will happen.
     */
    private void checkForGhost()
    {
        // find a ghost touching pacman
        Ghost ghost = (Ghost) getOneIntersectingObject(Ghost.class);
        
        // check if a ghost is found
        if (ghost != null) {
            // check if the ghost is normal
            if (ghost.isNormal()) {
                // if so pacman is now dead
                died();
            }
            // check if the ghost is in danger (if it's blue)
            else if (ghost.isDanger()) {
                // in which case the ghost is now dead
                ghost.setDead();
//                addScore( EAT_GHOST_POINTS );
            }
        }
    }
            
    /**
     * Sets PacMan to be dead, and starts the death
     * animation.
     */
    public void died()
    {
        if (!dead) {
            dead = true;
            frameType = -1;
        }
    }
}