package greenfoot;

import java.net.URL;

import javax.swing.ImageIcon;


/**
 * A GreenfootObject is a thing that can be in a world. To be in a world means
 * that it has a graphically representation and a location. Futhermore it has an
 * act method which will be called when the simulation is started.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootObject.java 3238 2004-12-14 18:43:54Z polle $
 */
public class GreenfootObject
{

    private int x;
    private int y;
    private double rotation = 0;
    private GreenfootWorld world;
    private Image image;

    /**
     * Constructor that creates an object with a default image.
     *  
     */
    public GreenfootObject()
    {
        setImage("greenfoot/greenfoot-logo.png");
    }
    
    /**
     * Constructor that creates an object with a default image and a specified
     * location.
     */
    public GreenfootObject(int x, int y)
    {
        setImage("greenfoot/greenfoot-logo.png");
        this.x = x;
        this.y = y;
    }

    /**
     * This method is called each time the object should do its stuff. This
     * implementation does nothing, so it should be overridden to create custom
     * behaviour for the objects.
     *  
     */
    public void act()
    {

    }

    /**
     * Get the current location of the object in the world.
     * 
     * @return The x-axis location
     */
    public int getX()
    {
        return x;
    }

    /**
     * Get the current location of the object in the world.
     * 
     * @return The y-axis location
     */
    public int getY()
    {
        return y;
    }
    
    /**
     * Get the width of object (based on the width of the image).
     * 
     */
    public int getWidth()
    {
        if(image != null) {
            return image.getWidth();
        } else {
            return -1;
        }
    }


    /**
     * Get the height of object (based on the width of the image)
     * 
     */
    public int getHeight()
    {
        if(image != null) {
            return image.getHeight();
        } else {
            return -1;
        }        
    }    
    
    /**
     * Gets the rotation of the object. <br>
     * 
     * Zero degrees is to the east. The angle is clockwise from this.
     * 
     * @see #setRotation(double)
     * 
     * @return The rotation in degress
     */
    public double getRotation()
    {
        return rotation;
    }

    /**
     * Sets the rotation of the object. <br>
     * 
     * Zero degrees is to the east. The angle is clockwise from this.
     * 
     * @param rotation
     *            The rotation in degress
     */
    public void setRotation(double rotation)
    {
        this.rotation = rotation;
    }

    /**
     * Sets a new location for this object.
     * 
     * <br>
     * 
     * If this method is overridden it is important to call this method with
     * super.setLocation(x,y) at the end of the overriding method.
     * 
     * @param x
     *            Location on the x-axis
     * @param y
     *            Location on the y-axis
     */
    public void setLocation(int x, int y)
    {
        if (world != null) {
            if (world.getWidth() <= x || x < 0) {
                return;
            }
            if (world.getHeight() <= y || y < 0) {
                return;
            }
        }
        int oldX = this.x;
        int oldY = this.y;

        this.x = x;
        this.y = y;
        if (world != null) {
            world.updateLocation(this, oldX, oldY);
        }
    }
 
    /**
     * Gets the world that this object lives in
     * 
     * @return The world
     */
    public GreenfootWorld getWorld()
    {
        return world;
    }

    /**
     * Returns an image representing this GreenfootObject.
     * 
     * @return The image
     */
    public Image getImage()
    {
        return image;
    }

    /**
     * Sets the image of this object to the one specified by the filename. <br>
     * The file should be located in the project directory.
     * 
     * 
     * @see #setImage(ImageIcon)
     * @param filename
     *            The filename of the image.
     */
    public void setImage(String filename)
    {
        URL imageURL = this.getClass().getClassLoader().getResource(filename);
        if (imageURL != null) {
            image = new Image(imageURL);
        }
    }

    /**
     * Sets the image of this object <br>
     * 
     * @see #setImage(String)
     * @param image
     *            The image.
     */
    final public void setImage(Image image)
    {
        this.image = image;
    }


    /**
     * Determines whether the given pixel location is considered to be inside this
     * object. <br>
     * This implementation uses the size of the image.
     * 
     * @param x
     *            The x-position relative to the location of the object
     * @param y
     *            The y-position relative to the location of the object
     * @return True if the image contains the point. If it has no image it will
     *         return false.
     */
    public boolean contains(int x, int y)
    {
        //TODO this disregards rotations. maybe this should be updated in the
        // getWidth/height methods
        if (image != null) {
            int width = world.toCellCeil(getWidth());
            int height = world.toCellCeil(getHeight());
            return intersects(x, y, 0, 0, width, height);
        }
        else {
            return false;
        }
    }

    /**
     * Determines if the given position intersects with the rectangle.
     */
    private boolean intersects(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight)
    {
        if (x >= rectX && x < (rectX + rectWidth) && y >= rectY && y < (rectY + rectHeight)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    
    
    /**
     * 
     * Translates the given location into cell-coordinates before setting the location.
     * 
     * @param x x-coordinate in pixels
     * @param y y-coordinate in pixels
     */
    void setLocationInPixels(int x, int y)
    {
        if (world != null) {
            setLocation(world.toCellFloor(x), world.toCellFloor(y));
        }
    }

    /**
     * Sets the world of this object
     * 
     * @param world
     */
    void setWorld(GreenfootWorld world)
    {
        //TODO Possible error if its location is out of bounds
        this.world = world;
    }


}