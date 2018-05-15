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

import greenfoot.event.SimulationListener;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Contains a collection of sounds that are currently open (playing or paused).
 * 
 * @author Poul Henriksen
 */
public class SoundCollection implements SimulationListener, SoundPlaybackListener
{
    /** Sounds currently playing or paused by this SoundCollection. */
    private Set<Sound> playingSounds = new HashSet<Sound>();
    
    /** Sounds paused by the user code. */
    private Set<Sound> pausedSounds = new HashSet<Sound>(); 
    
    /** Sounds closed by the user code. */
    private Set<Sound> stoppedSounds = new HashSet<Sound>();     
    
    private volatile boolean ignoreEvents = false;
    
    /**
     * Stop sounds when simulation is disabled (a new world is created).
     */
    @OnThread(Tag.Any)
    public void simulationChangedAsync(AsyncEvent e)
    {
        if (e == AsyncEvent.DISABLED) {
            close();
        }
    }

    @Override
    public @OnThread(Tag.Simulation) void simulationChangedSync(SyncEvent e)
    {
    }

    /**
     * Stops all sounds and makes them release the resources.
     * 
     */
    private void close()
    {    	
        synchronized (this) {
            ignoreEvents = true;
        }
        
        Iterator<Sound> iter = playingSounds.iterator();
        while (iter.hasNext() ) {
            Sound sound = iter.next();
            sound.close();
        }
        
        iter = pausedSounds.iterator();
        while (iter.hasNext() ) {
            Sound sound = iter.next();
            sound.close();
        }
        

        iter = stoppedSounds.iterator();
        while (iter.hasNext() ) {
            Sound sound = iter.next();
            sound.close();
        }
        
        playingSounds.clear();
        pausedSounds.clear();
        stoppedSounds.clear();

        synchronized (this) {
            ignoreEvents = false;
        }
    }

    // Listener callbacks

    public synchronized void playbackStarted(Sound sound)
    {
        if (!ignoreEvents) {
            playingSounds.add(sound);
            pausedSounds.remove(sound);
            stoppedSounds.remove(sound);
        }
    }

    public synchronized void playbackStopped(Sound sound)
    {       
        if (!ignoreEvents) {
            playingSounds.remove(sound);
            pausedSounds.remove(sound);
            stoppedSounds.add(sound);
        }
    }

    public synchronized void playbackPaused(Sound sound)
    {
        if (!ignoreEvents) {
            pausedSounds.add(sound);
            playingSounds.remove(sound);
            stoppedSounds.remove(sound);
        }
    }
    
    public synchronized void soundClosed(Sound sound) 
    {
        if (!ignoreEvents) {
            pausedSounds.remove(sound);
            playingSounds.remove(sound);
            stoppedSounds.remove(sound);
        }
    }
}
