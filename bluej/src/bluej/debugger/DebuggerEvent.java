/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import java.util.EventObject;


/**
 * Represents an event occurring in the BlueJ debugger implementation.
 */
public class DebuggerEvent extends EventObject
{
    public static interface BreakpointProperties
    {
        public Object get(Object key);
    }

    /**
     * The readiness state of the debugger changed.
     */
    public final static int DEBUGGER_STATECHANGED = 1;
    /** 
     * A thread halted, due to a step event, or because it was programmatically halted
     * via the DebuggerThread interface.
     */
    public final static int THREAD_HALT = 3;
    /**
     * A thread halted due to hitting a breakpoint.
     */
    public final static int THREAD_BREAKPOINT = 4;
    /**
     * A thread resumed execution (due to this being requested via the DebuggerThread
     * interface).
     */
    public final static int THREAD_CONTINUE = 5;

    private int id;
    private DebuggerThread thr;
    private int oldState, newState;
    private BreakpointProperties props;

    public DebuggerEvent(Object source, int id)
    {
        super(source);

        this.id = id;
    }

    public DebuggerEvent(Debugger source, int id, DebuggerThread thr, BreakpointProperties props)
    {
        this(source, id);

        this.thr = thr;
        this.props = props;
    }

    public DebuggerEvent(Object source, int id, int oldState, int newState)
    {
        this(source, id);

        this.oldState = oldState;
        this.newState = newState;
    }

    public int getID()
    {
        return id;
    }

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
}
