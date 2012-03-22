/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2012  Poul Henriksen and Michael Kolling 
 
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
 * A controllable timer, keeps track of time since a mark.
 */
public class TimeTracker 
{
    private long startTime;
    private boolean tracking;
    private long timeElapsed;
    
    public void start() 
    {
        if(tracking) {
            return;
        }
        startTime = System.currentTimeMillis();    
        tracking = true;
    }
    
    public void pause()
    {
        if(!tracking) {
            return;
        }
        long timeSincestart = getTimeSinceStart();
        timeElapsed += timeSincestart;
        tracking = false;
    }

    /**
     * Reset time to 0 and stop the timer.
     */
    public void reset()
    {
        startTime = 0;
        tracking = false;
        timeElapsed = 0;
    }
    
    private long getTimeSinceStart()
    {
        if(tracking) {
            return System.currentTimeMillis() - startTime;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Get the time tracked, in milliseconds, since the mark.
     */
    public long getTimeTracked()
    {
        return timeElapsed + getTimeSinceStart();
    }
    
    /**
     * Reset the time tracked to the given time. The time tracked continues
     * to count up if the TimeTracker is started.
     */
    public void setTimeTracked(long newTime) 
    {
        if(tracking) {
            startTime = System.currentTimeMillis() - newTime;
            timeElapsed = 0;
        } else {
            timeElapsed = newTime;
        }
    }
}
