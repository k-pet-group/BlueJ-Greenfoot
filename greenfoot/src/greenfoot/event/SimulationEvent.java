/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
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
/*
 * Created on Oct 7, 2003
 */
package greenfoot.event;

import java.util.EventObject;

/**
 * A simulation event
 * 
 * @author Poul Henriksen
 */
public class SimulationEvent extends EventObject
{
    /** The simulation started running */
    public final static int STARTED = 0;
    
    /** The simulation was paused */
    public final static int STOPPED = 1;
    
    /** The simulation speed changed */
    public final static int CHANGED_SPEED = 2;   
    
    /** 
     * The simulation was stopped and cannot be restarted
     * until a STOPPED event is received.
     */
    public final static int DISABLED = 3; 

    /** The simulation thread is paused because it hit a breakpoint,
     *  or the debugger has otherwise stopped its execution.
     *  
     *  Obviously, this event will not be processed in the
     *  Simulation thread.
     */
    public final static int DEBUGGER_PAUSED = 5;
    
    /** The opposite of DEBUGGER_PAUSED; the debugger has set
     * the Simulation going again.
     * 
     * Like DEBUGGER_PAUSED, this will not be processed in the Simulation
     * thread.
     */
    public final static int DEBUGGER_RESUMED = 6;
    

    private int type;

    public SimulationEvent(Object source, int type)
    {
        super(source);
        this.type = type;
    }

    public int getType()
    {
        return type;
    }
    
    @Override
    public String toString()
    {
        switch (type) {
            case STARTED:
                return "STARTED";
            case STOPPED:
                return "STOPPED";
            case CHANGED_SPEED:
                return "CHANGED_SPEED";
            case DISABLED:
                return "DISABLED";
            case DEBUGGER_PAUSED:
                return "DEBUGGER_PAUSED";
            case DEBUGGER_RESUMED:
                return "DEBUGGER_RESUMED";
        }
        return super.toString();
    }
}
