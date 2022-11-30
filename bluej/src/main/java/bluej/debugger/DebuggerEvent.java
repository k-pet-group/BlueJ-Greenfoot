/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2018  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.EventObject;


/**
 * Represents an event occurring in the BlueJ debugger implementation.
 */
@OnThread(Tag.Any)
public class DebuggerEvent extends EventObject
{
    public static interface BreakpointProperties
    {
        @OnThread(Tag.Any)
        public Object get(Object key);
    }

    /**
     * The readiness state of the debugger changed.
     */
    public final static int DEBUGGER_STATECHANGED = 1;
    /** 
     * A thread halted, due to a step event, or because it was programmatically halted
     * via the DebuggerThread interface.
     * 
     * We use the _UNKNOWN version when the exact cause is not known, and the more
     * specific _STEP_OVER or STEP_INTO when we know it is from a step event
     */
    public final static int THREAD_HALT_UNKNOWN = 2;
    public final static int THREAD_HALT_STEP_OVER = 3;
    public final static int THREAD_HALT_STEP_INTO = 4;
    /**
     * A thread halted due to hitting a breakpoint.
     */
    public final static int THREAD_BREAKPOINT = 5;
    /**
     * A thread resumed execution (due to this being requested via the DebuggerThread
     * interface).
     */
    public final static int THREAD_CONTINUE = 6;

    private int id;
    private DebuggerThread thr;
    private int oldState, newState;
    private BreakpointProperties props;

    public DebuggerEvent(Debugger source, int id, DebuggerThread thr, BreakpointProperties props)
    {
        super(source);
        this.id = id;
        this.thr = thr;
        this.props = props;
    }

    public DebuggerEvent(Object source, int id, int oldState, int newState)
    {
        super(source);
        this.id = id;
        this.oldState = oldState;
        this.newState = newState;
    }

    public int getID()
    {
        return id;
    }
    
    public boolean isHalt()
    {
        return id == THREAD_BREAKPOINT || id == THREAD_HALT_STEP_INTO || id == THREAD_HALT_STEP_OVER || id == THREAD_HALT_UNKNOWN;
    }

    /**
     * Get the thread involved in the event.  This is null for event DebuggerEvent.DEBUGGER_STATECHANGED
     */
    public DebuggerThread getThread()
    {
        return thr;
    }

    public int getOldState()
    {
        return oldState;
    }

    public int getNewState()
    {
        return newState;
    }

    /**
     * Get the properties associated with the breakpoint that was reached.
     * May return null.
     */
    public DebuggerEvent.BreakpointProperties getBreakpointProperties()
    {
        return props;
    }

    @Override
    public String toString()
    {
        return super.toString() + "[id=" + id + ",thr=" + thr + "]";
    }
    
    
}
