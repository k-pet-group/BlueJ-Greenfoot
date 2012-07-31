/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2012  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.core;

import greenfoot.GreenfootImage;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * An image cache, which uses soft references to avoid holding images when heap space becomes exhausted.
 * 
 * @author Davin McCall
 */
public class ImageCache
{
    private static ImageCache instance = new ImageCache();
    
    /** A soft reference to a cached image */
    private class CachedImageRef extends SoftReference<GreenfootImage>
    {
        String imgName;
        
        public CachedImageRef(String imgName, GreenfootImage image, ReferenceQueue<GreenfootImage> queue)
        {
            super(image, queue);
            this.imgName = imgName;
        }
    }
    
    private Map<String,CachedImageRef> imageCache = new HashMap<String,CachedImageRef>();
    private ReferenceQueue<GreenfootImage> imgCacheRefQueue = new ReferenceQueue<GreenfootImage>();
    
    /**
     * Retrieve the image cache instance.
     */
    public static ImageCache getInstance()
    {
        return instance;
    }

    /**
     * Requests that an image with associated name be added into the cache. The image may be null,
     * in which case the null response will be cached. Thread-safe.
     * 
     * @return  whether the image was cached.
     */
    public boolean addCachedImage(String fileName, GreenfootImage image) 
    {
        synchronized (imageCache) {
            if (image != null) {
                CachedImageRef cr = new CachedImageRef(fileName, image, imgCacheRefQueue);
                imageCache.put(fileName, cr);
            }
            else {
                imageCache.put(fileName, null);
            }
        }
        return true;
    }

    /**
     * Gets the cached image of the requested fileName. Thread-safe.
     *
     * @param name   name of the image file
     * @return The cached image (should not be modified), or null if the image
     *         is not cached.
     */
    public GreenfootImage getCachedImage(String fileName)
    { 
        synchronized (imageCache) {
            flushImgCacheRefQueue();
            CachedImageRef sr = imageCache.get(fileName);
            if (sr != null) {
                return sr.get();
            }
            return null;
        }
    }

    /**
     * Remove the cached version of an image for a particular class. This should be
     * called when the image for the class is changed. Thread-safe.
     */
    public void removeCachedImage(String fileName)
    {
        synchronized (imageCache) {
            CachedImageRef cr = imageCache.remove(fileName);
            if (cr != null) {
                cr.clear();
            }
        }
    }

    /**
     * Returns true if the fileName exists in the map and the image is cached as being null; 
     * returns false if it exists and is not null or if it does not exist in the map
     */
    public boolean isNullCachedImage(String fileName)
    {
        synchronized (imageCache) {
            return imageCache.containsKey(fileName) && imageCache.get(fileName) == null;
        }
    }

    /**
     * Clear the image cache.
     */
    public void clearImageCache()
    {
        synchronized (imageCache) {
            imageCache.clear();
            imgCacheRefQueue = new ReferenceQueue<GreenfootImage>();
        }
    }

    /**
     * Flush the image cache reference queue.
     * <p>
     * Because the images are cached via soft references, the references may be cleared, but the
     * key will still map to the (cleared) reference. Calling this method occasionally removes such
     * dead keys.
     */
    private void flushImgCacheRefQueue()
    {
        Reference<? extends GreenfootImage> ref = imgCacheRefQueue.poll();
        while (ref != null) {
            if (ref instanceof CachedImageRef) {
                CachedImageRef cr = (CachedImageRef) ref;
                imageCache.remove(cr.imgName);
            }
            ref = imgCacheRefQueue.poll();
        }
    }
}
