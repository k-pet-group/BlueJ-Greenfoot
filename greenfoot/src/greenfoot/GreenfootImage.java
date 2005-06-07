package greenfoot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * An image that can be loaded from a file and/or drawn by using the draw
 * methods.
 * 
 * Do we want to make the AWT image available? It might be useful for some
 * scenraio creators that wnats to do more advanced stuff.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootImage.java 3417 2005-06-07 11:02:00Z polle $
 */
public class GreenfootImage
{
    /** The image name is primarily use for debuging. */
    private String imageFileName; 
    
    private java.awt.Image image; 
    private Graphics2D graphics;
    private boolean tiled;

    /**
     * Loads an image from a file.<br>
     * 
     * It first tries to use the filename as an absolute path, and if that fails
     * it looks for the filename in the projects directory.
     * 
     * @param filename
     * @throws FileNotFoundException
     */
    public GreenfootImage(String filename)
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
            Greenfoot greenfoot = Greenfoot.getInstance();
            if (greenfoot == null) {
                // If there is no greenfoot instance, we should just return
                // without trying to load the image. This should never happen
                // when running greenfoot normally, but will be the case when
                // doing some of unit tests.
                return;
            }
            try {
                URI dir = greenfoot.getPackage().getDir().toURI();
                URL url = null;
                try {
                    url = dir.resolve(filename).toURL();
                    setImage(new ImageIcon(url).getImage());
                }
                catch (MalformedURLException e2) {
                    System.err.println("Could not load the URL (file): " + filename);
                    e2.printStackTrace();
                }
            }
            catch (ProjectNotOpenException e1) {
                e1.printStackTrace();
            }
            catch (PackageNotFoundException e1) {
                e1.printStackTrace();
            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }

        }

    }

    /**
     * Loads an image from an URL.
     * 
     * @param imageURL
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

    public GreenfootImage(int width, int height)
    {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        initGraphics();
    }

    /**
     * Constructs a new Image from specified AWT image
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
    
    private void initGraphics() {
        try {
            graphics = (Graphics2D) image.getGraphics();
        }
        catch (Throwable e) {
            //we MUST be able to get the graphics!
            BufferedImage bImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            graphics = (Graphics2D) bImage.getGraphics();
            graphics.drawImage(image, 0, 0, null);
            image = bImage;
        }
    }
    
    private Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Gets the width of the image.
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
     * Gets the height of the image.
     * 
     * @return Height of the image, or -1 if the width can't be determined
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
     * Fills the entire image with the given color.
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
     * @param image
     * @param x
     * @param y
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
     * Sets the tiled property of an image.
     * 
     * @param tiled
     *            Whether it should tile the image or not.
     */
    public void setTiled(boolean tiled)
    {
        this.tiled = tiled;
    }

    /**
     * Returns true if image is tiled
     * 
     * @return Wherher the image is tilled.
     */
    public boolean isTiled()
    {
        return tiled;
    }

    public void setColor(Color color)
    {
        getGraphics().setColor(color);
    }
    
    public Color getColor() {
        return getGraphics().getColor();
    }

    /**
     * Fills the specified rectangle. The left and right edges of the rectangle
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
     * Draws the outline of the specified rectangle. The left and right edges of
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
     * Draws the text given by the specified string, using the current font and
     * color. The baseline of the leftmost character is at position ( <i>x
     * </i>,&nbsp; <i>y </i>).
     * 
     * @param str
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
     * Fills an oval bounded by the specified rectangle with the current color.
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
     * Fills a closed polygon defined by arrays of <i>x </i> and <i>y </i>
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
     * @param xPoints
     *            a an array of <code>x</code> coordinates.
     * @param yPoints
     *            a an array of <code>y</code> coordinates.
     * @param nPoints
     *            a the total number of points.
     */
    public void fillPolygon(int[] xpoints, int[] ypoints, int nPoints)
    {
        getGraphics().fillPolygon(xpoints, ypoints, nPoints);
    }

    /**
     * Draws a line, using the current color, between the points
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
    
    public String toString() {        
        String superString = super.toString();
        if(imageFileName == null) {
            return superString;
        } else {
            return "Image file name: " + imageFileName + " " + superString;
        }
    }
}