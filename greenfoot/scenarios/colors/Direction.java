/**
 * A direction represents one of eight compas directions.
 * Each direction has an associated angle in degrees where EAST is 0 
 * degrees and it goes clock-wise from there. 
 *
 * @author Poul Henriksen
 * @version 0.5
 */
public enum Direction
{
    EAST(0), 
    SOUTH_EAST(45), 
    SOUTH(90), 
    SOUTH_WEST(135), 
    WEST(180), 
    NORTH_WEST(225), 
    NORTH(270), 
    NORTH_EAST(315);
    
    /** Degrees between directions */
    public final static int TURN = 45;
    
    /** Angle corresponding to this direction */
    private int angle;
    
    /**
     * Constructor used to associate an angle with the direction.
     * @param angle in degrees from 0-359
     */
    Direction(int angle)
    {
        this.angle = angle;
    }

    /** 
     * Gets the direction that corresponds to the given angle. 
     * East is 0 degrees and it goes clock-wise from there.
     * 
     * @param angle in degrees from 0-359
     */
    public static Direction getDirection(int angle) 
    {
        //First we normalise the angle so that: 0 <= angle < 360
        int normalisedAngle = angle % 360;
        
        //Make sure there are no negative values
        if(normalisedAngle < 0) {
            normalisedAngle = 360 + normalisedAngle;
        }
        
        switch(normalisedAngle) {
            case 0:
                return EAST;
            case 45:
                return SOUTH_EAST;
            case 90:
                return SOUTH;
            case 135:
                return SOUTH_WEST;
            case 180:
                return WEST;
            case 225:
                return NORTH_WEST;
            case 270:
                return NORTH;
            case 315:
                return NORTH_EAST;
            default:
                throw new RuntimeException("No direction exists for angle: " + angle + "  Normalised to: " + normalisedAngle) ;                
        }  
    }
    
    /**
     * Get the direction closest to the direction of the vector with the
     * x-component dx and y-component dy.
     * 
     * @param dx x-component of the vector
     * @param dy y-component of the vector
     */
    public static Direction getDirection(int dx, int dy) 
    {
        double radians = Math.atan2(dy, dx);
        double degrees = Math.toDegrees(radians);
        double turns = degrees / TURN;
        int turnsRounded = (int) Math.round(turns);
        int angle = turnsRounded * TURN;
        return getDirection(angle);        
    }
    
    /**
     * Gets the next direction to the left of this direction (counter clock-wise).
     */
    public Direction getLeft() 
    {
        return getDirection(angle - TURN);
    }
    
    /**
     * Gets the next direction to the right of this direction (clock-wise).
     */
    public Direction getRight() 
    {
        return getDirection(angle + TURN);
    }
    
    /**
     * Get the x component of the vector that points in this direction.
     * 
     * @return -1, 0 or 1 
     */
    public int getDeltaX() 
    {
        double cos = Math.cos( Math.toRadians(angle) );
        
        if(cos < -0.2) {
            return -1;
        }
        else if(cos > 0.2) {
            return 1;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Get the y component of the vector that points in this direction.
     * 
     * @return -1, 0 or 1 
     */
    public int getDeltaY() 
    {    
        double sin = Math.sin( Math.toRadians(angle) );
        if(sin < -0.2) {
            return -1;
        }
        else if(sin > 0.2) {
            return 1;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Get the angle between this direction and the x-axis.
     * 
     * @return angle in degrees from 0-359
     */
    public int getAngle() 
    {
        return angle;
    }
}