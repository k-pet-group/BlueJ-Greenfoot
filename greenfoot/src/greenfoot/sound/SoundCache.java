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
package greenfoot.sound;

/**
 * Cache for holding SoundClips.
 *
 * @author mik
 * @author Poul Henriksen
 */
public class SoundCache
{
    private static final int CACHE_SIZE = 20;

    private SoundClip[] cache = new SoundClip[CACHE_SIZE];
    private int nextEntry;

    /**
     * Create an empty SoundCache.
     */
    public SoundCache()
    {
        nextEntry = 0;
    }
    
    /**
     * Indicates whether the next slot in the cache has never been used.
     * This is only useful very early on in the lifetime of the cache, to check
     * when the cache first reaches its capacity.  After that, hasFreeSpace()
     * will always return false.
     */
    public boolean hasFreeSpace()
    {
        return cache[nextEntry] == null;
    }

    /**
     * Add a sound to the cache.
     */
    public void put(SoundClip sound)
    {
        if (cache[nextEntry] != null) {
            // Make sure the discarded clip is closed when it has finished
            // pĺaying.
            cache[nextEntry].setCloseWhenFinished(true);
        }
        cache[nextEntry] = sound;
        nextEntry = (nextEntry + 1) % CACHE_SIZE;
    }

    /**
     * Try to find a sound in the cache. Returns the sound or null.
     */
    public SoundClip get(String name)
    {
        int i = 0;
        while (i < CACHE_SIZE) {
            if (cache[i] != null && name.equals(cache[i].getName())
                    && !cache[i].isPlaying()) {
                return cache[i];
            }
            i++;
        }
        return null;
    }
}
