/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.util;

import greenfoot.platforms.GreenfootUtilDelegate;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bluej.Config;
import bluej.utility.Utility;
import javax.imageio.ImageIO;



/**
 * General utility methods for Greenfoot.
 * 
 * @author Davin McCall
 * @version $Id: GreenfootUtil.java 6423 2009-07-09 13:35:24Z mjrb4 $
 */
public class GreenfootUtil
{
    // constants for use with createSpacer()
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    
    private static GreenfootUtilDelegate delegate;

    private static final Color urlColor = new Color(0, 90, 200);
    
    public static void initialise(GreenfootUtilDelegate newDelegate) {
        delegate = newDelegate;
    }
    
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
     * Get a spacer along the specified axis and with the specified width.
     * 
     * A Spacer is like a strut, but with a minimum height/width of 0,
     * so it will collapse to provide additional space to other
     * components if necessary.
     */
    public static JComponent createSpacer(int axis, int width)
    {
        JPanel spacer = new JPanel();
        
        spacer.setMinimumSize(new Dimension(0,0));
        
        Dimension size = new Dimension();
        
        // Preferred size...
        size.width = 0;
        size.height = 0;
        if (axis == X_AXIS) {
            size.width = width;
        }
        else {
            size.height = width;
        }
        spacer.setPreferredSize(size);
        
        // Maximum size
        spacer.setMaximumSize(size);

        spacer.setBorder(null);
        
        return spacer;
    }
    
    /**
     * Create a JLabel suitable for displaying help text (small font).
     */
    public static JLabel createHelpLabel()
    {
        JLabel helpLabel = new JLabel();
        Font smallFont = helpLabel.getFont().deriveFont(Font.ITALIC, 11.0f);
        helpLabel.setFont(smallFont);
        return helpLabel;
    }

