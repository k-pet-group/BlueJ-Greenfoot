import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * The City class sets up the world.
 * It places all the baskets, the bagle shooter
 * and holds the wind object.
 * 
 * When placing the baskets and the bagle shooter,
 * it looks for the colour of ground on it's background.
 * When it finds this colour, it has found the top of the land
 * and places an object there.
 * If it does not find land, the object will be placed at
 * the bottom of the world.
 * 
 * The shape of the land can be edited in the 'land.png' image,
 * which is drawn on top of the background before it is set.
 * 
 * @author Joseph Lenton
 * @version 13/03/07
 */
public class City extends World
{
    // the gravity in the world
    public static final double GRAVITY = 0.2;
    
    // the number of baskets to have in the game
    private static final int NUMBER_OF_BASKETS = 3;
    
    // how off from the perfect basket x position, the basket will be
    private static final int BASKET_OFFSET = 80;
    
    // the minimum and maximum Basket position along the x axis
    private static final int BASKET_X_MIN = 250;
    private static final int BASKET_X_MAX = 760;
    
    // the x position of the Bagle Shooter
    private static final int BAGLE_SHOOTER_X = 60;
    // how high above the ground the bagle shooter sits
    private static final int BAGLE_SHOOTER_HEIGHT = 100;
    
    // the color of land
    private static final Color LAND_COLOR = new Color(6, 192, 15);
    
    private Wind wind;
    
    /**
     * Constructor for the City.
     */
    public City()
    {
        // Create a new world with 20x20 cells with a cell size of 10x10 pixels.
        super(800, 600, 1);
        
        // load in the background image
        GreenfootImage background = new GreenfootImage("city_background.png");
        // draw the land on top of the image
        background.drawImage(new GreenfootImage("land.png"), 0, 0);
        setBackground(background);
        
        wind = new Wind();
        addObject(wind, getWidth()/2, 50);
        placeBagleShooter();
        placeBaskets();
    }
    
    /**
     * Reads the background for land and places a new
     * DeliveryMan if it can find land.
     */
    private void placeBagleShooter()
    {
        // get the background image
        GreenfootImage background = getBackground();
        
        // checks pixels working down the background image,
        // trying to find land
        for (int y = 0; y < getHeight(); y++) {
            // get the color at that position
            Color pixel = getColorAt(BAGLE_SHOOTER_X, y);
            
            // if it is equal to the lang color,
            // or at the bottom of the world
            if (isLandColor(pixel) || y == getHeight()-1) {
                // so I add the DeliveryMan 1 pixel above the ground
                addObject(
                    new BagleShooter(),
                    BAGLE_SHOOTER_X,
                    y - BAGLE_SHOOTER_HEIGHT
                );
                
                addObject(
                    new BagleShooterBody(),
                    BAGLE_SHOOTER_X + BagleShooter.BODY_OFFSET_X,
                    y - BAGLE_SHOOTER_HEIGHT + BagleShooter.BODY_OFFSET_Y
                );
                
                // and end the loop
                break;
            }
        }
    }
    
    /**
     * Reads the background for land and places a new
     * Basket if it can find land.
     */
    private void placeBaskets()
    {
        // get the background image
        GreenfootImage background = getBackground();
        
        // calculate how much to step by in the for loop
        int basketStep = (BASKET_X_MAX-BASKET_X_MIN) / NUMBER_OF_BASKETS;
        // the starting basket position
        int basketX = BASKET_X_MIN;
        
        // iterate for each basket
        for (int i = 0; i < NUMBER_OF_BASKETS; i++) {
            // find the x position for this basket
            // which is the basketX position, plus or minus up to half the basket offset.
            int x = basketX + Greenfoot.getRandomNumber(BASKET_OFFSET) - BASKET_OFFSET/2;
            // increase the basketX
            basketX += basketStep;
            
            // checks pixels working down the background image,
            // trying to find land
            for (int y = 0; y < getHeight(); y++) {
                // get the color at that position
                Color pixel = getColorAt(x, y);
                
                // if it is equal to the lang color,
                // or if it is at the bottom of the world
                if (isLandColor(pixel) || y == getHeight()-1) {
                    // I add the DeliveryMan 1 pixel above the ground
                    addObject( new Basket(), x, y-1);
                    
                    // end the loop
                    break;
                }
            }
        }
    }
    
    /**
     * To find out if the color given is the same color
     * as the color of land.
     * 
     * @return true if the pixel is the same color as land
     */
    public boolean isLandColor(Color color)
    {
        return color.getRed() == LAND_COLOR.getRed() &&
                color.getBlue() == LAND_COLOR.getBlue() &&
                color.getGreen() == LAND_COLOR.getGreen();
    }
    
    /**
     * Pauses the game
     * 
     * @param the number of steps to pause
     */
    public void pause(int steps)
    {
        for (int i = 0; i < steps; i++) {
            Greenfoot.delay(1);
        }
    }
    
    /**
     * Returns the current srength of the wind.
     * 
     * @return the current strength of the wind.
     */
    public double getWindStrength()
    {
        return wind.getWindStrength();
    }
}
