/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.runtime;

/**
 * A simple map implementation, for use in J2ME. Not efficient, but fine for
 * how it is presently used.
 * 
 * @author Davin McCall
 */
public class BJMap<K,V>
{
    private V[] values;
    private K[] keys;
    
    @SuppressWarnings("unchecked")
    public BJMap()
    {
        values = (V[]) new Object[0];
        keys = (K[]) new Object[0];
    }
    
    public V get(K key)
    {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(key)) {
                return values[i];
            }
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public void put(K key, V value)
    {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(key)) {
                values[i] = value;
                return;
            }
        }
        
        K [] newKeys = (K []) new Object[keys.length + 1];
        V [] newValues = (V []) new Object[keys.length + 1];
        System.arraycopy(keys, 0, newKeys, 0, keys.length);
        System.arraycopy(values, 0, newValues, 0, keys.length);
        newKeys[keys.length] = key;
        newValues[keys.length] = value;
        keys = newKeys;
        values = newValues;
    }
    
    @SuppressWarnings("unchecked")
    public void remove(K key)
    {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(key)) {
                K [] newKeys = (K []) new Object[keys.length - 1];
                V [] newValues = (V []) new Object[keys.length - 1];
                System.arraycopy(keys, 0, newKeys, 0, i);
                System.arraycopy(keys, i+1, newKeys, i, keys.length - i - 1);
                System.arraycopy(values, 0, newValues, 0, i);
                System.arraycopy(values, i+1, newValues, i, keys.length - i - 1);
                keys = newKeys;
                values = newValues;
                return;
            }
        }
    }
    
    public K[] getKeys()
    {
        return keys;
    }
}
