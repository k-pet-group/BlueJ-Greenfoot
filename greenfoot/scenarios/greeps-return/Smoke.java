import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * A puff of smoke. Once placed into the world, it quickly evaporates and
 * then disappears.
 * 
 * @author Michael Kolling
 * @version 1.0
 */
public class Smoke extends Actor
{
    private static final int SPREAD = 50;
    private static final GreenfootImage image1 = new GreenfootImage("smoke-green.png");
    private static final GreenfootImage image2 = new GreenfootImage("smoke-purple.png");
    private int delay;
    
    private boolean teamTwo;
    private int fade;               // the rate of fading
    private int children;
    
    public Smoke(int children, boolean isTeamTwo)
    {
        teamTwo = isTeamTwo;
        if(isTeamTwo) {            
            setImage(new GreenfootImage(image2));
        }
        else {
            setImage(new GreenfootImage(image1));
        }
        
        fade = Greenfoot.getRandomNumber(10) +5;
        
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
        if(Greenfoot.getRandomNumber(3) > 0) {
            shrink();
        }
    }    
    
    private void spawn()
    {
        for (int i = 0; i < children; i++) {
            int x = getX() + Greenfoot.getRandomNumber(SPREAD) - (SPREAD/2);
            int y = getY() + Greenfoot.getRandomNumber(SPREAD) - (SPREAD/2);
            getWorld().addObject( new Smoke(children-3, teamTwo), x, y);
        }
    }

    private void shrink()
    {
        GreenfootImage img = getImage();
        int trans = getImage().getTransparency();
        if(trans < 30) {
            getWorld().removeObject(this);
        } else {
            getImage().setTransparency(trans - fade);
        }
    }
}
