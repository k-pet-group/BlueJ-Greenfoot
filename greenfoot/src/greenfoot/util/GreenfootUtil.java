package greenfoot.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * General utility methods for Greenfoot.
 * 
 * @author Davin McCall
 * @version $Id: GreenfootUtil.java 3995 2006-04-21 10:12:54Z polle $
 */
public class GreenfootUtil
{
    /**
     * Extracts the name of a class from the qualified class name.
     */
    public static String extractClassName(String qualifiedName) {
        int index = qualifiedName.lastIndexOf('.');
        String name = qualifiedName;
        if (index >= 0) {
            name = qualifiedName.substring(index + 1);
        }
        return name;
    }
    
    /**
     * Extracts the package of a class from the qualified class name.
     */
    public static String extractPackageName(String qualifiedName) {
        int index = qualifiedName.lastIndexOf('.');
        String name = "";
        if (index >= 0) {
            name = qualifiedName.substring(0, index);
        }
        return name;
    }    
    
    /**
     * Scale an image, but avoid stretching small images and changing of the image's
     * aspect ratio. If the input image is smaller than the desired size, it is centered
     * in the output image.
     * 
     * @param inputImage  The image to scale
     * @param w           The desired width
     * @param h           The desired height
     * @return         The scaled image
     */
    public static Image getScaledImage(Image inputImage, int w, int h)
    {
        ImageWaiter waiter = new ImageWaiter(inputImage);
        
        waiter.waitDimensions();
        int inputw = waiter.width;
        int inputh = waiter.height;
        
        // If the image is already the correct size, return it.
        if (w == inputw && h == inputh) {
            return inputImage;
        }
        
        // Otherwise create a new image
        BufferedImage rImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = rImage.createGraphics();
        
        // if the input image is smaller in both dimensions than the required
        // image, just stamp it.
        if (inputw <= w && inputh <= h) {
            int xoffs = (w - inputw) / 2;
            int yoffs = (h - inputh) / 2;
            // graphics.drawImage(inputImage, xoffs, yoffs, null);
            waiter.drawWait(graphics, xoffs, yoffs);
        }
        else {
            // Otherwise, scale the image, maintaining the aspect ratio
            float xscale = (float) w / inputw; // required scaling horizontal
            float yscale = (float) h / inputh; // required scaling vertical
        
            // The scale factor we use must be the lower of the required horizontal
            // scaling and required vertical scaling. We then use the scale for both
            // horizontal and vertical scaling, thereby preserving aspect ratio.
            float scale = xscale < yscale ? xscale : yscale;
            
            // Scale, and check that rounding hasn't caused problems
            int neww = (int)(inputw * scale);
            int newh = (int)(inputh * scale);
            if (neww > inputw) {
                neww = inputw;
            }
            if (newh > inputh) {
                newh = inputh;
            }
            if (neww < 1) {
                neww = 1;
            }
            if (newh < 1) {
                newh = 1;
            }
            if (neww < w && newh < h) {
                neww++; newh++;
            }

            // draw the scaled image centered
            int xoffs = (w - neww) / 2;
            int yoffs = (h - newh) / 2;
            // graphics.drawImage(inputImage, xoffs, yoffs, neww, newh, null);
            waiter.drawWait(graphics, xoffs, yoffs, neww, newh);
        }
        return rImage;
    }
    
    /**
     * A class which conveniently allows us to synchronously retrieve the
     * width and height of an Image, and to draw the image to a graphics
     * context.
     * 
     * @author Davin McCall
     */
    public static class ImageWaiter implements ImageObserver
    {
        public int width;
        public int height;
        public boolean done;
        public boolean gotDimensions;
        public Image src;
        
        public ImageWaiter(Image src)
        {
            this.src = src;
            done = false;
            gotDimensions = false;
            synchronized (this) {
                width = src.getWidth(this);
                height = src.getHeight(this);
                if (width != -1 && height != -1) {
                    gotDimensions = true;
                }
            }
        }
        
        /**
         * Wait until we have the dimensions of the image.
         */
        public synchronized void waitDimensions()
        {
            try {
                while (! gotDimensions) {
                    wait();
                }
            }
            catch (InterruptedException ie) {}
        }
        
        /**
         * Draw the source image to a graphics context and wait for the drawing
         * operation to complete.
         * 
         * @param canvas   The graphics context to draw to
         * @param x        The x position to draw to
         * @param y        The y position to draw to
         */
        public synchronized void drawWait(Graphics canvas, int x, int y)
        {
            done = canvas.drawImage(src, x, y, this);
            try {
                while (! done) {
                    wait();
                }
            }
            catch (InterruptedException ie) {}
        }
        
        /**
         * Scale and draw the source image to a graphics context and wait for the
         * drawing operation to complete.
         * 
         * @param canvas   The graphics context to draw to
         * @param x        The x position to draw to
         * @param y        The y position to draw to
         * @param w        The width to scale to
         * @param h        The height to scale to
         */
        public synchronized void drawWait(Graphics canvas, int x, int y, int w, int h)
        {
            done = canvas.drawImage(src, x, y, w, h, this);
            try {
                while (! done) {
                    wait();
                }
            }
            catch (InterruptedException ie) {}
        }
        
        /*
         * This is the asynchronous callback for when the dimensions of the image
         * become available, or the draw operation finishes.
         * 
         *  (non-Javadoc)
         * @see java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
         */
        public synchronized boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
        {
            // First stage: get image dimensions
            if (! gotDimensions) {
                if ((infoflags & WIDTH) != 0) {
                    this.width = width;
                }
                
                if ((infoflags & HEIGHT) != 0) {
                    this.height = height;
                }
                
                if (this.width != -1 && this.height != -1) {
                    gotDimensions = true;
                    notify();
                    return false;
                }
                else {
                    return true;
                }
            }
            
            // Second stage: wait for draw operation to complete
            if ((infoflags & (FRAMEBITS | ALLBITS | ERROR | ABORT)) != 0) {
                done = true;
                notify();
                return false;
            }
            
            return true;
        }
    }
    
    
    /**
     * 
     * Copies the src-DIR recursively into dst.
     * 
     */
    public static void copyDir(File src, File dst)
    {
        if (!src.isDirectory()) {
            return;
        }
        if (!dst.exists()) {
            dst.mkdirs();
        }
        File[] files = src.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            File newDst = new File(dst, file.getName());
            if (file.isDirectory()) {
                copyDir(file, newDst);
            }
            else {
                copyFile(file, newDst);
            }
        }
    }

    /**
     * Copies the src to dst. Creating parent dirs for dst. If dst exist it
     * overrides it.
     * 
     * @param src
     *            The source. It must be a file
     * @param dst
     *            Must not exist as a DIR
     */
    public static void copyFile(File src, File dst)
    {
        if (!src.isFile() || dst.isDirectory()) {
            return;
        }
        dst.getParentFile().mkdirs();
        if (dst.exists()) {
            dst.delete();
        }
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(src));
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dst));

            byte[] buffer = new byte[8192];
            int read = 0;
            while (read != -1) {
                os.write(buffer, 0, read);
                read = is.read(buffer);
            }
            os.flush();
            is.close();
            os.close();
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {

        }
    }
        
}