    /**
     * Check whether a given file is an image that can be read by Java.
     * @param file the file to check
     * @return true if the file is a valid image, false otherwise.
     */
    public static boolean isImage(File file)
    {
        try {
            BufferedImage img = ImageIO.read(file);
            if(img==null) return false;
            return true;
        }
        catch(Exception ex) {
            return false;
        }
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
        BufferedImage rImage = GraphicsUtilities.createCompatibleTranslucentImage(w, h);
        Graphics2D graphics = rImage.createGraphics();
        // We'd like interpolated image rendering.
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
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

    /**
     * Opens a file browser and lets the user select a greenfoot scenario.
     * @param parent For placing the file browser
     * @return The selected scenario or null if nothing was selected
     */
    public static File getScenarioFromFileBrowser(Component parent) {
        return delegate.getScenarioFromFileBrowser(parent);
    }


    /**
     *  Get a file name from the user, using a file selection dialogue.
     *  If cancelled or an invalid name was specified, return null.
     */
    public static String getNewProjectName(Component parent)
    {
        return delegate.getNewProjectName(parent);
    }
    

    
    /**
     * Tries to find the filename using the classloader. It first searches in
     * 'projectdir/dir/', then in the 'projectdir' and last as an absolute
     * filename or URL.
     * 
     * @param filename Name of the file
     * @param dir directory to search in first
     * @return A URL that can be read or null if the URL could not be found.
     * @throws FileNotFoundException If the file cannot be found.
     */
    public static URL getURL(final String filename, final String dir) throws FileNotFoundException
    {
        if (filename == null) {
            throw new NullPointerException("Filename must not be null.");
        }
        
        URL url = null;
       
        // If the 'dir' is part of the filename already, we attempt to use the
        // filename alone first in order to avoid a wrong lookup for applets,
        // because this can take a while over the net.
        boolean pathContainsDir = false;
        if (dir != null) {
            // TODO It might not actually be the file system separator that
            // should be used. Should also check for "/" to make it work on
            // windows. If an applet it is probably "/" that is used on all 
            // platforms.
            int sepLoc = filename.lastIndexOf(System.getProperty("file.separator"));
            if(sepLoc > 0) {
              String pathOnly = filename.substring(0,sepLoc);
              pathContainsDir = pathOnly.endsWith(dir);
            }
        }        

        // Also look for ':' which will indicate some sort of protocol (http,
        // jar, file, etc) which will be absolute and hence no need to use
        // 'dir'.
        boolean pathContainsColon = filename.contains(":");
        
        boolean dirSearched = false;     
        if (!pathContainsColon && !pathContainsDir) {
            // First, try the project directory, unless it is already part of the filename or an absolute path.
            url = delegate.getResource(dir + "/" + filename);
            dirSearched = true;
        } 

        if (url == null) {
            url = delegate.getResource(filename);
        }
        if (url == null && !dirSearched) {
            // Second, try the specified dir
            url = delegate.getResource(dir + "/" + filename);
            dirSearched = true;
        }
        if (url == null) {
            // Third, try as an absolute file
            File f = new File(filename);
            
            try {
                if (f.canRead()) {
                    url = f.toURI().toURL();
                }
            }
            catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (AccessControlException ace) {
                // Can get this when running in an applet (on Mac, when filename contains whitespace)
                // Should be safe to ignore, since it will be picked up as an URL below.
            }        
        }
        if(url == null) {
            // Fourth, try as an absolute  URL.
            InputStream s = null;
            try {
                url = new URL(filename);
                s = url.openStream();
                s.close();
            }
            catch (MalformedURLException e) {
                url =null;
            }
            catch (IOException e) {
                url =null;
            } finally {
                if(s != null) {
                    try {
                        s.close();
                    }
                    catch (IOException e) {
                    }
                }
            }
        }

        checkCase(url);

        if(url == null) {
            throw new FileNotFoundException("Could not find file: " + filename);
        }
        return url;
    }

    
    /**
     * Checks whether the case is correct for the given URL. If it is detected
     * NOT to be the right case a IllegalArgumentException will be thrown.
     * 
     * @throws IllegalArgumentException If the case is wrong.
     */
    private static void checkCase(URL url)
    {
        if (url != null) {
            String errMsg = null;
            try {
                File f = new File(url.toURI());
                String givenName = f.getName();
                String realName = f.getCanonicalFile().getName();
                if (!realName.equals(givenName) && realName.equalsIgnoreCase(givenName)) {
                    errMsg = "Filename \'" + givenName + "\' has the wrong case. It should be: \'" + realName + "\'";
                }

            }
            catch (Throwable e) {
                // things might go wrong if we are running in an applet or from
                // a jar. Just ignore all exceptions.
            }
            if (errMsg != null) {
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * 
     * Creates the skeleton for a new class
     * 
     */
    public static void createSkeleton(String className, String superClassName, File file, String templateFileName) throws IOException   {
        delegate.createSkeleton(className, superClassName, file, templateFileName);
    }

    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    public static String getGreenfootLogoPath()
    {        
        return delegate.getGreenfootLogoPath();
    }
    
    public static boolean canBeInstantiated(Class<?> cls) {
        // ACC_INTERFACE 0x0200 Is an interface, not a class.
        // ACC_ABSTRACT 0x0400 Declared abstract; may not be
        // instantiated.
        if(cls == null)
            return false;
        int modifiers = cls.getModifiers();
        return ( (0x0600 & modifiers) == 0x0000);
    }
    
    /**
     * Creates a new image which is a copy of the original with a drop shadow added.
     * 
     */
    public static BufferedImage createDragShadow(BufferedImage image)
    {
        BufferedImage dragImage = ShadowRenderer.createDropShadow(image, 3, 0.3f, Color.BLACK);
        Graphics2D g2 = dragImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return dragImage;
    }
    
    /**
     * Create a new button for the use in the Greenfoot UI. 
     */
    public static JButton createButton(Action action)
    {
        JButton button = new JButton(action);
        button.setFocusable(false);
        return button;
    }
    
    /**
     * Creates a new font derived from the one passed in, but with an added underline.
     */
    @SuppressWarnings("unchecked")
    private static Font deriveUnderlinedFont(Font f)
    {
        Map attr =  f.getAttributes();                
        attr.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        Font underLineFont = f.deriveFont(attr);
        return underLineFont;
    }

    /**
     * Makes this label into a clickable link to some website. It will modify
     * the font to look like a classic link from HTML pages by making it blue
     * with an underline.
     * 
     * @param label The label to make into a hyperlink.
     * @param url The url to open when clicked.
     */
    public static void makeLink(final JLabel label, final String url)
    {
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setForeground(urlColor);
        Font f = label.getFont();
        Font underLineFont = deriveUnderlinedFont(f);
        label.setFont(underLineFont);
        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                Utility.openWebBrowser(url);
            }
        });
    }
    
    /**
     * Replaces all occurrences of BlueJ with Greenfoot in the title of the frame.
     * <p>
     * Should be called from event thread.
     */
    public static void makeGreenfootTitle(Frame frame)
    {
        String title = frame.getTitle();
        String newTitle = title.replaceAll("BlueJ", "Greenfoot");
        frame.setTitle(newTitle);
    }    
    

    /**
     * Replaces all occurrences of BlueJ with Greenfoot in the title of the dialog.
     * <p>
     * Should be called from event thread.
     */
    public static void makeGreenfootTitle(Dialog dialog)
    {
        String title = dialog.getTitle();
        String newTitle = title.replaceAll("BlueJ", "Greenfoot");
        dialog.setTitle(newTitle);
    }    
    
    
    /**
     * Tries to locate the top level greenfoot dir. This method takes the
     * different platforms into account. Specifically the Mac has a different
     * structure.
     * 
     * @throws IOException If it can't read the greenfoot dir.
     * 
     */
    public static File getGreenfootDir()
        throws IOException
    {
        File libDir = Config.getBlueJLibDir();
        // The parent dir of the lib dir is the top level dir of greenfoot
        File greenfootDir = libDir.getParentFile();
        // But on the mac it is further back in the hierarchy.
        if (Config.isMacOS() && greenfootDir != null && greenfootDir.toString().endsWith(".app/Contents/Resources")) {
            greenfootDir = greenfootDir.getParentFile().getParentFile().getParentFile();
        }
        if (greenfootDir == null || !(greenfootDir.isDirectory() && greenfootDir.canRead())) {
            throw new IOException("Could not read from greenfoot directory: " + greenfootDir);
        }
        return greenfootDir;
    }

    /**
     * Opens the given page of the Greenfoot API documentation in a web browser.
     * @param page name of the page relative to the root of the API doc.
     * @throws IOException If the greenfoot directory can not be read
     */
    public static void showApiDoc(String page)
        throws IOException
    {
        String customUrl = Config.getPropString("greenfoot.url.javadoc", null);
        if(customUrl != null) {
            Utility.openWebBrowser(customUrl);
        }
        else {
            File greenfootDir = GreenfootUtil.getGreenfootDir();
            File location = new File(greenfootDir, "/doc/API/" + page);
            if (location.canRead()) {
                Utility.openWebBrowser(location);
            }
        }
    }
}
