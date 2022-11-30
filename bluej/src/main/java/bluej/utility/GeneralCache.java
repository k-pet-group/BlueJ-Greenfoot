/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A general cache, which caches a fixed number of key/value combinations, and which
 * uses a recently-used strategy to determine which entries to keep.
 * 
 * @author Davin McCall
 */
public class GeneralCache<K,V>
{
    private Map<K,V> cacheMap = new HashMap<K,V>();
    private List<K> cachedKeys = new LinkedList<K>();
    private int cacheSize;
    
    /**
     * Construct a cache to cache the given number of items.
     */
    public GeneralCache(int cacheSize)
    {
        this.cacheSize = cacheSize;
    }
    
    /**
     * Retrieve an entry from the cache. If no value for the given key is cached,
     * the return is null. To determine if a null return was due to a null value
     * or to the cache not containing a value, use containsKey().
     */
    public V get(K key)
    {
        V rval = cacheMap.get(key);
        if (rval != null) {
            // Mark the retrieved key as recently used
            for (Iterator<K> i = cachedKeys.iterator(); ; ) {
                K k = i.next();
                if (k.equals(key)) {
                    i.remove();
                    cachedKeys.add(key);
                    break;
                }
            }
        }
        return rval;
    }
    
    /**
     * Check whether a value for the given key is currently cached.
     */
    public boolean containsKey(K key)
    {
        return cacheMap.containsKey(key);
    }
    
    /**
     * Put an item in the cache. The given key must not already have a cached value.
     */
    public void put(K key, V value)
    {
        if (cachedKeys.size() >= cacheSize) {
            K toRemove = cachedKeys.remove(0);
            cacheMap.remove(toRemove);
        }
        
        cacheMap.put(key, value);
        cachedKeys.add(key);
    }
    
    /**
     * Remove all cache entries.
     */
    public void clear()
    {
        cacheMap.clear();
        cachedKeys.clear();
    }
}
