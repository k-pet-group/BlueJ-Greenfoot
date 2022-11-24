/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.event;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.EventListener;

/**
 * Listener for simulation events.
 * 
 * <p>Events may be dispatched from the simulation thread and must be dealt with
 * quickly.
 * 
 * @author Poul Henriksen
 */
public interface SimulationListener
{
    /**
     * Simulation events which can occur off the simulation thread.
     */
    public static enum AsyncEvent
    {
        /** The simulation was paused */
        STOPPED,
        
        /** The simulation speed changed */
        CHANGED_SPEED,

        /**
         * The simulation was disabled and cannot be restarted
         * until a STOPPED event is received.
         */
        DISABLED;
    }

    /**
     * Simulation events which always occur on the simulation thread
     * (and where the listener is thus called on the simulation thread)
     */
    public static enum SyncEvent
    {
        /** The simulation started running */
        STARTED,
        
        /**
         * Execution of a new "Act" round has commenced.
         */
        NEW_ACT_ROUND,

        /**
         * Execution of an "Act" round has completed.
         */
        END_ACT_ROUND,

        /**
         * A task was queued to run on the simulation thread and is now being run.
         */
        QUEUED_TASK_BEGIN,

        /**
         * A task was queued to run on the simulation thread and has now finished.
         */
        QUEUED_TASK_END,

        /**
         * Entering Delay loop by the Simulation
         */
        DELAY_LOOP_ENTERED,

        /**
         * Delay loop is completed
         */
        DELAY_LOOP_COMPLETED;    
    }
    
    
    /**
     * The simulation state changed or a simulation event occurred. The simulation may have
     * stopped, started, changed enabled state, begun a new act round, etc.
     */
    @OnThread(Tag.Simulation)
    public void simulationChangedSync(SyncEvent e);

    @OnThread(Tag.Any)
    public void simulationChangedAsync(AsyncEvent e);
}
