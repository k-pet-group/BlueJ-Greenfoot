import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;
import java.awt.Font;

/**
 * This is Earth. Or at least some remote, uninhabited part of Earth. Here, Greeps can
 * land and look for piles of tomatoes...
 * 
 * @author Michael Kolling
 * @author Davin McCall
 * @version 2.0
 */
public class Earth extends World
{
    public static final boolean PLAY_SOUNDS = true;

    public static final int SCORE_DISPLAY_TIME = 240;
    
    private GreenfootImage map;
    private Ship ship1;
    private Ship ship2;
    private Timer timer;
    private int currentMap;
    private boolean firstStart;
    
    /* The first two 3-tuples are the ships: target (landing) y-coordinate, then initial x and y. */
    /* Remaining 3-tuples: number of tomatoes, x co-ordinate, y co-ordinate */
    private int[][][] mapData = {
        
         { {240, 400, 0}, {360, 400, 599},
           {14, 275, 43}, {14, 547, 541},       // map 1
           {30, 114, 531}, {30, 663, 61} },
           
         { {305, 700, 0}, {305, 100, 0},
           {60, 400, 300},
           {7, 100, 500}, {7, 700, 500},        // map 2
           {12, 100, 50}, {12, 700, 50} },


         { {480, 640, 0},{480, 160, 0},
           {25, 40, 50}, {30, 50, 550},         // map 3
           {25, 760, 50}, {30, 750, 549},
           {50, 400, 220}, {50, 400, 440}} ,

         { {280, 310, 0},  {280, 490, 599},
           {50, 385, 52}, {50, 404, 523} }     // map 4       

     };

    private int[][] scores;
    
    /**
     * Create a new world. 
     */
    public Earth()
    {
        super(800, 600, 1);
        currentMap = 0;
        firstStart = true;
        scores = new int[2][mapData.length];    // one score for each map
        setPaintOrder(ScoreBoard.class, Counter.class, Smoke.class, Ship.class, Greep.class, TomatoPile.class);
    }
    
    /**
     * We detect the first to have a small delay for showing the start-screen.
     */
    public void started()
    {   
        if (firstStart) {
            showMap(currentMap);
            firstStart = false;
            Greenfoot.delay(50);
        }
    }
    
    /**
     * Return true, if the specified coordinate shows water.
     * (Water is defined as a predominantly blueish color.)
     */
    public boolean isWater(int x, int y)
    {
        Color col = map.getColorAt(x, y);
        return col.getBlue() > (col.getRed() * 2);
    }
    
    /**
     * Jump to the given map number (1..n).
     */
    public void jumpToMap(int map)
    {
        clearWorld();
        currentMap = map-1;
        showMap(currentMap);
        firstStart = false;
    }
    
    /**
     * Set up the start scene.
     */
    private void showMap(int mapNo)
    {
        map = new GreenfootImage("map" + mapNo + ".jpg");
        setBackground(map);
        Counter mapTitle = new Counter("Map ", mapNo+1);
        addObject(mapTitle, 60, 20);
        int[][] thisMap = mapData[mapNo];
        for(int i = 2; i < thisMap.length; i++) {
            int[] data = thisMap[i];
            addObject(new TomatoPile(data[0]), data[1], data[2]);
        }
        
        
        int [] shipData1 = thisMap[0];
        int [] shipData2 = thisMap[1];
        if (Greenfoot.getRandomNumber(2) == 0) {
            shipData2 = thisMap[0];
            shipData1 = thisMap[1];
        }        
        
        // First ship
        ship1 = new Ship("spaceship-green.png", shipData1[0], 1);
        addObject(ship1, shipData1[1], shipData1[2]);
        
        // Second ship
        ship2 = new Ship("spaceship-purple.png", shipData2[0], 2);
        addObject(ship2, shipData2[1], shipData2[2]);        
        
        // Timer starts when both ships have landed
        timer = null;
    }
    
    /**
     * The Earth's act method opens the ships' hatches when they are in position and 
     * starts the timer to check when the map run on this map should end.
     */
    public void act()
    {
        if (timer == null && ship1.inPosition() && ship2.inPosition()) {
            timer = new Timer(ship1, ship2);
            addObject(timer, 700, 570);
            
            ship1.openHatch();
            showAuthor(ship1);
            ship2.openHatch();
            showAuthor(ship2);
        }
    }
    
    /**
     * Game is over. Stop running, display score.
     */
    public void mapFinished(int time)
    {
        displayScore(time);
        Greenfoot.delay(SCORE_DISPLAY_TIME);
        clearWorld();
        currentMap++;
        if(currentMap < mapData.length) {
            showMap(currentMap);
        }
        else {
            displayFinalScore();
            Greenfoot.stop();
        }
    }
    
    /**
     * Write the author names on the background.
     */
    private void showAuthor(Ship ship)
    {
        GreenfootImage im = getBackground();
        Font font = im.getFont();
        font = font.deriveFont(14f);
        im.setFont(font);
        im.drawString(ship.getGreepName(), ship.getX()-40, ship.getY()-36);
    }
    
    /**
     * Display the score board for the result on this map.
     * 
     * @param time How many act loops the score board should be shown for.
     */
    private void displayScore(int time)
    {
        int points1 = ship1.getTomatoCount();
        int points2 = ship2.getTomatoCount();
        scores[0][currentMap] = points1;
        scores[1][currentMap] = points2;
        String[] authors = new String[]{ship1.getGreepName(), ship2.getGreepName()};
        ScoreBoard board = new ScoreBoard(authors, currentMap, scores);
       
        addObject(board, getWidth() / 2, getHeight() / 2);
    }
    
    /**
     * Display the score board with the final results.
     */
    private void displayFinalScore()
    {
        clearWorld();
        String[] authors = new String[]{ship1.getGreepName(), ship2.getGreepName()};
        ScoreBoard board = new ScoreBoard(authors,  scores);
        addObject(board, getWidth() / 2, getHeight() / 2);
    }
    
    /**
     * Remove everything from the world.
     */
    private void clearWorld()
    {
        removeObjects(getObjects(null));
    }
}
