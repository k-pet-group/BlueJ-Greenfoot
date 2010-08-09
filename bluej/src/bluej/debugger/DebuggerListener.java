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

import java.util.EventListener;

/**
 * The listener for Debugger events.
 *
 * <p>Debugger events are processed in two stages
 * 
 * <p>First, all the events in a set are passed to examineDebuggerEvent.
 * This method should return true if it wants to set the thread going again,
 * or otherwise upset the interface updates.
 * 
 * <p>The result of all examineDebuggerEvent calls for an event set are ORed
 * together and later passed to processDebuggerEvent, which should act on the
 * boolean accordingly.  In particular, if the interface is given the value true,
 * it should not update.
 * 
 * <p>Calls to examineDebuggerEvent() and processDebuggerEvent() are synchronous,
 * that is:
 * <ul>
 * <li>A single set of events is handled at a time. Such a set will include events
 * affecting only a single thread.
 * <li>examineDebuggerEvent() will be called for each event in the set, in series.
 * <li>processDebuggerEvent() will then be called for each event in the set, in series.
 * <li>The processing of two separate event sets will occur in series.
 * </ul>
 */
public interface DebuggerListener extends EventListener
{
    /**
     * Examines the debugger event -- a precursor to a call to
     * processDebuggerEvent. This should return true if the event should not
     * cause the debugger UI to be updated.
     * 
     * <p>Related events (belonging to the same thread) will all be processed
     * before being passed on to processDebuggerEvent().
     * 
     * @see #processDebuggerEvent(DebuggerEvent, boolean)
     */
    boolean examineDebuggerEvent(DebuggerEvent e);
    
    /**
     * Called after examineDebuggerEvent to process an event.
     * 
     * @param skipUpdate   true if any listener requested that
     *                  the UI not be updated due to this event
     *                 (or another event in this event set).
     */
    void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate);
    
    /**
     * Called when the user instigates a halt of a thread in the debugger.
     * @return true if you do not want the thread display to be updated
     * (e.g. if you have set the thread running again),
     *   or false if you want things to be handled normally. 
     */
    boolean threadHalted(Debugger debugger, DebuggerThread thread);
}
