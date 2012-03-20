/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012  Poul Henriksen and Michael Kolling 
 
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

import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Class responsible for creating Sounds and loading them.
 * 
 * @author Poul Henriksen
 */
public class SoundFactory 
{
    /** singleton */
    private static SoundFactory instance;    

    /**
     * Collection of all sounds, which can be used to affect the state of all
     * sounds, for instance it can pause/resume all sounds.
     */
    private SoundCollection soundCollection;
    
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
        
        for (String soundFile : GreenfootUtil.getSoundFiles())
        {
            // This loads the file, and if it's a SoundClip, puts it in
            // the sound cache.  It also happens to make objects for
            // non-SoundClip items, but since they are all streams,
            // that shouldn't cause a big slowdown or waste of resources.
            Sound s = createSound(soundFile, true);
            
            if (s instanceof SoundClip)
                ((SoundClip)s).preLoad();
            
            // if (!soundCache.hasFreeSpace())
            //    return; // No point continuing
        }
    }

    public synchronized static SoundFactory getInstance()
    {
        if (instance == null) {
            instance = new SoundFactory();
        }
        return instance;
    }
    
    public SoundCollection getSoundCollection()
    {
        return soundCollection;
    }
   
    /**
     * Creates the sound from file.
     * 
     * @param file Name of a file or an url
     * @param quiet   if true, failure is silent; otherwise an IllegalArgumentException may be thrown
     * @throws IllegalArgumentException if the specified sound is invalid or cannot be played, unless quiet is true
     * @return  a sound, or if quiet is true, possibly null
     */
    public Sound createSound(final String file, boolean quiet)
    {      
        try {
            URL url = GreenfootUtil.getURL(file, "sounds");
            int size = url.openConnection().getContentLength();
            if (isMidi(url)) {
                return new MidiFileSound(url, soundCollection);
            }
            else if(!GreenfootUtil.isMp3LibAvailable() && isMp3(url)) {
                // This is an mp3 file but we don't have the mp3 library available.
                SoundExceptionHandler.handleMp3LibNotAvailable();
            }   
            else if(isMp3(url)) {
                return new SoundStream(new Mp3AudioInputStream(url), soundCollection);
            }            
            else if (isJavaAudioStream(size)) {
                return new SoundStream(new JavaAudioInputStream(url), soundCollection);
            } 
            else {
                // The sound is small enough to be loaded into memory as a clip.
                return new SoundClip(file, url, soundCollection);
            }
        } catch (IOException e) {
            if (! quiet) {
                SoundExceptionHandler.handleIOException(e, file);
            }
        } catch (UnsupportedAudioFileException e) {
            if (! quiet) {
                SoundExceptionHandler.handleUnsupportedAudioFileException(e, file);
            }
        }  
        return null;
    }
    
    private boolean isJavaAudioStream(int size)
    {
        // If we can not get the size, or if it is a big file we stream
        // it in a thread.
        return size == -1 || size > maxClipSize;
    }    

    private boolean isMidi(URL url)
    {
        String lowerCaseName = url.toString().toLowerCase();
        return lowerCaseName.endsWith("mid") || lowerCaseName.endsWith("midi");
    }    

    private boolean isMp3(URL url)
    {
        String lowerCaseName = url.toString().toLowerCase();
        return lowerCaseName.endsWith("mp3");
    }
}
