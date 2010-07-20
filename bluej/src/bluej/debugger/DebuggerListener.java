/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
 * Debugger events are processed in two stages
 * 
 * First, all the events in a set are passed to examineDebuggerEvent.
 * This method should return true if it wants to set the thread going again,
 * or otherwise upset the interface updates.
 * 
 * The result of all examine.. calls are ORed together and later passed to
 * processDebuggerEvent, which should act on the boolean accordingly.  In particular,
 * if the interface is given the value true, it should not update. 
 */
public interface DebuggerListener extends EventListener
{
    /**
     * Examines the debugger event -- a precursor to a call to
     * processDebuggerEvent.  First, examineDebuggerEvent is called on all the listeners.
     * The results of all of them are ORed together,
     * and then processDebuggerEvent is called with this value.
     * 
     * So if you return true from examineDebuggerEvent, you
     * are guaranteed that the corresponding processDebuggerEvent
     * calls will have true as their parameter.
     */
    boolean examineDebuggerEvent(DebuggerEvent e);
    
    /**
     * Called after examineDebuggerEvent to process an event.
     * For an explanation of the skipUpdate parameter, see examineDebuggerEvent.
     */
    void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate);
    
    
    /**
     * Called when the user instigates a halt of a thread in the debugger.
     * @return true if you do not want the thread display to be updated (e.g. if you have set the thread running again),
     *   or false if you want things to be handled normally. 
     */
    boolean threadHalted(Debugger debugger, DebuggerThread thread);
}
