/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2015,2019  Michael Kolling and John Rosenberg 
 
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

/**
 * The listener for Debugger events.
 *
 * <p>Debugger events are processed in two stages.
 * 
 * <p>First, for certain event types (including breakpoint and step events),
 * all the events in a set are passed to examineDebuggerEvent. This method
 * should return true to prevent debugger user interface updates from reflecting
 * the thread stoppage; this might be done because the listener intends to resume
 * the thread execution immediately.
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
 * 
 * @see DebuggerEvent
 */
@OnThread(Tag.Any)
public interface DebuggerListener
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
    @OnThread(Tag.VMEventHandler)
    default boolean examineDebuggerEvent(DebuggerEvent e)
    {
        return false;
    }
    
    /**
     * Called after examineDebuggerEvent to process an event.
     * 
     * @param skipUpdate   true if any listener requested that
     *                  the UI not be updated due to this event
     *                 (or another event in this event set).
     */
    @OnThread(Tag.VMEventHandler)
    void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate);
}
