/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015 Michael Kölling and John Rosenberg 
 
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
package bluej.utility.javafx;
import java.util.HashMap;

/**
 * A cache from keys to values of a calculation which must be performed on the
 * FX thread.
 * 
 * A cache has a maximum size.  It is a very naive most-recently-used implementation;
 * if the cache gets full, it discards the values and starts refilling on demand.
 * This could be improved upon.
 * 
 * @param <K> The keys (compared using same method as HashMap)
 * @param <V> The values, arbitrary.
 */
public class FXCache<K, V>
{
    /**
     * The actual cache
     */
    private final HashMap<K, V> cache = new HashMap<>();
    /**
     * The calculation to produce the new value, given a key
     */
    private final FXFunction<K, V> calculate;
    /**
     * The size limit on the cache
     */
    private final int limit;

    /**
     * Creates a new FXCache
     * 
     * @param calculate The FX-thread function to calculate an output/value given
     *                  an input/key
     * @param limit The maximum size of the cache
     */
    public FXCache(FXFunction<K, V> calculate, int limit)
    {
        this.calculate = calculate;
        this.limit = limit;
    }

    /**
     * Gets a value for a key, either cached or calculating it if necessary.
     */
    public V get(K key)
    {
        // We have a size limit, and easiest way to make sure old values are purged is to clear
        // them all and let them be re-added as needed:
        if (cache.size() > limit)
            cache.clear();
        return cache.computeIfAbsent(key, calculate::apply);
    }

    /**
     * Clears the cache
     */
    public void clear()
    {
        cache.clear();
    }
}
