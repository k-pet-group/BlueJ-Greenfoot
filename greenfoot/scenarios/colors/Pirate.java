import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * YARR! A pirate looks for treasure.
 * 
 * @author Poul Henriksen
 * @version 0.5 
 */
public class Pirate extends Agent
{
    /** Keeps track of which color to leave behind when leaving a trail towards treasure */
    private int colorCount;
    
    /** 
     * Should normallly not be used since a pirate needs a home base.
     */
    public Pirate()
    {
        super();
    }

    /**
     * Create new pirate.
     */
    public Pirate(Direction direction, Base base) {
        super(direction, base);
    }   
    
    /**
     * Moves around an obstacle.
     */
    protected void avoidObstacle() {
        while(!canMove()) {
            turnLeft();
        }
        moveForward();
    }
    
    /**
     * Move towards the base.
     */
    protected void moveHome() {
        if(getObjectsAtOffset(0,0, Base.class).contains(getBase())) {
            dropTreasure();
        } 
        else {
            turnToward(getBase());
            putColor(new Yellow());
            if(canMove()) {
                moveForward();
            }
            else {                
                avoidObstacle();
            }            
        }
    }
    
    /**
     * Move away from the base.
     */
    protected void moveAway() {
        int dx = getBase().getX() - getX();
        int dy = getBase().getY() - getY();
        Direction newDirection = Direction.getDirection(-dx, -dy);
        setDirection(newDirection);
        if(canMove()) {
            moveForward();
        }
        else {                
            avoidObstacle();
        }         
    }
    
    /**
     * This pirate marks newly visited places with a blue color,
     * and leaves a trail of yellow to mark a path from a treasure 
     * back to the base.
     */
    public void act()
    {
        if(gotTreasure()) {
            moveHome();
            return;            
        }
        
        Target treasure = searchForTarget();
        AbstractColor color = getColor();       
        if(treasure != null) {            
            pickUpTarget(treasure);
        }
        else if(color == null) {
            //Leave a blue color if at a new location.
            putColor(new Blue());
            moveRandom();    
        }    
        else if(! (color instanceof Blue) ){
            //If we find another color, do something clever.
            //TODO
           moveAway();
        }
        else if (color instanceof Blue) {
            //We have already been here - go somewhere else
            //TODO
            moveRandom();      
        }
        else {            
            turnLeft();
        }
    }
    
    /**
     * Doesn't actually follow the trail - just moves away from home
     */
    private void followTrail() {
        if(getBase() == null) {
            moveRandom();
            return;
        }
        int dx = getBase().getX() - getX();        
        int dy = getBase().getY() - getY();
        setDirection( Direction.getDirection(dx, dy) );
    }
    
    private void moveRandom() {
        int random = 0;
        if(canMove()) {            
            random = Greenfoot.getRandomNumber(5);
        }
        else {
            //if we can't move forward exclude the move forward option
            random = Greenfoot.getRandomNumber(2);
        }
        
        if(random == 0) {
            turnLeft();
        }
        else if(random == 1) {            
            turnRight();
        }
        else {
            moveForward();
        }
    }
}