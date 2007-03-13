import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * The Wind class is to draw and move the Wind bar at the
 * top of the screen. It's wind value is for affecting the
 * Bagle's x momentum.
 * 
 * When the wind changes it's strength,
 * the speed of the wind change is designed to be
 * that of a curve. Using steps from 0 to 180,
 * it will determine how far from the futureWindStrength
 * the current windStrength should be set to.
 * 
 * At 180 windStrength and futureWindStrength are equal,
 * so a new futureWindStrength is calculated.
 * 
 * @author Joseph Lenton 
 * @version 13/03/07
 */
public class Wind extends Actor
{
    private static final double MAXIMUM_WIND_STRENGTH = 0.2;
    private static final int WIND_CHANGE_VALUE = 180;
    
    // the images used for drawing the arrow and it's body.
    // WIND_LEFT_ARROW is also used to find the height for all images.
    private GreenfootImage leftArrowImage;
    private GreenfootImage leftBodyImage;
    private GreenfootImage rightArrowImage;
    private GreenfootImage rightBodyImage;
    // the 'wind-o-meter' image overlay
    private GreenfootImage windMeterImage;
    
    // the width of the actors image,
    private int arrowWidth;
    private int arrowHeight;
    
    private double imageScale;
    
    // the current strength of the wind
    private double windStrength;
    // the future strength of the wind
    private double futureWindStrength;
    // half the distance between the windStrength and futureWindStrength,
    // from when the futureWindStrength was calculated.
    private double windStrengthHalfDistance;
    
    // the number of steps from the futureWindStrength
    private int windStep;
    // the speed that the windStep will incriment by
    private int windStepSpeed;
    
    /**
     * Constructor for instances of the Wind class.
     * Loads in all the winds images.
     */
    public Wind()
    {
        // the wind images
        leftArrowImage = new GreenfootImage("wind_left_arrow.png");
        leftBodyImage = new GreenfootImage("wind_left_body.png");
        rightArrowImage = new GreenfootImage("wind_right_arrow.png");
        rightBodyImage = new GreenfootImage("wind_right_body.png");
        windMeterImage = new GreenfootImage("wind_meter.png");
        
        // I want the left and right arrow to, at most,
        // be only be half the width of the windMeter image overlay.
        arrowWidth = windMeterImage.getWidth()/2;
        arrowHeight = leftArrowImage.getHeight();
        
        // this calculates the number of pixels in the image width,
        // for each unit of wind strength
        imageScale = arrowWidth / MAXIMUM_WIND_STRENGTH;
        
        setImage( windMeterImage );
        
        windStrength = 0.0;
        windStep = 180;
        
        changeWindStrength();
    }
    
    /**
     * Changes the wind strength, and incriments the wind step.
     */
    public void act() 
    {
        changeWindStrength();
        
        windStrength = calculateWindStrength();
        windStep += windStepSpeed;
        updateImage();
    }
    
    /**
     * Checks if the wind strength should be changed,
     * and if it should be it will calculate a new wind strength
     * for the future wind strength and the speed it should travel
     * at to reach that wind strength.
     * If it should not change the wind strength, it will not.
     */
    private void changeWindStrength()
    {
        // if the wind strength should change
        if ( windStep >= WIND_CHANGE_VALUE ) {
            futureWindStrength = getRandomWindStrength();
            
            windStrengthHalfDistance = (futureWindStrength-windStrength) / 2;
            
            windStepSpeed = Greenfoot.getRandomNumber(4)+1;
            windStep = 0;
        }
    }
    
    /**
     * Calculates a new random wind strength
     * between (but not including) the negative maximum wind strength
     * and the positive maximum wind strength.
     * 
     * @return a valid random wind strength
     */
    private double getRandomWindStrength()
    {
        return ( (Greenfoot.getRandomNumber(98)+1) / 100.0) * (2*MAXIMUM_WIND_STRENGTH) - MAXIMUM_WIND_STRENGTH;
    }
    
    /**
     * calculates what the wind strength should currently be
     * and then returns it.
     * 
     * @return the wind strength
     */
    private double calculateWindStrength()
    {
        return futureWindStrength - windStrengthHalfDistance - windStrengthHalfDistance * Math.cos(Math.toRadians(windStep));
    }
    
    /**
     * Checks if the given windStrength is valid,
     * where it is between the maximum negative
     * and positive wind strengths.
     * 
     * @param windStrength the strength of the wind to check.
     * @return true if the windStrength is a valid value, false if not.
     */
    private boolean validWindStrength(double windStrength)
    {
        return (windStrength >= -MAXIMUM_WIND_STRENGTH &&
                windStrength <= MAXIMUM_WIND_STRENGTH);
    }
    
    /**
     * Updates the image of the Wind instance.
     * This includes re-drawing the arrows onto the image.
     */
    private void updateImage()
    {
        // first I clear the image of the actor
        getImage().clear();
        
        // if the wind strength is negative, I will draw only the left side
        if (windStrength < 0) {
            // I clear the left image
            GreenfootImage drawingImage = new GreenfootImage(arrowWidth, arrowHeight);
            
            // this is the x position for each image to be drawn, onto this image (drawingImage)
            // it's starting value is the strength of the wind, multiplied by the scale,
            // and subtract this from the image's width
            int windImageX = (int) (arrowWidth + imageScale*windStrength);
            // I draw the left arrow head at this position
            drawingImage.drawImage(
                leftArrowImage,
                windImageX,
                0);
            
            // then add on the width of the image we just drew,
            // so we now draw directly after it
            windImageX += leftArrowImage.getWidth();
            
            // the number of arrow body sections is unknown,
            // so I iterate from the current x position (windImageX)
            // until I am off the end of leftArrowImage
            for (int x = windImageX; x < arrowWidth; x += leftBodyImage.getWidth()) {
                // I draw a body section
                drawingImage.drawImage(
                    leftBodyImage,
                    x,
                    ( leftArrowImage.getHeight() - leftBodyImage.getHeight() ) /2);
            }
            
            // I now draw the left arrow to the wind actor's image
            getImage().drawImage(drawingImage, 0, 9);
        }
        // if the wind strength is positive, I will draw only the right side
        else if (windStrength > 0) {
            // I clear the left image
            GreenfootImage drawingImage = new GreenfootImage(arrowWidth, arrowHeight);
            
            // set it to the strength of the wind, multiplied by the scale
            // to find the beginning pixels
            int windImageX = (int) ( imageScale*windStrength - rightArrowImage.getWidth() );
            
            // I draw the arrow head
            drawingImage.drawImage(
                rightArrowImage,
                windImageX,
                0);
            
            // as I am drawing right to left, not left to right,
            // I have to subtract the width of the image I am drawing on each iteration.
            // This is until x is less then minus the width of the arrow's body image,
            // as then the arrow body section will not appear if drawn
            for (int x = windImageX-rightBodyImage.getWidth(); x > -rightBodyImage.getWidth(); x -= rightBodyImage.getWidth()) {
                // draw the arrow body section
                drawingImage.drawImage(
                    rightBodyImage,
                    x,
                    ( rightArrowImage.getHeight() - rightBodyImage.getHeight() ) /2
                    );
            }
            
            // I now draw the right arrow to the wind actor's image
            getImage().drawImage(drawingImage, getImage().getWidth()/2, 9);
        }
        
        getImage().drawImage(windMeterImage, 0, 0);
    }
    
    /**
     * Returns the current wind strength.
     * 
     * @return the current wind strength.
     */
    public double getWindStrength()
    {
        return windStrength;
    }
}