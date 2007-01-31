import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * PirateCove Class
 * 
 * The PirateCove is for creating the Pirate's world.
 * The various methods are for settings up different
 * aspects of the world, such as the locations of the
 * Pirate's home, rocks and treasure.
 */
public class PirateCove extends World
{
    /**
     * Creates a new world, with 800x600 cells.
     * Each cell is only 1 pixel in size.
     */
    public PirateCove() {
        super(800, 600, 1);
        setBackground("map edges.png");
        addFlags();
        addRocksLeft();
        addRocksRight();
        addPortals();
        addTreasure();
        addObject(new Home(), 650, 100);
        addObject(new Native(), 400, 300);
        addObject(new Native(), 400, 200);
//        addFlags2();
    }
    
    /**
     * Creates and locates all the Rock instances
     * which appear on the left of PirateCove.
     */
    private void addRocksLeft()
    {
        for (int i = 0; i < 14; i++) {
            addObject(new Rock(), i*30, 360+i*i);
        }
    }
    
    /**
     * Creates and locates all the Rock instances
     * which appear on the right of PirateCove.
     */
    private void addRocksRight()
    {
        for (int i = 0; i < 10; i++) {
            addObject(new Rock(), getWidth()-1-i*30, 300+(i*i));
        }
        addObject(new Rock(), 520, 400);
        addObject(new Rock(), 525, 420);
        addObject(new Rock(), 530, 440);
    }
    
    /**
     * Creates and locates all the Flag instances
     * in PirateCove.
     */
    private void addFlags()
    {
        for (int i = 40; i < 240; i += 24) {
            addObject(new UpFlag(), i, 40);
            addObject(new DownFlag(), i, 240);
            addObject(new LeftFlag(), 40, i);
            addObject(new RightFlag(), 240, i);
        }
    }
    
    /**
     * An alternate set up for creating and locating
     * all the Flag instances in PirateCove.
     */
    private void addFlags2()
    {
        int midX = getWidth()/2;// - distance/2;
        int midY = getHeight()/2;// - distance/2;
        
        for (int i = 60; i < 260; i += 24) {
            addObject(new UpFlag(), midX+i, midY);
            addObject(new RightFlag(), midX, midY+i);
            addObject(new DownFlag(), midX-i, midY);
            addObject(new LeftFlag(), midX, midY-i);
        }
    }
    
    /**
     * Creates and locates all the Treasure instances
     * in PirateCove.
     */
    private void addTreasure()
    {
        // behind left rocks
        addObject(new Treasure(), 100, 500);
        // behind right rocks
        addObject(new Treasure(), 720, 440);
        // in the middle of the flags
        addObject(new Treasure(), 120, 120);
        addObject(new Treasure(), 140, 160);
        addObject(new Treasure(), 160, 120);
    }
    
    /**
     * Creates and locates all the Portal instances
     * in PirateCove.
     */
    private void addPortals()
    {
        Portal portalA = new Portal();
        Portal portalB = new Portal(portalA);
        portalA.setExitPortal(portalB);
        
        addObject(portalA, 700, 60);
        addObject(portalB, 340, 150);
//        addObject(portalB, 280, 500);
    }
}