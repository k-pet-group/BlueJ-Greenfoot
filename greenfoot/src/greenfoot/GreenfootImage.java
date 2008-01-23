package greenfoot;

import greenfoot.util.GraphicsUtilities;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.net.URL;

/**
 * An image to be shown on screen. The image may be loaded from an image file
 * and/or drawn by using various drawing methods.
 * 
 * @author Poul Henriksen
 * @version 1.4.0
 * @cvs-version $Id: GreenfootImage.java 5488 2008-01-23 16:53:29Z polle $
 */
public class GreenfootImage
{
    private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 0);
    /** The image name is primarily use for debuging. */
    private String imageFileName;
    private BufferedImage image;
    private Graphics2D graphics;
    private static MediaTracker tracker;
    
    /**
     * Copy on write is used for performance reasons. If an image is
     * copyOnWrite, it means that the actual image data might be shared between
     * several GreenfootImage instances. As soon as a copy-on-write GreenfootImage is
     * modified, it is necessary to create a copy of the image, in order not to
     * change the image for the rest of the GreenfootImages sharing this image.
     * This flag is used to keep track of whether it is a shared image that
     * needs to be copied upon write (changes) to the image.
     */
    private boolean copyOnWrite = false;

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
        setImage(GraphicsUtilities.createCompatibleTranslucentImage(width, height));
    }

    /**
     * Create a GreenfootImage from another GreenfootImage.
     */
    public GreenfootImage(GreenfootImage image)
        throws IllegalArgumentException
    {
        if (! image.copyOnWrite) {
            setImage(GraphicsUtilities.createCompatibleTranslucentImage(image.getWidth(), image.getHeight()));
            drawImage(image, 0, 0);
        }
        else {
            // If the source image is a copy-on-write image, we can easily
            // make this a copy-on-write image as well.
            this.image = image.image;
            copyOnWrite = true;
        }
    }
    
    /**
     * Create a copy-on-write image based on the given BufferedImage.
     * If the GreenfootImage is modified, the original BufferedImage will
     * not be affected.
     */
    GreenfootImage(BufferedImage image)
    {
        this.image = image;
        copyOnWrite = true;
    }

    private void loadURL(URL imageURL)
        throws IllegalArgumentException
    {
        if (imageURL == null) {
            throw new NullPointerException("Image URL must not be null.");
        }
        try {
            image = GraphicsUtilities.loadCompatibleImage(imageURL);
            copyOnWrite = false;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not load image from: " + imageFileName);
        }
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
        URL url = GreenfootUtil.getURL(filename, "images");

        loadURL(url);
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
        this.image = getBufferedImage(image);
        copyOnWrite = false;
    }


    /**
     * Returns the java.awt.image.BufferedImage that backs this GreenfootImage. Any changes to
     * the returned image will be reflected in the GreenfootImage.
     * 
     * @return The java.awt.image.BufferedImage backing this GreenfootImage
     * @since Greenfoot version 1.0.2
     */
    public BufferedImage getAwtImage()
    {
        ensureWritableImage();
        return image;
    }

    private Graphics2D getGraphics()
    {
        if (graphics == null) {
            if (copyOnWrite) {
                // this also sets graphics
                ensureWritableImage();
            }
            else {
                graphics = (Graphics2D) image.createGraphics();
                graphics.setBackground(DEFAULT_BACKGROUND);
            }
        }
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
     * Rotates this image around the center.
     * 
     * @param degrees
     */
    public void rotate(int degrees)
    {
        AffineTransform tx = AffineTransform.getRotateInstance(Math.toRadians(degrees), getWidth()/2., getHeight()/2.);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        BufferedImage newImage = GraphicsUtilities.createCompatibleTranslucentImage(getWidth(), getHeight());
        setImage(op.filter(image, newImage));
    }

    /**
     * Scales this image to a new size.
     * 
     * @param width Width of new image
     * @param height Height of new image
     */
    public void scale(int width, int height)
    {
        setImage(image.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING));
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
        setImage(op.filter(image, null));
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
        setImage(op.filter(image, null));
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
     * Set the current font. This font will be used for subsequent text
     * operations.
     */
    public void setFont(Font f)
    {
        getGraphics().setFont(f);
    }
    
    /**
     * Get the current font.
     */
    public Font getFont()
    {
        return getGraphics().getFont();
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
        return new Color(getRGBAt(x, y), true); 
    }
    
    /**
     * Sets the color at the given pixel to the given color.
     */
    public void setColorAt(int x, int y, Color color) {
        setRGBAt(x, y, color.getRGB());
    }
    
    private int getRGBAt(int x, int y)
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

        return image.getRGB(x,y);
    }
    
    private void setRGBAt(int x, int y, int rgb)
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

        ensureWritableImage();
        image.setRGB(x,y,rgb);
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
     * @param xPoints a an array of <code>x</code> coordinates.
     * @param yPoints a an array of <code>y</code> coordinates.
     * @param nPoints a the total number of points.
     */
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {
        getGraphics().fillPolygon(xPoints, yPoints, nPoints);
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
     * @param xPoints an array of <code>x</code> coordinates.
     * @param yPoints an array of <code>y</code> coordinates.
     * @param nPoints the total number of points.
     */
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {
        getGraphics().drawPolygon(xPoints, yPoints, nPoints);
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
    
    static boolean equal(GreenfootImage image1, GreenfootImage image2)
    {
        if (image1 == null || image2 == null) {
            return image1 == image2;
        }
        else {
            return (image1.image == image2.image || image1.equals(image2));
        }
    }

    /**
     * Ensure we have an image which we are allowed to write to. If we are
     * a copy-on-write image, create a copy of the image (and set up the
     * graphics2d object) before returning.
     */
    private void ensureWritableImage()
    {
        if (copyOnWrite) {
            BufferedImage bImage = GraphicsUtilities.createCompatibleTranslucentImage(image.getWidth(null), image.getHeight(null));
            graphics = bImage.createGraphics();
            graphics.setBackground(DEFAULT_BACKGROUND);
            graphics.drawImage(image, 0, 0, null);
            image = bImage;
            copyOnWrite = false;
        }
    }
    
    /**
     * Gets a BufferedImage of the AWT Image that this GreenfootImage
     * represents. We need this for some of the image manipulation methods.
     */
    private static BufferedImage getBufferedImage(Image image)
    {
        if (image instanceof BufferedImage) {}
        else if (image instanceof VolatileImage) {
            image = ((VolatileImage) image).getSnapshot();
            waitForImageLoad(image);
        }
        else {
            waitForImageLoad(image);
            BufferedImage bImage = GraphicsUtilities.createCompatibleTranslucentImage(image.getWidth(null), image.getHeight(null));
            Graphics g = bImage.getGraphics();
            g.drawImage(image, 0, 0, null);
            image = bImage;
        }
        return (BufferedImage) image;
    }
        
    /**
     * Wait until the iamge is fully loaded and then init the graphics.
     * 
     */
    private static void waitForImageLoad(Image image)
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
    }
}