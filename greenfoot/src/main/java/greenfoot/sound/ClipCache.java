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
package greenfoot.sound;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * A cache for soundclip data.
 * 
 * @author Davin McCall
 */
public class ClipCache
{
    /** Data for clips that aren't currently in use */
    private LinkedHashMap<String,ClipData> freeClips = new LinkedHashMap<String,ClipData>();
    private int numberFreeClips = 0;
    
    private static int MAX_CACHED_CLIPS = 20;
    
    /** Data for clips that are in use */
    private Map<String,ClipData> cachedClips = new HashMap<String,ClipData>();
    
    public synchronized ClipData getCachedClip(URL url)
        throws IOException, UnsupportedAudioFileException
    {
        String urlStr = url.toString();
        ClipData data = cachedClips.get(urlStr);
        if (data == null) {
            // Maybe we have a free clip
            data = freeClips.remove(urlStr);
            if (data != null) {
                numberFreeClips --;
                cachedClips.put(urlStr, data);
            }
        }
        if (data == null) {
            // We need to create a new clip
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            AudioFormat af = ais.getFormat();
            long frameLength = ais.getFrameLength();
            
            int total = (int)(af.getFrameSize() * frameLength);
            byte[] allBytes = new byte[(int)(af.getFrameSize() * frameLength)];
            int pos = 0;
            
            try {
                while (pos < total) {
                    int r = ais.read(allBytes, pos, total - pos);
                    if (r == -1) {
                        break;
                    }
                    pos += r;
                }
            }
            finally {
                ais.close();
            }
            
            data = new ClipData(urlStr, allBytes, af, (int) frameLength);
        }
        else {
            data.addUser();
        }
        
        return data;
    }
    
    public synchronized void releaseClipData(ClipData data)
    {
        if (data.release()) {
            cachedClips.remove(data.getUrl());
            freeClips.put(data.getUrl(), data);
            numberFreeClips++;
            if (numberFreeClips > MAX_CACHED_CLIPS) {
                // remove least recently used free clip
                Iterator<ClipData> it = freeClips.values().iterator();
                it.next();
                it.remove();
                numberFreeClips = MAX_CACHED_CLIPS;
            }
        }
    }
}
