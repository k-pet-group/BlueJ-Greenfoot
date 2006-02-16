import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.Collection;
import java.util.Iterator;

public class Ball extends GreenfootObject
{
    public static int BALLSIZEX = 20;
    public static int BALLSIZEY = 19;

    private int motionX;
    private int motionXdenom;
    private int motionXaccum;
    
    private int motionY;
    private int motionYdenom;
    private int motionYaccum;
    
    private int ballSpeed;

    public Ball()
    {
        setImage("images/ball.png");
        motionX = -20;
        motionXdenom = 10;
        motionY = -20;
        motionYdenom = 10;
        ballSpeed = (int) Math.sqrt(motionX * motionX + motionY * motionY);
    }
    
    public Ball(int x, int y)
    {
        this();
        setLocation(x,y);
    }
    
    public static Bounds getBounds(GreenfootObject go)
    {
        Bounds b = new Bounds();
        int width = go.getWidth();
        int height = go.getHeight();
        b.lx = go.getX() - width / 2;
        b.ty = go.getY() - height / 2;
        b.rx = b.lx + width;
        b.by = b.ty + height;
        return b;
    }
    
    public void act()
    {
        //here you can create the behaviour of your object
        int newX = getX() + motionX / motionXdenom;
        int newY = getY() + motionY / motionYdenom;
        motionXaccum += motionX % motionXdenom;
        motionYaccum += motionY % motionYdenom;
        
        // X direction adjustments
        if (motionXaccum >= motionXdenom) {
            newX++;
            motionXaccum -= motionXdenom;
        }
        else if (motionXaccum < 0) {
            newX--;
            motionXaccum += motionXdenom;
        }

        // Y direction adjustments
        if (motionYaccum >= motionYdenom) {
            newY++;
            motionYaccum -= motionYdenom;
        }
        else if (motionYaccum < 0) {
            newY--;
            motionYaccum += motionYdenom;
        }

        // bounce off walls
        if (newX < BALLSIZEX / 2) {
            newX = BALLSIZEX / 2;
            motionX = -motionX;
        }
        else if (newX + BALLSIZEX / 2 >= BrickWorld.SIZEX) {
            newX = BrickWorld.SIZEX - BALLSIZEX / 2;
            motionX = -motionX;
        }
        
        if (newY < BALLSIZEY / 2) {
            newY = BALLSIZEY / 2;
            motionY = -motionY;
        }
        else if (newY + BALLSIZEY / 2 >= BrickWorld.SIZEY) {
            newY = BrickWorld.SIZEY - BALLSIZEY / 2;
            motionY = -motionY;
        }
        
        setLocation(newX, newY);
        int centerX = getX();
        int centerY = getY();
        
        // check for collision with bricks
        //Collection objs = getObjectsInRange(centerX, centerY, 30, GreenfootObject.class);
        //Collection objs = getObjectsInRange(30, GreenfootObject.class);
        Collection objs = getIntersectingObjects(GreenfootObject.class);
        Iterator i = objs.iterator();
        while (i.hasNext()) {
            GreenfootObject go = (GreenfootObject) i.next();
        
            if (go instanceof Brick) {
                Brick brick = (Brick) go;
                boolean collision = true; // assume a collision for now
                
                
                // can bounce off an edge, or bounce off the corner
                // if the center of the ball is between two edges, bounce
                // of an edge.
                Bounds brickBounds = getBounds(brick);
                if (Math.abs(brickBounds.ty - centerY) > 9 &&
                    Math.abs(brickBounds.by - centerY) > 9)
                  collision = false;
                else {
                    if (centerX >= brickBounds.lx && centerX <= brickBounds.rx) {
                        motionY = -motionY;
                    }
                    else if (centerY >= brickBounds.ty && centerY <= brickBounds.by) {
                        motionX = -motionX;
                    }
                    else {
                        // corner case (no pun intended...)
                        int cornerX;
                        int cornerY;
                        float forceX;
                        float forceY;
                        
                        // find corner X
                        if (centerX < brickBounds.lx)
                            cornerX = brickBounds.lx;
                        else
                            cornerX = brickBounds.rx;
                        
                        // find corner Y
                        if (centerY < brickBounds.ty)
                            cornerY = brickBounds.ty;
                        else
                            cornerY = brickBounds.by;
                        
                        // Find the collision vector, ie.
                        // the vector from the circle center
                        // to the corner
                        int collisionX = centerX - cornerX;
                        int collisionY = centerY - cornerY;
                        float collisionG = (float) collisionY / collisionX;

                        // Check the corner is close enough
                        float dist = (float) Math.sqrt(collisionX * collisionX + collisionY * collisionY);
                        if (dist <= 10) {

                            // the tangent vector is orthogonal to
                            // the collision vector
                            int tangentX = -collisionY;
                            int tangentY = collisionX;
                            
                            float d = (motionY - collisionG * motionX) /
                                        (collisionG * tangentX - tangentY);
                            
                            int mPointX = (int) (motionX + tangentX * d);
                            int mPointY = (int) (motionY + tangentY * d);
                            
                            // calculate new motion vecotr
                            int newMotionX = (int) (- motionX - 2 * tangentX * d);
                            int newMotionY = (int) (- motionY - 2 * tangentY * d);
        
                            motionX = newMotionX;
                            motionY = newMotionY;
                        }
                        else
                            collision = false;
                    }
                }
                if (collision)
                    brick.collide();
            }
            
            else if (go instanceof Paddle && motionY > 0) {
                motionY = -motionY;
                motionX += centerX - go.getX();
                if (newY > go.getY() - 10)
                    newY = go.getY() - 10;
                correctMotion();
            }
            
        }
    }
    
    /**
     * Adjust motion vectors (X,Y) to correct speed.
     */
    public void correctMotion()
    {
        float gotSpeed = (float) Math.sqrt(motionX * motionX + motionY * motionY);
        motionX *= ballSpeed / gotSpeed;
        motionY *= ballSpeed / gotSpeed;
    }
    
    public static class Bounds
    {
        public int lx; // left edge
        public int rx; // right edge
        public int ty; // top edge
        public int by; // bottom edge
    }
}
