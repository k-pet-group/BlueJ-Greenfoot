package greenfoot;

import greenfoot.util.GreenfootUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import bluej.runtime.ExecServer;

/**
 * An image to be shown on screen. The image may be loaded from an image file
 * and/or drawn by using various drawing methods.
 * 
 * @author Poul Henriksen
 * @version 0.6 dev
 * @cvs-version $Id: GreenfootImage.java 4065 2006-05-02 10:56:34Z mik $
 */
public class GreenfootImage
{
    /** The image name is primarily use for debuging. */
    private String imageFileName; 
    
    
    private java.awt.Image image; 
    private Graphics2D graphics;

    /**
     * Create an image from an image file. Supported file formats are JPEG, GIF and PNG.<p>
     * 
     * The file name may be an absolute path, a base name for a file located in the
     * project directory.
     * 
     * @param filename The name of the file to be loaded.
     * @throws FileNotFoundException
     */
    public GreenfootImage(String filename)
    {
        loadFile(filename);
    }

    private void loadFile(String filename)
    {
        if(filename == null) {
            throw new NullPointerException("Filename must not be null.");
        }
        imageFileName = filename;
        URL imageURL;
        try {
            imageURL = new URL(filename);
            setImage(new ImageIcon(imageURL).getImage());
            return;
        }
        catch (MalformedURLException e) {
            URL url;
            ClassLoader currentLoader = ExecServer.getCurrentClassLoader();
            if (currentLoader != null) {    
                url = currentLoader.getResource(filename);
            }
            else {
                url = ClassLoader.getSystemResource(filename);
            }
                    
            if (url != null) {
                setImage(new ImageIcon(url).getImage());
            }
            else if (image == null) {
                setImage(new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR));
            }
        }
    }

    /**
     * Create an image from an URL.
     * 
     * @param imageURL The URL of the image file.
     */
    public GreenfootImage(URL imageURL)
    {
        if(imageURL == null) {
            throw new NullPointerException("Image URL must not be null.");
        }
        imageFileName = imageURL.getFile();
        image = new ImageIcon(imageURL).getImage();
        initGraphics();
    }

    /**
     * Create an empty (white) image with a specified size.
     * 
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     */
    public GreenfootImage(int width, int height)
    {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        initGraphics();
    }

    /**
     * Create a GreenfootImage from specified AWT image.
     */
    public GreenfootImage(java.awt.Image image)
    {
        setImage(image);
        initGraphics();
    }

    /**
     * Sets the image to the specified AWT image
     * 
     * @param image
     */
    private void setImage(java.awt.Image image)
    {
        this.image = image;
        initGraphics();
    }
    
    /**
     * Gets the Java AWT image that this GreenfootImage represents.
     * 
     * @return
     */
    public Image getAWTImage() {
        return image;
    }
    
    private void initGraphics() {
        try {
            if(image==null && imageFileName!=null) {
                loadFile(imageFileName);
            }
            MediaTracker tracker = new MediaTracker(new Container());
            tracker.addImage(image, 0);
            try {
                tracker.waitForAll();
            }
            catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            graphics = (Graphics2D) image.getGraphics();
        }
        catch (Throwable e) {
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            
            if (width == -1 || height == -1) {
                // Failed to load for some reason
                width = 1;
                height = 1;
            }
            
            //we MUST be able to get the graphics!
            BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            graphics = (Graphics2D) bImage.getGraphics();
            graphics.drawImage(image, 0, 0, null);
            image = bImage;
        }
    }
    
    private Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Return the width of the image.
     * 
     * @return Width of the image, or -1 if the width can't be determined
     */
    public int getWidth()
    {
        if (image != null) {
            return image.getWidth(null);
        }
        else {
            return -1;
        }
    }

    /**
     * Return the height of the image.
     * 
     * @return Height of the image, or -1 if the height can't be determined
     */
    public int getHeight()
    {
        if (image != null) {
            return image.getHeight(null);
        }
        else {
            return -1;
        }
    }
    
    /**
     * Creates a new image that is a scaled version of this image.
     * 
     * @param width Width of new image
     * @param height Height of new image
     * @return A new scaled image
     */
    public GreenfootImage scaleTo(int width, int height) {
        return new GreenfootImage(GreenfootUtil.getScaledImage(getAWTImage(), width, height));
    }
    

    /**
     * Fill the entire image with the given color.
     * 
     * @param color
     */
    public void fill(Color color)
    {
        Graphics g = getGraphics();
        Color oldColor = g.getColor();
        g.setColor(color);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(oldColor);
    }

    /**
     * Draws the given Image onto this image
     * 
     * @param image The image to draw onto this one.
     * @param x  x-coordinate for drawing the image.
     * @param y  y-coordinate for drawing the image.
     */
    public void drawImage(GreenfootImage image, int x, int y)
    {
        Graphics thisGraphics = getGraphics();
        image.drawImage(thisGraphics, x, y, null);
    }

    /**
     * Draws this image onto the given Graphics object.
     *  
     */
    void drawImage(Graphics g, int x, int y, ImageObserver observer)
    {
        g.drawImage(image, x, y, observer);
    }
    
    /**
     * Set a color to be used for subsequent drawing operations.
     * 
     * @param color The color to be used.
     */
    public void setColor(Color color)
    {
        getGraphics().setColor(color);
    }

    /**
     * Return the current drawing color.
     * 
     * @return The current color.
     */
    public Color getColor() {
        return getGraphics().getColor();
    }
    
    /**
     * Return the color at the given pixel.
     * 
     * @throws IndexOutOfBoundsException If the pixel location is not within the image bounds.
     */
    public Color getColorAt(int x, int y) {
        if(x >= getWidth()) {
            throw new IndexOutOfBoundsException("X is out of bounds. It was: " + x + " and it should have been smaller than: " + getWidth());
        }
        if(y >= getHeight()) {
            throw new IndexOutOfBoundsException("Y is out of bounds. It was: " + y + " and it should have been smaller than: " + getHeight());
        }
        if(x < 0) {
            throw new IndexOutOfBoundsException("X is out of bounds. It was: " + x + " and it should have been at least: 0");
        }
        if(y < 0) {
            throw new IndexOutOfBoundsException("Y is out of bounds. It was: " + y + " and it should have been at least: 0");
        }
        
        int rgb = 0;
        if(image instanceof BufferedImage) {
            rgb = ((BufferedImage) image).getRGB(x, y);
        }
        else if(image instanceof VolatileImage) {
            rgb = ((VolatileImage) image).getSnapshot().getRGB(x, y);
        } else {
            throw new IllegalStateException("The type of image was neither BufferedImage or VolatileImage. It was. " + image.getClass());
        }
        return new Color(rgb);
    }

    /**
     * Fill the specified rectangle. The left and right edges of the rectangle
     * are at <code>x</code> and
     * <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>. The top and bottom
     * edges are at <code>y</code> and
     * <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>. The resulting
     * rectangle covers an area <code>width</code> pixels wide by
     * <code>height</code> pixels tall. The rectangle is filled using the
     * current color.
     * 
     * @param x
     *            the <i>x </i> coordinate of the rectangle to be filled.
     * @param y
     *            the <i>y </i> coordinate of the rectangle to be filled.
     * @param width
     *            the width of the rectangle to be filled.
     * @param height
     *            the height of the rectangle to be filled.
     */
    public void fillRect(int x, int y, int width, int height)
    {
        getGraphics().fillRect(x, y, width, height);
    }

    /**
     * Draw the outline of the specified rectangle. The left and right edges of
     * the rectangle are at <code>x</code> and
     * <code>x&nbsp;+&nbsp;width</code>. The top and bottom edges are at
     * <code>y</code> and <code>y&nbsp;+&nbsp;height</code>. The rectangle
     * is drawn using the current color.
     * 
     * @param x
     *            the <i>x </i> coordinate of the rectangle to be drawn.
     * @param y
     *            the <i>y </i> coordinate of the rectangle to be drawn.
     * @param width
     *            the width of the rectangle to be drawn.
     * @param height
     *            the height of the rectangle to be drawn.
     */
    public void drawRect(int x, int y, int width, int height)
    {
        getGraphics().drawRect(x, y, width, height);
    }

    /**
     * Draw the text given by the specified string, using the current font and
     * color. The baseline of the leftmost character is at position ( <i>x
     * </i>,&nbsp; <i>y </i>).
     * 
     * @param string
     *            the string to be drawn.
     * @param x
     *            the <i>x </i> coordinate.
     * @param y
     *            the <i>y </i> coordinate.
     */
    public void drawString(String string, int x, int y)
    {
        getGraphics().drawString(string, x, y);
    }

    /**
     * Fill an oval bounded by the specified rectangle with the current color.
     * 
     * @param x
     *            the <i>x </i> coordinate of the upper left corner of the oval
     *            to be filled.
     * @param y
     *            the <i>y </i> coordinate of the upper left corner of the oval
     *            to be filled.
     * @param width
     *            the width of the oval to be filled.
     * @param height
     *            the height of the oval to be filled.
     */
    public void fillOval(int x, int y, int width, int height)
    {
        getGraphics().fillOval(x, y, width, height);
    }

    /**
     * Fill a closed polygon defined by arrays of <i>x </i> and <i>y </i>
     * coordinates.
     * <p>
     * This method draws the polygon defined by <code>nPoint</code> line
     * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code> line
     * segments are line segments from
     * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code>
     * to <code>(xPoints[i],&nbsp;yPoints[i])</code>, for 1&nbsp;&le;&nbsp;
     * <i>i </i>&nbsp;&le;&nbsp; <code>nPoints</code>. The figure is
     * automatically closed by drawing a line connecting the final point to the
     * first point, if those points are different.
     * <p>
     * The area inside the polygon is defined using an even-odd fill rule, also
     * known as the alternating rule.
     * 
     * @param xpoints
     *            a an array of <code>x</code> coordinates.
     * @param ypoints
     *            a an array of <code>y</code> coordinates.
     * @param nPoints
     *            a the total number of points.
     */
    public void fillPolygon(int[] xpoints, int[] ypoints, int nPoints)
    {
        getGraphics().fillPolygon(xpoints, ypoints, nPoints);
    }

    /**
     * Draw a line, using the current color, between the points
     * <code>(x1,&nbsp;y1)</code> and <code>(x2,&nbsp;y2)</code>.
     * 
     * @param x1
     *            the first point's <i>x </i> coordinate.
     * @param y1
     *            the first point's <i>y </i> coordinate.
     * @param x2
     *            the second point's <i>x </i> coordinate.
     * @param y2
     *            the second point's <i>y </i> coordinate.
     */
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        getGraphics().drawLine(x1, y1, x2, y2);
    }
    
    /**
     * Return a text representation of the image.
     */
    public String toString() {        
        String superString = super.toString();
        if(imageFileName == null) {
            return superString;
        } else {
            return "Image file name: " + imageFileName + " " + superString;
        }
    }
    
    /**
     * Make a copy of this image. Drawing in the copy will not affect the original.
     */
    GreenfootImage copy()
    {
        int width = getWidth();
        int height = getHeight();
        
        if (width == -1 || height == -1) {
            return new GreenfootImage(1, 1);
        }
        
        GreenfootImage dest = new GreenfootImage(width, height);
        dest.drawImage(this, 0, 0);
        return dest;
    }
}