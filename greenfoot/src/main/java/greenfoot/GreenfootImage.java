/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2014,2015,2016,2017,2019  Poul Henriksen and Michael Kolling
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot;

import greenfoot.util.GraphicsUtilities;
import greenfoot.util.GreenfootUtil;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;


/**
 * An image to be shown on screen. The image may be loaded from an image file
 * and/or drawn by using various drawing methods.
 * 
 * @author Poul Henriksen
 * @version 2.6
 */
public class GreenfootImage
{
    private static final greenfoot.Color DEFAULT_BACKGROUND = new greenfoot.Color(255,255,255,0);
    private static final greenfoot.Color DEFAULT_FOREGROUND = greenfoot.Color.BLACK;
    
    /** The image name and url are primarily used for debugging. */
    private String imageFileName;
    private URL imageUrl;
    
    private BufferedImage image;
    private static MediaTracker tracker;
    
    private greenfoot.Color currentColor = DEFAULT_FOREGROUND;
    private greenfoot.Font currentFont;
    
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
     * Value from 0 to 255, with 0 being completely transparent and 255 being opaque.
     */
    private int transparency = 255;

    /**
     * Create an image from an image file. Supported file formats are JPEG, GIF
     * and PNG.
     * 
     * <p>The file name may be an absolute path, or a base name for a file located in
     * the project directory.
     * 
     * @param filename Typically the name of a file in the images directory within
     *            the project directory.
     * @throws IllegalArgumentException If the image can not be loaded.
     */
    public GreenfootImage(String filename)
        throws IllegalArgumentException
    {
        GreenfootImage gImage = GreenfootUtil.getCachedImage(filename);
        if (gImage != null)
        {
            createClone(gImage);
        }
        else 
        {
            try{
                loadFile(filename);
            }
            catch(IllegalArgumentException ile){
                GreenfootUtil.addCachedImage(filename, null);
                throw ile;
            }
        }
        //if the image was successfully cached, ensure that the image is copyOnWrite
        boolean success = GreenfootUtil.addCachedImage(filename, new GreenfootImage(this));
        if (success){
            copyOnWrite = true;
        }
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
     * 
     * @param image The source image to be copied.
     */
    public GreenfootImage(GreenfootImage image)
        throws IllegalArgumentException
    {
        if (! image.copyOnWrite) {
            setImage(GraphicsUtilities.createCompatibleTranslucentImage(image.getWidth(), image.getHeight()));
            Graphics2D g = getGraphics();
            g.setComposite(AlphaComposite.Src);
            g.drawImage(image.getAwtImage(), 0, 0, null);
            g.dispose();
        }
        else {
            // If the source image is a copy-on-write image, we can easily
            // make this a copy-on-write image as well.
            this.image = image.image;
            copyOnWrite = true;
        }
        copyStates(image, this);
    }
    
    
    /**
     * Creates an image with the given string drawn as text using the given font size, with the given foreground
     * Color on the given background Color.  If the string has newline characters, it is split into multiple
     * lines which are drawn horizontally-centred.
     * 
     * @param string the string to be drawn
     * @param size the requested height in pixels of each line of text (the actual height may be different by a pixel or so)
     * @param foreground the color of the text.  Since Greenfoot 3.0.4, passing null will use black.
     * @param background the color of the image behind the text.  Since Greenfoot 3.0.4, passing null with leave the background transparent.
     * @since 2.0.1
     */
    public GreenfootImage(String string, int size, greenfoot.Color foreground, greenfoot.Color background)
    {
        this(string, size, foreground == null ? null : foreground.getColorObject(), background == null ? null : background.getColorObject(), null);
    }
    
    /**
     * Creates an image with the given string drawn as text using the given font size, with the given foreground
     * Color on the given background Color.  If the string has newline characters, it is split into multiple
     * lines which are drawn horizontally-centred.
     * 
     * @param string the string to be drawn
     * @param size the requested height in pixels of each line of text (the actual height may be different by a pixel or so)
     * @param foreground the color of the text.  Since Greenfoot 2.2.0, passing null will use black.
     * @param background the color of the image behind the text.  Since Greenfoot 2.2.0, passing null with leave the background transparent.
     * @param outline the colour of the outline that will be drawn around the text.  Passing null will draw no outline.
     * @since 2.4.0
     */
    GreenfootImage(String string, int size, Color foreground, Color background, Color outline)
    {
        String[] lines = GraphicsUtilities.splitLines(string);
        GraphicsUtilities.MultiLineStringDimensions d = GraphicsUtilities.getMultiLineStringDimensions(lines, Font.BOLD, size);
        image = GraphicsUtilities.createCompatibleTranslucentImage(d.getWidth(), d.getHeight());
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.setColor(background == null ? new Color(0, 0, 0, 0) : background);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        GraphicsUtilities.drawOutlinedText(g, d, foreground, outline);
        g.dispose();
    }
    
    /**
     * Creates an image with the given string drawn as text using the given font size, with the given foreground
     * color on the given background color.  If the string has newline characters, it
     * is split into multiple lines which are drawn horizontally-centred.
     * 
     * @param string the string to be drawn
     * @param size the requested height in pixels of each line of text (the actual height may be different by a pixel or so)
     * @param foreground the color of the text.  Since Greenfoot 3.0.4, passing null will use black.
     * @param background the color of the image behind the text.  Since Greenfoot 3.0.4, passing null with leave the background transparent.
     * @param outline the Color of the outline that will be drawn around the text.  Passing null will draw no outline.
     * @since 3.0.4
     */
    public GreenfootImage(String string, int size, greenfoot.Color foreground, greenfoot.Color background, greenfoot.Color outline)
    {
        this(string, size, foreground == null ? null : foreground.getColorObject(),
                background == null ? null : background.getColorObject(), outline == null ? null : outline.getColorObject());
    }
    
    //Package-visible:
    GreenfootImage(byte[] imageData)
    {
        try {
            image = GraphicsUtilities.loadCompatibleTranslucentImage(imageData);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not load image" + (imageFileName != null ? (" from: " + imageFileName) : ""));
        }
    }  

    private GreenfootImage() { }
    
    /**
     * Create a copy-on-write image based on this image. If the new image is
     * modified, the original image will not be affected.
     * <p>
     * Only use this method if you are sure that the original image will never
     * be modified.
     */
    GreenfootImage getCopyOnWriteClone()
    {
        GreenfootImage clone = new GreenfootImage();
        clone.copyOnWrite = true;
        clone.image = image;
        copyStates(this, clone);
        
        return clone;
    }
    
    /**
     * Creates a copy of the cached image
     * @param cachedImage image to copy
     */
    void createClone(GreenfootImage cachedImage)
    {
        this.copyOnWrite = true;
        this.image = cachedImage.image;
        copyStates(cachedImage, this);
    }
    
    /**
     * Copies the states from the src image to dst image.
     */
    private static void copyStates(GreenfootImage src, GreenfootImage dst)
    {
        dst.imageFileName = src.imageFileName;
        dst.imageUrl = src.imageUrl;
        dst.currentColor = src.currentColor;
        dst.currentFont = src.currentFont;
        dst.transparency = src.transparency;
    }    
    
    private void loadURL(URL imageURL)
        throws IllegalArgumentException
    {
        if (imageURL == null) {
            throw new NullPointerException("Image URL must not be null.");
        }
        try {
            image = GraphicsUtilities.loadCompatibleTranslucentImage(imageURL);
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
        try {
            imageUrl = GreenfootUtil.getURL(filename, "images");
        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);           
        }
        loadURL(imageUrl);
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
    
    /**
     * Remember to call dispose() when no longer using the graphics object.
     */
    private Graphics2D getGraphics()
    {
        if (copyOnWrite) {
        ensureWritableImage();
        }
        Graphics2D graphics = image.createGraphics();
        initGraphics(graphics);
        return graphics;
    }

    /**
     * Initialises the graphics. Should be called whenever we have created a
     * graphics for this image.
     */
    private void initGraphics(Graphics2D graphics)
    {
        if(graphics != null) {
            graphics.setBackground(DEFAULT_BACKGROUND.getColorObject());
            graphics.setColor(currentColor.getColorObject());
            if(currentFont != null) {
                graphics.setFont(currentFont.getFontObject());
            }
        }
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
     * @param degrees The number of degrees the object will rotate for.
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
        if (width == image.getWidth() && height == image.getHeight())
            return;
        
        // getScaledInstance is too slow, see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6196792
        // This is adapted from: http://java.sun.com/products/java-media/2D/reference/faqs/index.html#Q_How_do_I_create_a_resized_copy
        BufferedImage scaled = GraphicsUtilities.createCompatibleTranslucentImage(width, height);
        Graphics2D g = scaled.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        setImage(scaled);
    }

    /**
     * Mirrors the image vertically (the top of the image becomes the bottom, and vice versa).
     */
    public void mirrorVertically()
    {
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -image.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        setImage(op.filter(image, null));
    }

    /**
     * Mirrors the image horizontally (the left of the image becomes the right, and vice versa).
     */
    public void mirrorHorizontally()
    {
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-image.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        setImage(op.filter(image, null));
    }

    /**
     * Fill the entire image with the current drawing color.
     */
    public void fill()
    {
        Graphics g = getGraphics();
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
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
        Graphics2D g = getGraphics();
        image.drawImage(g, x, y, null, true);
        g.dispose();
    }

    /**
     * Draws this image onto the given Graphics object.
     * 
     * @param useTransparency Whether the transparency value should be used when
     *            drawing the image.
     */
    void drawImage(Graphics2D g, int x, int y, ImageObserver observer, boolean useTransparency)
    {
        Composite oldComposite = null;
        if(useTransparency) {
            float opacity = getTransparency() / 255f;
            if(opacity < 1) {
                // Don't bother with the composite if completely opaque.
                if(opacity < 0) opacity = 0;
                oldComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            }
        }
        
        g.drawImage(image, x, y, observer);

        if(oldComposite != null) {
            g.setComposite(oldComposite);
        }
    }
    
    /**
     * Set the current font. This font will be used for subsequent text operations.
     * 
     * @param f The new Font to be used.
     */
    public void setFont(greenfoot.Font f)
    {
        currentFont = f;
    }
    
    /**
     * Get the current font.
     * 
     * @return The current used font, if none, set it as the Graphics font, then return it.
     */
    public greenfoot.Font getFont()
    {
        if (currentFont == null) {
            currentFont = new greenfoot.Font(getGraphics().getFont());
        }
        return currentFont;
    }

    /**
     * Set the current drawing color. This color will be used for subsequent
     * drawing operations.
     * 
     * @param color The color to be used.
     */
    public void setColor(greenfoot.Color color)
    {
        if (color == null)
            throw new NullPointerException("Cannot set color of GreenfootImage to null");
        currentColor = color;
    }

    /**
     * Return the current drawing color.
     * 
     * @return The current color.
     */
    public greenfoot.Color getColor()
    {
        return currentColor;
    }

    /**
     * Return the color at the given pixel.
     * 
     * @throws IndexOutOfBoundsException If the pixel location is not within the
     *             image bounds.
     * @param x The horizontal coordinate of the pixel.
     * @param y The vertical coordinate of the pixel.
     * @return The Color at the specific pixel.
     */
    public greenfoot.Color getColorAt(int x, int y)
    {
        return new greenfoot.Color(new Color(getRGBAt(x, y), true));
    }
    
    /**
     * Sets the given pixel to the given color.
     * 
     * @param x The horizontal coordinate of the pixel.
     * @param y The vertical coordinate of the pixel.
     * @param color The Color to be assigned at the specific pixel.
     */
    public void setColorAt(int x, int y, greenfoot.Color color)
    {
        setRGBAt(x, y, color.getColorObject().getRGB());
    }

    /**
     * Set the transparency of the image.
     * 
     * @param t A value in the range 0 to 255. 0 is completely transparent
     *            (invisible) and 255 is completely opaque (the default).
     */
    public void setTransparency(int t)
    {
        if (t < 0 || t > 255) {
            throw new IllegalArgumentException("The transparency value has to be in the range 0 to 255. It was: " + t);
        }

        this.transparency = t;
    }

    /**
     * Return the current transparency of the image.
     * 
     * @return A value in the range 0 to 255. 0 is completely transparent
     *         (invisible) and 255 is completely opaque (the default).
     */
    public int getTransparency()
    {
        return transparency;
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
        Graphics2D g = getGraphics();
        g.fillRect(x, y, width, height);
        g.dispose();
    }

    /**
     * Clears the image.
     */
    public void clear()
    {
        Graphics2D g = getGraphics();
        g.clearRect(0, 0, getWidth(), getHeight());
        g.dispose();
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
        Graphics2D g = getGraphics();
        g.drawRect(x, y, width, height);
        g.dispose();
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
        Graphics2D g = getGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int height = g.getFontMetrics(g.getFont()).getHeight();
        
        String[] lines = GraphicsUtilities.splitLines(string);
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], x, y + (i * height));
        }
        
