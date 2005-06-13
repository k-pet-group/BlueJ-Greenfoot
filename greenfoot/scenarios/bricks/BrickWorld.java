import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import greenfoot.GreenfootImage;

import java.awt.Color;

public class BrickWorld extends GreenfootWorld
{
    public static int SIZEX = 400;
    public static int SIZEY = 300;
    
    public static int BRICKSPACING = 10;
    public static int BRICKLEFTBORDER = 30;
    public static int BRICKRIGHTBORDER = 30;
    public static int BRICKWIDTH = 30;
    public static int BRICKHEIGHT = 15;
    public static int BRICKTOPBORDER = 10;
    
    /**
     * A brave new BrickWorld.
     */
    public BrickWorld() {
        super(SIZEX, SIZEY, 1, false);
        GreenfootImage background = new GreenfootImage(20,20);
        background.fill(Color.BLACK);
        background.setTiled(true);
        setBackground(background);
        
        for (int i = BRICKLEFTBORDER + BRICKWIDTH / 2; i+BRICKWIDTH/2 < SIZEX - BRICKRIGHTBORDER; i+=BRICKWIDTH+BRICKSPACING) {
            for (int j = 0; j < 4; j++ ) {
                Brick newBrick = new Brick();
                newBrick.setLocation(i, BRICKTOPBORDER + (BRICKHEIGHT + BRICKSPACING) * j);
                addObject(newBrick);
            }
        }
        
        addObject(new Ball(200, 120));
        addObject(new Paddle(200, SIZEY - 20));
    }
}
