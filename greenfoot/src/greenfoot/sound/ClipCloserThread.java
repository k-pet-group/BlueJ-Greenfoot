/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2012,2021  Poul Henriksen and Michael Kolling 
 
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

import java.util.LinkedList;

import javax.sound.sampled.Clip;

/**
 * On OpenJDK/IcedTea (pulseaudio, 2012-03-26) it seems that clips take a long time to close
 * after they have finished playing. To prevent this from blocking us, we have a dedicated thread
 * (this class) to close old clips.
 * 
 * @author Davin McCall
 */
public class ClipCloserThread implements Runnable
{
    private LinkedList<Clip> clips = new LinkedList<Clip>();
    
    private Thread thread;
    
    public ClipCloserThread()
    {
    }
    
    public void addClip(Clip clip)
    {
        synchronized (clips) {
            clips.add(clip);
            clips.notify();
            
            if (thread == null || ! thread.isAlive()) {
                thread = new Thread(this, "Clip closer");
                thread.setDaemon(true);
                thread.start();
            }
        }
    }
    
    @Override
    public void run()
    {
        try {
            while (true) {
                Clip clip;
                synchronized (clips) {
                    while (clips.isEmpty()) {
                        clips.wait();
                    }
                    clip = clips.removeFirst();
                }
                
                clip.close();
            }
        }
        catch (InterruptedException ie) {}
    }
}
