/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2014,2015,2016,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.Boot;
import bluej.Config;
import greenfoot.GreenfootImage;
import greenfoot.UserInfo;
import greenfoot.core.ImageCache;
import greenfoot.platforms.GreenfootUtilDelegate;

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
import java.util.Set;
import java.util.TreeSet;

import threadchecker.OnThread;
import threadchecker.Tag;

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

    private static boolean haveCheckedForMp3 = false;
    private static boolean mp3available = false;
    
    @OnThread(Tag.Any)
    public static void initialise(GreenfootUtilDelegate newDelegate)
    {
        delegate = newDelegate;
        imageCache = ImageCache.getInstance();
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Simulation)
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
    @OnThread(Tag.Simulation)
    public static boolean isStorageSupported()
    {
        return delegate.isStorageSupported();
    }

    /**
     * null if an error, blank values if no previous storage
     */
    @OnThread(Tag.Simulation)
    public static UserInfo getCurrentUserInfo()
    {
        return delegate.getCurrentUserInfo();
    }

    /**
     * returns whether it was successful
     */
    @OnThread(Tag.Simulation)
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
    @OnThread(Tag.Simulation)
    public static List<UserInfo> getTopUserInfo(int limit)
    {
        return delegate.getTopUserInfo(limit);
    }

    /**
     * returns null if storage not supported.
     */
    @OnThread(Tag.Simulation)
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
            r.setColor(greenfoot.Color.DARK_GRAY);
            r.fill();
            
            final int CHARS_PER_LINE = 6; // Heuristic: 15 pixels high, assume 8 pixels width per char, 50 / 8 ~= 6
            
            StringBuilder wrappedName = new StringBuilder();
            if (userName == null)
                userName = "";
            for (int i = 0 ;i < userName.length(); i += CHARS_PER_LINE)
                wrappedName.append(userName.substring(i, Math.min(userName.length(), i + CHARS_PER_LINE))).append("\n");
                    
            GreenfootImage textImage = new GreenfootImage(wrappedName.toString(), 15, greenfoot.Color.WHITE, greenfoot.Color.DARK_GRAY);
            r.drawImage(textImage, Math.max(0, (50 - textImage.getWidth()) / 2), Math.max(0, (50 - textImage.getHeight()) / 2));
        }
        // Should never return null:
        return r;
    }

    // For local storage, this is the username set via the IDE
    // For remote storage, this is the username got from the applet params
    // For turned off, this is null
    @OnThread(Tag.Simulation)
    public static String getUserName()
    {
        return delegate.getUserName();
    }

    /**
     * Get info for users near the current player when sorted by score
     * 
     * @return  null if problem, empty list if simply no data.
     */
    @OnThread(Tag.Simulation)
    public static List<UserInfo> getNearbyUserData(int maxAmount)
    {
        return delegate.getNearbyUserInfo(maxAmount);
    }
}
