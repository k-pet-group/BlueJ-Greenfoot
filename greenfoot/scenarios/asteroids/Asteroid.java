import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A rock in space
 * 
 * @author Poul Henriksen
 */
public class Asteroid extends MovingThing
{
    /** Size of this asteroid */
    private int size;
    /** Whether is is exploded */
    private boolean exploded = false;
    /** When the health reaches 0 the asteroid will explode*/
    private int health;

    
    public Asteroid()
    {
        this(64);
    }
    
    public Asteroid(int size)
    {
        super(new Vector(Greenfoot.getRandomNumber(360), 2));
        setSize(size);
    }
    
    public Asteroid(int size, Vector speed)
    {
        super(speed);
        setSize(size);
    }
    
    public void act()
    {         
        if(exploded) return;
        move();
    }

    public void setSize(int size) {
        health = size;
        this.size = size;
        GreenfootImage image = new GreenfootImage("rock.gif");
        image.scale(size, size);
        setImage(image);
    }
    
    /**
     * Explodes this asteroid into two smaller asteroids
     */
    private void explode() {
        if(exploded) return;
        exploded = true;
        
        if(size <= 16) {
            getWorld().removeObject(this);
            return;
        }
        
        int r = getSpeed().getDirection() + Greenfoot.getRandomNumber(45);
        double l = getSpeed().getLength();
        Vector speed1 = new Vector(r + 90, l * 1.2);
        Vector speed2 = new Vector(r - 90, l * 1.2);        
        Asteroid a1 = new Asteroid(size/2, speed1);        
        Asteroid a2 = new Asteroid(size/2, speed2);
        getWorld().addObject(a1, getX(), getY());
        getWorld().addObject(a2, getX(), getY());        
        a1.move();
        a2.move();
    
        getWorld().removeObject(this);
    }
    
    /**
     * Hit this asteroid dealing the given amount of damage.
     */
    public void hit(int damage) {
        if(exploded) return;
        health = health - damage;
        if(health <= 0) 
            explode();         
    }
}