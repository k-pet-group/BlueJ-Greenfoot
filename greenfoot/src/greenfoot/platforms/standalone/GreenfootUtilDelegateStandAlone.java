/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.standalone;

import greenfoot.GreenfootImage;
import greenfoot.platforms.GreenfootUtilDelegate;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GreenfootUtilDelegateStandAlone implements GreenfootUtilDelegate
{
    /** A set containing names of classes whose image couldn't be resolved */
    public Set<String> nullImages = new HashSet<String>();
    /** Holds images for classes. Avoids loading the same image twice */
    public static Map<String, GreenfootImage> classImages = new HashMap<String, GreenfootImage>();
    
    public void createSkeleton(String className, String superClassName, File file, String templateFileName)
    throws IOException
    {
        // Not needed in stand alone
    }

    public URL getResource(String path)
    {
        // Resources from the standalone should always be in a jar, which means
        // they should contain the character "!". If we do get a URL back, and
        // it doesn't contain a ! it is probably because it didn't exists, but
        // the webserver produced an error page at the given URL instead of
        // returning a fail. Therefore, we need to explicitly test for the
        // existence of a ! in the returned URL.
        URL res = this.getClass().getClassLoader().getResource(path);
        if (res != null && res.toString().contains("!")) {  
            return res;
        }
        else {
            if (path.indexOf('\\') != -1) {
                // Looks suspiciously like a Windows path.
                path = path.replace('\\', '/');
                res = this.getClass().getClassLoader().getResource(path);
                if (res != null && res.toString().contains("!")) {  
                    return res;
                }
            }
            return null;
        }
    }

    /**
     * Returns the path to a small version of the greenfoot logo.
     */
    public String getGreenfootLogoPath()
    {    
        return this.getClass().getClassLoader().getResource("greenfoot.png").toString();
    }

    /**
     * Adds an name of a null image
     * @param name to be added
     */
    public void addNullImage(String name)
    {
        nullImages.add(name);
    }

    /**
     * Checks whether the class is known to have no valid image.
     * This is the case if we previously attempted to load the class
     * image, and failed.
     */
    public boolean isClassImageInvalid(String className)
    {
        return nullImages.contains(className);
    }

    public void cacheGreenfootImage(String name, GreenfootImage image)
    {
        if (image != null) {
            addCachedImage(name, image);
        }
        else {
            addNullImage(name);
        }
        
    }
    
    /**
     * Remove the cached version of an image for a particular class. This should be
     * called when the image for the class is changed. Thread-safe.
     */
    public void removeCachedImage(String className)
    {
        synchronized (classImages) {
            classImages.remove(className);
        }
    }
   
    /**
     * Adds a filename with the associated image into the cache
     * @param name filename (should be the image filename)
     * @param image GreenfootImage
     */
    public void addCachedImage(String name, GreenfootImage image)
    {
        synchronized (classImages) {
            classImages.put(name, image);
        }
    }
    
    /**
     * Gets the cached image of the requested name
     * @param name of the image
     */
    public GreenfootImage getCachedImage(String name)
    {
        synchronized (classImages) {
            return classImages.get(name);
        }
    }

}
