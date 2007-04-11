/*
 * Class SoundCache is a cache for recently played sounds.
 *
 * @author mik
 * @version 1.0
 */

package greenfoot.sound;


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
     * Add a sound to the cache.
     */
    public void put(SoundClip sound)
    {
        cache[nextEntry] = sound;
        nextEntry = (nextEntry+1) % CACHE_SIZE;
    }
    
    /**
     * Try to find a sound in the cache. Returns the sound or null.
     */
    public SoundClip get(String name) 
    {
        int i = 0;
        while(i < CACHE_SIZE) {
            if(cache[i] != null && name.equals(cache[i].getName()) && !cache[i].isPlaying()) {
                return cache[i];
            }
            i++;
        }
        return null;
    }
}
