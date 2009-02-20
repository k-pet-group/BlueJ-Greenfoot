/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
 * @version $Id: SimulationEvent.java 6170 2009-02-20 13:29:34Z polle $
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

    /** The simulation is about to start a new act round */
    public final static int NEW_ACT = 4;

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
    
    public String toString() {
        switch (type) {
            case STARTED:
                return "STARTED";
            case STOPPED:
                return "STOPPED";
            case CHANGED_SPEED:
                return "CHANGED_SPEED";
            case DISABLED:
                return "DISABLED";
            case NEW_ACT:
                return "NEW_ACT";                
        }
        return super.toString();
        
    }
}