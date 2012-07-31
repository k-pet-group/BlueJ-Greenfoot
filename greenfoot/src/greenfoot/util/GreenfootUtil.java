/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.GreenfootImage;
import greenfoot.UserInfo;
import greenfoot.core.ImageCache;
import greenfoot.platforms.GreenfootUtilDelegate;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bluej.Boot;
import bluej.Config;
import bluej.utility.Utility;

/**
 * General utility methods for Greenfoot.
 * 
 * @author Davin McCall
 */
public class GreenfootUtil
{
    // constants for use with createSpacer()
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    
    private static GreenfootUtilDelegate delegate;
    private static ImageCache imageCache;

    private static final Color urlColor = new Color(0, 90, 200);
    
    private static boolean haveCheckedForMp3 = false;
    private static boolean mp3available = false;
    
    public static void initialise(GreenfootUtilDelegate newDelegate)
    {
        delegate = newDelegate;
        imageCache = ImageCache.getInstance();
    }
    
    /**
     * Extracts the name of a class from the qualified class name.
     */
    public static String extractClassName(String qualifiedName)
    {
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
    public static String extractPackageName(String qualifiedName)
    {
        int index = qualifiedName.lastIndexOf('.');
        String name = "";
        if (index >= 0) {
            name = qualifiedName.substring(0, index);
        }
        return name;
    }   
    
    /**
     * Get a spacer along the specified axis and with the specified width.
     * <p>
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
     * 
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
            
            // This can throw an exception if the image is too big:
            try {
                waiter.drawWait(graphics, xoffs, yoffs, neww, newh);
            }
            catch (java.lang.OutOfMemoryError oome) {
                // draw a white background overlaid with a red cross
                graphics.setColor(Color.white);
                graphics.fillRect(1, 1, w - 2, h - 2);
                graphics.setColor(Color.red);
                graphics.drawRect(0, 0, w - 1, h - 1);
                graphics.drawLine(0, 0, w, h);
                graphics.drawLine(0, h, w, 0);
            }
        }
        graphics.dispose();
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
        @Override
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
     * Copies the src-DIR recursively into dst.
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
     * Copies the src to dst, creating parent dirs for dst. If dst exists it
     * is overwritten.
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
     * Gets a list of the sound files in this scenario
     * @return A list of files in the sounds subdirectory, without the path prefix (e.g. "foo.wav")
     */
    public static Iterable<String> getSoundFiles()
    {
        return delegate.getSoundFiles();
    }

    /**
     * Tries to find the specified file using the classloader. It first searches in
     * 'projectdir/dir/', then in the 'projectdir' and last as an absolute filename or URL.
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
        
        URL url = delegate.getResource(dir + "/" + filename);

        if (url == null) {
            url = delegate.getResource(filename);
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
                // Not a URL that Java can handle
            }
            catch (SecurityException se) {
                // Can get this when running as an applet
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
                url = null;
            }
            catch (IOException e) {
                url = null;
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
     * Returns the path to a small version of the greenfoot logo.
     */
    public static String getGreenfootLogoPath()
    {        
        return delegate.getGreenfootLogoPath();
    }
    
    /**
     * Check whether a class can be instantiated: it is not abstract
     * or an interface.
     */
    public static boolean canBeInstantiated(Class<?> cls)
    {
        // ACC_INTERFACE 0x0200 Is an interface, not a class.
        // ACC_ABSTRACT 0x0400 Declared abstract; may not be
        // instantiated.
        if (cls == null) {
            return false;
        }
        if (cls.isEnum() || cls.isInterface()) {
            return false;
        }
        return ! Modifier.isAbstract(cls.getModifiers());
    }
    
    /**
     * Creates a new image which is a copy of the original with a drop shadow added.
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
    
    /**
     * Returns a set of the third party libraries used by Greenfoot.
     * 
     */
    public static Set<File> get3rdPartyLibs()
    {
        File bluejLibDir = Config.getBlueJLibDir();      
        String[] thirdPartyLibs = Boot.GREENFOOT_EXPORT_JARS;
        Set<File> jars = new TreeSet<File>();
        for (String lib : thirdPartyLibs) {
            jars.add(new File(bluejLibDir, lib));
        }
        return jars;
    }

    /**
     * Check whether MP3 support is available.
     */
    public static boolean isMp3LibAvailable()
    {
        if (! haveCheckedForMp3) {
            URL url = delegate.getResource("javazoom/jl/decoder/BitstreamException.class");
            mp3available = url != null;
            haveCheckedForMp3 = true;
        }
        return mp3available;
    }

    /**
     * First tries to create the file with the given name and type. If it
     * already exists, it will try creating the file with "01" appended to the
     * filename, if that exists it will try "02" and so on.
     * 
     * @param dir Directory where the file should be created.
     * @param name Base name of the file
     * @param type Type of the file (extension) (without the dot)
     * @throws IOException If an IO error is generate when trying to create the
     *             file.
     */
    public static File createNumberedFile(File dir, String name, String type)
        throws IOException
    {
        File f = new File(dir, name + "." + type);
        int number = 1;
        while (!f.createNewFile()) {
            String numberString = null;
            if (number < 10) {
                numberString = "0" + number;
            }
            else {
                numberString = "" + number;
            }
            f = new File(dir, name + numberString + "." + type);
            number++;
        }
        return f;
    }
    
    /**
     * Retrieves the GreenfootImage either from the cache or a new image if not previously created
     * Adds the image to the cached image list or the null image list (if none was found)
     * @param className name of the class
     * @param imageName filename of the image
     */
    public static GreenfootImage getGreenfootImage(String className, String imageName)
    {   
        //try {throw new Exception();} catch (Exception e) {e.printStackTrace();}
        GreenfootImage image=null;
        if (imageName==null){
            return image;
        }
        if (isInvalidImageFilename(imageName)){
            return image;
        }
        // If it is the Actor class the image is always the same:
        if (className.equals("Actor")) {
            return new GreenfootImage(getGreenfootLogoPath());
        }
        try {
            image = new GreenfootImage(imageName);
        }
        catch (IllegalArgumentException iae) {
            // This occurs if the image file doesn't exist anymore
        }
        return image;
    }

    /**
     * Remove the cached version of an image for a particular class. This should be
     * called when the image for the class is changed. Thread-safe.
     */
    public static void removeCachedImage(String className)
    {
        imageCache.removeCachedImage(className);
    }
   
    /**
     * Adds a filename with the associated image into the cache
     * @param name filename (should be the image filename)
     * @param image GreenfootImage
     */
    public static boolean addCachedImage(String name, GreenfootImage image)
    {
        return imageCache.addCachedImage(name, image);
    }
    
    /**
     * Gets the cached image (if any) of the requested name. Thread-safe.
     * 
     * @param name   name of the image file
     * @return The cached image (should not be modified), or null if the image
     *         is not cached.
     */
    public static GreenfootImage getCachedImage(String name)
    {
        return imageCache.getCachedImage(name);
    }
    
    /**
     * Returns whether the cached image is null
     */
    public static boolean isInvalidImageFilename(String fileName)
    {
        return imageCache.isNullCachedImage(fileName);
    }
    
    /**
     * Display a message to the user; how the message is displayed is dependent
     * upon the platform context. In the Greenfoot IDE, the message will be displayed
     * in a dialog; otherwise it will be written to the terminal/console/log.
     * 
     * @param parent  The parent component (if a dialog is used to display the message)
     * @param messageText   The message text itself.
     */
    public static void displayMessage(Component parent, String messageText)
    {
        delegate.displayMessage(parent, messageText);
    }

    /**
     * Given a string that represents a filename (or long path),
     * chops off the extension if any is present.
     * 
     * So Crab.java becomes Crab, and /tmp/pic.png becomes /tmp/pic
     */
    public static String removeExtension(String full)
    {
        int n = full.lastIndexOf('.');
        if (n == -1) {
            return full;
        }
        else {
            return full.substring(0, n);
        }
    }

    /**
     * Find out whether storage is supported in the current setting
     */
    public static boolean isStorageSupported()
    {
        return delegate.isStorageSupported();
    }

    /**
     * null if an error, blank values if no previous storage
     */
    public static UserInfo getCurrentUserInfo()
    {
        return delegate.getCurrentUserInfo();
    }

    /**
     * returns whether it was successful
     */
    public static boolean storeCurrentUserInfo(UserInfo data)
    {
        if (data.getUserName().equals(getUserName()))
            return delegate.storeCurrentUserInfo(data);
        else
        {
            // This message the user should see, because
            // it indicates a programming mistake:
            System.err.println("Attempted to store the data for another user, \"" + data.getUserName() + "\" (i.e. a user other than the current user, \"" + getUserName() + "\")");
            return false;
        }
    }

    /**
     * null if problem, empty list if simply no data
     * 
     * Returns highest data when sorted by integer index 0
     */
    public static List<UserInfo> getTopUserInfo(int limit)
    {
        return delegate.getTopUserInfo(limit);
    }

    /**
     * returns null if storage not supported.
     */
    public static GreenfootImage getUserImage(String userName)
    {
        if (userName == null || userName.equals("")) {
            userName = getUserName();
        }
        
        GreenfootImage r = null;
        
        if (userName != null) {
            r = delegate.getUserImage(userName);
        }
        
        if (r == null)
        {
            // This can be because there was a problem reading from the gallery,
            // or because we're using local storage:
            r = new GreenfootImage(50, 50);
            r.setColor(java.awt.Color.DARK_GRAY);
            r.fill();
            
            final int CHARS_PER_LINE = 6; // Heuristic: 15 pixels high, assume 8 pixels width per char, 50 / 8 ~= 6
            
            StringBuilder wrappedName = new StringBuilder();
            if (userName == null)
                userName = "";
            for (int i = 0 ;i < userName.length(); i += CHARS_PER_LINE)
                wrappedName.append(userName.substring(i, Math.min(userName.length(), i + CHARS_PER_LINE))).append("\n");
                    
            GreenfootImage textImage = new GreenfootImage(wrappedName.toString(), 15, java.awt.Color.WHITE, java.awt.Color.DARK_GRAY);
            r.drawImage(textImage, Math.max(0, (50 - textImage.getWidth()) / 2), Math.max(0, (50 - textImage.getHeight()) / 2));
        }
        // Should never return null:
        return r;
    }

    // For local storage, this is the username set via the IDE
    // For remote storage, this is the username got from the applet params
    // For turned off, this is null
    public static String getUserName()
    {
        return delegate.getUserName();
    }

    /**
     * Get info for users near the current player when sorted by score
     * 
     * @return  null if problem, empty list if simply no data.
     */
    public static List<UserInfo> getNearbyUserData(int maxAmount)
    {
        return delegate.getNearbyUserInfo(maxAmount);
    }
}