        g.dispose();
    }

    /**
     * Draw a shape directly on the image. Shapes are specified by the <a href=
     * "https://docs.oracle.com/javase/8/docs/api/java/awt/Shape.html">shape
     * interface</a>.
     * @param shape the shape to be drawn.
     */
    public void drawShape(Shape shape)
    {
        Graphics2D g = getGraphics();
        g.draw(shape);
        g.dispose();
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
        Graphics2D g = getGraphics();
        g.fillOval(x, y, width, height);
        g.dispose();
    }

    /**
     * Draw an oval bounded by the specified rectangle with the current drawing
     * color.
     * 
     * @param x the <i>x </i> coordinate of the upper left corner of the oval to
     *            be drawn.
     * @param y the <i>y </i> coordinate of the upper left corner of the oval to
     *            be drawn.
     * @param width the width of the oval to be drawn.
     * @param height the height of the oval to be drawn.
     */
    public void drawOval(int x, int y, int width, int height)
    {
        Graphics2D g = getGraphics();
        g.drawOval(x, y, width, height);
        g.dispose();
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
     * @param xPoints an array of <code>x</code> coordinates.
     * @param yPoints an array of <code>y</code> coordinates.
     * @param nPoints the total number of points.
     */
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {
        Graphics2D g = getGraphics();
        g.fillPolygon(xPoints, yPoints, nPoints);
        g.dispose();
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
        Graphics2D g = getGraphics();
        g.drawPolygon(xPoints, yPoints, nPoints);
        g.dispose();
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
        Graphics2D g = getGraphics();
        g.drawLine(x1, y1, x2, y2);
        g.dispose();
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
            return "Image file name: " + imageFileName +   "   Image url: " + imageUrl + "  " + superString;
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
            Graphics2D graphics = bImage.createGraphics();
            initGraphics(graphics);
            graphics.drawImage(image, 0, 0, null);
            image = bImage;
            copyOnWrite = false;
            graphics.dispose();
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
     * Wait until the image is fully loaded and then init the graphics.
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