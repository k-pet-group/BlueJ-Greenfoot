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

public class FXCache<K, V>
{
    private final HashMap<K, V> cache = new HashMap<>();
    private final FXFunction<K, V> calculate;
    private final int limit;
    
    public FXCache(FXFunction<K, V> calculate, int limit)
    {
        this.calculate = calculate;
        this.limit = limit;
    }
    
    public V get(K key)
    {
        // We have a size limit, and easiest way to make sure old values are purged is to clear
        // them all and let them be re-added as needed:
        if (cache.size() > limit)
            cache.clear();
        return cache.computeIfAbsent(key, calculate::apply);
    }
}
