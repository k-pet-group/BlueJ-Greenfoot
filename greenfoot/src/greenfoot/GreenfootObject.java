package greenfoot;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * A GreenfootObject is a thing that can be in a world. To be in a world means
 * that it has a graphically representation and a location. Futhermore it has an
 * act method which will be called when the simulation is started.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootObject.java 3198 2004-11-29 01:32:03Z davmac $
 */
public class GreenfootObject
{

    private int x;
    private int y;
    private double rotation = 0;
    private GreenfootWorld world;
    private ImageIcon image;

    private final static Component comp = new Canvas();

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
     * Get the current objects location in the world
     * 
     * @return The x-axis location
     */
    public int getX()
    {
        return x;
    }

    /**
     * Get the current objects location in the world
     * 
     * @return The y-axis location
     */
    public int getY()
    {
        return y;
    }
    
    /**
     * Get the width of object (based on the width of the image)
     * 
     */
    public int getWidth()
    {
        if(image != null) {
            return image.getIconWidth();
        }
        return 0;
    }


    /**
     * Get the height of object (based on the width of the image)
     * 
     */
    public int getHeight()
    {
        if(image != null) {
            return image.getIconHeight();
        }
        return 0;
    }    
    

    /**
     * Gets the rotation in degrees.
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
     * Sets the world of this object
     * 
     * @param world
     */
    void setWorld(GreenfootWorld world)
    {
        //TODO Possible error if its location is out of bounds
        this.world = world;
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
    public ImageIcon getImage()
    {
        return image;
    }

    /**
     * Sets the image of this object to the one specified by the filename. <br>
     * The file should be located in the project directory.
     * 
     * @param filename
     *            The filename of the image.
     */
    public void setImage(String filename)
    {
        URL imageURL = this.getClass().getClassLoader().getResource(filename);
        if (imageURL != null) {
            setImage(new ImageIcon(imageURL));
        }
    }

    /**
     * Sets the image of this object <br>
     * 
     * @see #setImage(String)
     * @param filename
     *            The filename of the image.
     */
    final public void setImage(ImageIcon image)
    {
        try {
            image.getImage().getGraphics();
        }
        catch (Throwable e) {
            //we MUST be able to get the graphics!
            BufferedImage bImg = new BufferedImage(image.getIconWidth(), image.getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            image.paintIcon(comp, bImg.getGraphics(), 0, 0);
            image.setImage(bImg);

        }
        this.image = image;
    }

    /**
     * Gets a canvas to paint on. Origo of the canvas is at the center of the
     * cell that this greenfootObject is in. Remember to call update afterwards
     * for any painting to be visible.
     * 
     * @see #update()
     * @return A canvas to draw on, or null if a world is not available
     */
    public Graphics2D getCanvas()
    {
        if (world == null) {
            return null;
        }

        return world.getCanvas(getX(), getY());
    }

    /**
     * Updates the world. Should be called when changes have been made that
     * needs updating.
     *  
     */
    public void update()
    {
        if (world != null) {
            world.update();
        }
    }

    /**
     * Delays for one time-slice
     *  
     */
    public void delay()
    {
        if (world != null)
            world.delay();
    }

    /**
     * Determines whether the given location is considered to be inside this
     * object. <br>
     * The default implementation is to use the size of the image. If it does
     * not have an image, the entire cell will be considered to be inside the
     * object.
     * 
     * @param x
     *            The x-position relative to the top left corner of the cell
     *            that this object is currently in
     * @param y
     *            The y-position relative to the top left corner of the cell
     *            that this object is currently in
     * @return
     */
    public boolean contains(int x, int y)
    {
        ImageIcon image = getImage();
        if (image != null) {
            int width = image.getIconWidth();
            int height = image.getIconHeight();
            return intersects(x, y, 0, 0, width, height);
        }
        else {
            return false;
        }
    }

    /**
     * Determines if the given position intersects with the rectangle.
     * 
     * @param x
     * @param y
     * @param rectX
     * @param rectY
     * @param rectWidth
     * @param rectHeight
     * @return
     */
    private boolean intersects(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight)
    {
        if (x > rectX && x < (rectX + rectWidth) && y > rectY && y < (rectY + rectHeight)) {
            return true;
        }
        else {
            return false;
        }
    }

}