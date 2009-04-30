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

import greenfoot.util.GreenfootUtil;

import java.io.IOException;
import java.net.URL;

/**
 * Class responsible for creating Sounds and loading them.
 * 
 * @author Poul Henriksen
 */
public class SoundFactory 
{

    /** singleton */
    private static SoundFactory instance;    

    private SoundCollection soundCollection;
    
    private SoundCache soundCache = new SoundCache();

	/**
	 * Only use clips when the size of the clip is below this value (size of the
	 * file in bytes). 
	 * TODO: make this user configurable for platforms where
	 * clips don't work so well. What about applets?
	 */
	private static final int maxClipSize = 500 * 1000;

    private SoundFactory()
    {
       soundCollection = new SoundCollection();
    }

    public synchronized static SoundFactory getInstance()
    {
        if (instance == null) {
            instance = new SoundFactory();
        }
        return instance;
    }
    
    public SoundCollection getSoundCollection() {
        return soundCollection;
    }
   
    /**
     * Creates the sound from file.
     * 
     * @param file Name of a file or an url
     */
    public Sound createSound(final String file) 
    {      
    	try {
			// First, determine the size of the sound, if possible
			URL url = GreenfootUtil.getURL(file, "sounds");

			int size = url.openConnection().getContentLength();

			if (isStream(size)) {
				// If we can not get the size, or if it is a big file we stream
				// it in a thread.

				System.out.println("Creating stream: " + file);
				final Sound soundStream = new SoundStream(url, soundCollection);
				return soundStream;
			} else {
				System.out.println("Creating clip: " + file);
				// The sound is small enough to be loaded into memory as a clip.
				SoundClip sound = new SoundClip(file, url, soundCollection);
				return sound;
			}
		} catch (IOException e) {
			SoundExceptionHandler.handleIOException(e, file);
		}  
		return null;
    }
    
    /**
     * Gets a cached sound file if possible. If not possible, it will return a new sound.
     * 
     */
    public Sound getCachedSound(final String file)  
    {      
    	Sound sound = soundCache.get(file);
        if(sound == null) {
        	sound = createSound(file);
        	if(sound instanceof SoundClip) {
        		soundCache.put((SoundClip) sound);
        	}
        	System.out.println("Cache miss on: " + file);
        } 
        else {
        	System.out.println("Cache hit on: " + file);
        }
        return sound;
    }     

	private boolean isStream(int size) {
		return size == -1 || size > maxClipSize;
	}

}
