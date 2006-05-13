package greenfoot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.net.URL;

import javax.swing.ImageIcon;

import bluej.runtime.ExecServer;

/**
 * An image to be shown on screen. The image may be loaded from an image file
 * and/or drawn by using various drawing methods.
 * 
 * @author Poul Henriksen
 * @version 1.0
 * @cvs-version $Id: GreenfootImage.java 4235 2006-05-13 15:14:23Z polle $
 */
public class GreenfootImage
{
    private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 0);
    /** The image name is primarily use for debuging. */
    private String imageFileName;
    private Image image;
    private Graphics2D graphics;
    private static MediaTracker tracker;

    /**
     * Create an image from an image file. Supported file formats are JPEG, GIF
     * and PNG.
     * <p>
     * 
     * The file name may be an absolute path, a base name for a file located in
     * the project directory.
     * 
     * @param filename Typically the name of a file in the images directory in
     *            the project directory.
     * @throws IllegalArgumentException If the image can not be loaded.
     */
    public GreenfootImage(String filename)
        throws IllegalArgumentException
    {
        loadFile(filename);
    }

    /**
     * Create an empty (transparent) image with the specified size.
     * 
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     */
    public GreenfootImage(int width, int height)
    {
        setImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }

    /**
     * Create a GreenfootImage from another GreenfootImage.
     */
    public GreenfootImage(GreenfootImage image)
        throws IllegalArgumentException
    {
        this(image.getWidth(), image.getHeight());
        drawImage(image, 0, 0);
    }

    private void loadURL(URL imageURL)
        throws IllegalArgumentException
    {
        if (imageURL == null) {
            throw new NullPointerException("Image URL must not be null.");
        }
        Image newImage = new ImageIcon(imageURL).getImage();
        if (newImage.getWidth(null) == -1) {
            throw new IllegalArgumentException("Could not load image from: " + imageFileName);
        }
        setImage(newImage);
    }

    /**
     * Tries to find the filename using the classloader. It first searches in
     * 'projectdir/images/', then in the 'projectdir' and last as an absolute
     * filename or URL
     * 
     * @param filename Name of the image file
     * @throws IllegalArgumentException If it could not read the image.
     */
    private void loadFile(String filename)
        throws IllegalArgumentException
    {
        if (filename == null) {
            throw new NullPointerException("Filename must not be null.");
        }
        imageFileName = filename;

        ClassLoader currentLoader = ExecServer.getCurrentClassLoader();

        // First, try the project's images dir
        URL imageURL = currentLoader.getResource("images/" + filename);
        if (imageURL == null) {
            // Second, try the project directory
            imageURL = currentLoader.getResource(filename);
        }
        try {
            loadURL(imageURL);
        }
        catch (Throwable e) {
            // Third, try as an absolute filename or URL.
            Image newImage = new ImageIcon(filename).getImage();
            if (newImage.getWidth(null) == -1) {
                throw new IllegalArgumentException("Could not load image from: " + imageFileName);
            }
            setImage(newImage);
        }
    }

    /**
     * Sets the image to the specified AWT image
     * 
     * @param image
     */
    private void setImage(Image image)
        throws IllegalArgumentException
    {
        if (image == null) {
            throw new IllegalArgumentException("Image must not be null.");
        }
        this.image = image;
        initGraphics();
    }

    /**
     * Gets the Java AWT image that this GreenfootImage represents.
     * 
     */
    Image getAWTImage()
    {
        return image;
    }

    private void initGraphics()
    {
        try {
            graphics = (Graphics2D) image.getGraphics();
        }
        catch (Throwable e) {
            // we MUST be able to get the graphics!
            image = getBufferedImage();
            graphics = (Graphics2D) image.getGraphics();
        }
        graphics.setBackground(DEFAULT_BACKGROUND);
    }

    private Graphics2D getGraphics()
    {
        return graphics;
    }

    /**
     * Return the width of the image.
     * 
     * @return Width of the image.
     */
    public int getWidth()
    {
        return image.getWidth(null);
    }

    /**
     * Return the height of the image.
     * 
     * @return Height of the image.
     */
    public int getHeight()
    {
        return image.getHeight(null);
    }

    /**
     * Creates a new image that is a scaled version of this image.
     * 
     * @param width Width of new image
     * @param height Height of new image
     * @return A new scaled image
     */
    public void scale(int width, int height)
    {
        image = image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        waitForImageLoad();
    }

    /**
     * Mirrors the image vertically (flip around the y-axis).
     * 
     */
    public void mirrorVertically()
    {
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -image.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        setImage(op.filter(getBufferedImage(), null));
    }

    /**
     * Mirrors the image horizontally (flip around the x-axis).
     * 
     */
    public void mirrorHorizontally()
    {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-image.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        setImage(op.filter(getBufferedImage(), null));
    }

    /**
     * Fill the entire image with the current drawing dcolor.
     * 
     */
    public void fill()
    {
        Graphics g = getGraphics();
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Draws the given Image onto this image
     * 
     * @param image The image to draw onto this one.
     * @param x x-coordinate for drawing the image.
     * @param y y-coordinate for drawing the image.
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
     * Set the current drawing color. This color will be used for subsequent
     * drawing operations.
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
    public Color getColor()
    {
        return getGraphics().getColor();
    }

    /**
     * Return the color at the given pixel.
     * 
     * @throws IndexOutOfBoundsException If the pixel location is not within the
     *             image bounds.
     */
    public Color getColorAt(int x, int y)
    {
        if (x >= getWidth()) {
            throw new IndexOutOfBoundsException("X is out of bounds. It was: " + x
                    + " and it should have been smaller than: " + getWidth());
        }
        if (y >= getHeight()) {
            throw new IndexOutOfBoundsException("Y is out of bounds. It was: " + y
                    + " and it should have been smaller than: " + getHeight());
        }
        if (x < 0) {
            throw new IndexOutOfBoundsException("X is out of bounds. It was: " + x
                    + " and it should have been at least: 0");
        }
        if (y < 0) {
            throw new IndexOutOfBoundsException("Y is out of bounds. It was: " + y
                    + " and it should have been at least: 0");
        }

        int rgb = 0;
        if (image instanceof BufferedImage) {
            rgb = ((BufferedImage) image).getRGB(x, y);
        }
        else if (image instanceof VolatileImage) {
            rgb = ((VolatileImage) image).getSnapshot().getRGB(x, y);
        }
        else {
            throw new IllegalStateException("The type of image was neither BufferedImage or VolatileImage. It was. "
                    + image.getClass());
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
     * @param x the <i>x </i> coordinate of the rectangle to be filled.
     * @param y the <i>y </i> coordinate of the rectangle to be filled.
     * @param width the width of the rectangle to be filled.
     * @param height the height of the rectangle to be filled.
     */
    public void fillRect(int x, int y, int width, int height)
    {
        getGraphics().fillRect(x, y, width, height);
    }

    /**
     * Clears the image.
     * 
     */
    public void clear()
    {
        getGraphics().clearRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Draw the outline of the specified rectangle. The left and right edges of
     * the rectangle are at <code>x</code> and
     * <code>x&nbsp;+&nbsp;width</code>. The top and bottom edges are at
     * <code>y</code> and <code>y&nbsp;+&nbsp;height</code>. The rectangle
     * is drawn using the current color.
     * 
     * @param x the <i>x </i> coordinate of the rectangle to be drawn.
     * @param y the <i>y </i> coordinate of the rectangle to be drawn.
     * @param width the width of the rectangle to be drawn.
     * @param height the height of the rectangle to be drawn.
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
     * @param string the string to be drawn.
     * @param x the <i>x </i> coordinate.
     * @param y the <i>y </i> coordinate.
     */
    public void drawString(String string, int x, int y)
    {
        getGraphics().drawString(string, x, y);
    }

    /**
     * Fill an oval bounded by the specified rectangle with the current drawing
     * color.
     * 
     * @param x the <i>x </i> coordinate of the upper left corner of the oval to
     *            be filled.
     * @param y the <i>y </i> coordinate of the upper left corner of the oval to
     *            be filled.
     * @param width the width of the oval to be filled.
     * @param height the height of the oval to be filled.
     */
    public void fillOval(int x, int y, int width, int height)
    {
        getGraphics().fillOval(x, y, width, height);
    }

    /**
     * Draw an oval bounded by the specified rectangle with the current drawing
     * color.
     * 
     * @param x the <i>x </i> coordinate of the upper left corner of the oval to
     *            be filled.
     * @param y the <i>y </i> coordinate of the upper left corner of the oval to
     *            be filled.
     * @param width the width of the oval to be filled.
     * @param height the height of the oval to be filled.
     */
    public void drawOval(int x, int y, int width, int height)
    {
        getGraphics().drawOval(x, y, width, height);
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
     * @param xpoints a an array of <code>x</code> coordinates.
     * @param ypoints a an array of <code>y</code> coordinates.
     * @param nPoints a the total number of points.
     */
    public void fillPolygon(int[] xpoints, int[] ypoints, int nPoints)
    {
        getGraphics().fillPolygon(xpoints, ypoints, nPoints);
    }

    /**
     * Draws a closed polygon defined by arrays of <i>x</i> and <i>y</i>
     * coordinates. Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates
     * defines a point.
     * <p>
     * This method draws the polygon defined by <code>nPoint</code> line
     * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code> line
     * segments are line segments from
     * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code>
     * to <code>(xPoints[i],&nbsp;yPoints[i])</code>, for 1&nbsp;&le;&nbsp;<i>i</i>&nbsp;&le;&nbsp;<code>nPoints</code>.
     * The figure is automatically closed by drawing a line connecting the final
     * point to the first point, if those points are different.
     * 
     * @param xPoints a an array of <code>x</code> coordinates.
     * @param yPoints a an array of <code>y</code> coordinates.
     * @param nPoints a the total number of points.
     */
    public void drawPolygon(int[] xpoints, int[] ypoints, int nPoints)
    {
        getGraphics().drawPolygon(xpoints, ypoints, nPoints);
    }

    /**
     * Draw a line, using the current drawing color, between the points
     * <code>(x1,&nbsp;y1)</code> and <code>(x2,&nbsp;y2)</code>.
     * 
     * @param x1 the first point's <i>x </i> coordinate.
     * @param y1 the first point's <i>y </i> coordinate.
     * @param x2 the second point's <i>x </i> coordinate.
     * @param y2 the second point's <i>y </i> coordinate.
     */
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        getGraphics().drawLine(x1, y1, x2, y2);
    }

    /**
     * Return a text representation of the image.
     */
    public String toString()
    {
        String superString = super.toString();
        if (imageFileName == null) {
            return superString;
        }
        else {
            return "Image file name: " + imageFileName + " " + superString;
        }
    }
    
    /**
     * Gets a BufferedImage of the AWT Image that this GreenfootImage
     * represents. We need this for some of the image manipulation methods.
     * 
     * 
     */
    private BufferedImage getBufferedImage()
    {
        if (image instanceof BufferedImage) {}
        else if (image instanceof VolatileImage) {
            image = ((VolatileImage) image).getSnapshot();
        }
        else {
            BufferedImage bImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bImage.getGraphics();
            g.drawImage(image, 0, 0, null);
            image = bImage;
            waitForImageLoad();
        }
        return (BufferedImage) image;
    }
    
    /**
     * Wait until the iamge is fully loaded and then init the graphics.
     * 
     */
    private void waitForImageLoad()
    {
        if (tracker == null) {
            tracker = new MediaTracker(new Component() {});
        }
        tracker.addImage(image, 0);
        try {
            tracker.waitForID(0);
            tracker.removeImage(image);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        initGraphics();
    }
}