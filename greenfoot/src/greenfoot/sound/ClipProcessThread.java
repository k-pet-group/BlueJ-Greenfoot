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

import java.util.LinkedList;

/**
 * A thread to process certain sound commands, which for some reason
 * must be processed in a separate thread.
 * 
 * @author Davin McCall
 */
public class ClipProcessThread implements Runnable
{
    private Thread thread;
    
    private LinkedList<SoundClip> queue = new LinkedList<SoundClip>();
    
    public ClipProcessThread()
    {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }
    
    public void addToQueue(SoundClip clip)
    {
        synchronized (queue) {
            queue.add(clip);
            queue.notify();
            
            // When running online, threads can be terminated willy-nilly, but
            // static state is kept. We need to check for this:
            if (! thread.isAlive()) {
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.start();
            }
        }
    }
    
    @Override
    public void run()
    {
        try {
            SoundClip item;
            while (true) {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        queue.wait();
                    }
                    item = queue.removeFirst();
                }

                item.processState();
            }
        }
        catch (InterruptedException ie) { }
    }
}
