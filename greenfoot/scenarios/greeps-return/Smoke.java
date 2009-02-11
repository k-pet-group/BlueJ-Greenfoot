import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * A puff of smoke. Once placed into the world, it quickly evaporaes and
 * then disappears.
 * 
 * @author Michael Kolling
 * @version 1.0
 */
public class Smoke extends Actor
{
    private static final int SPREAD = 30;
    private static final GreenfootImage image1 = new GreenfootImage("smoke-green.png");
    private static final GreenfootImage image2 = new GreenfootImage("smoke-purple.png");
    private int delay;
    
    private GreenfootImage image;   // the original image
    private boolean teamTwo;
    private int fade;               // the rate of fading
    private int children;
    
    public Smoke(int children, boolean isTeamTwo)
    {
        teamTwo = isTeamTwo;
        if(isTeamTwo) {
            image = image2;
        }
        else {
            image = image1;
        }
        fade = Greenfoot.getRandomNumber(4) + 1;
        if (fade > 3) {
          fade = fade - 2;
        }
        delay = Greenfoot.getRandomNumber(4)+1;
        this.children = children;
    }

    /**
     * In every step, get smaller until we disappear.
     */
    public void act() 
    {
        delay--;
        if (delay == 0) {
            spawn();
        }
        shrink();
    }    
    
    private void spawn()
    {
        for (int i = 0; i < children; i++) {
            int x = getX() + Greenfoot.getRandomNumber(SPREAD) - (SPREAD/2);
            int y = getY() + Greenfoot.getRandomNumber(SPREAD) - (SPREAD/2);
            getWorld().addObject( new Smoke(children-2, teamTwo), x, y);
        }
    }

    private void shrink()
    {
        if(getImage().getWidth() < 10) {
            getWorld().removeObject(this);
        }
        else {
            GreenfootImage img = new GreenfootImage(image);
            img.scale ( getImage().getWidth()-fade, getImage().getHeight()-fade );
            setImage (img);
        }
    }
}
