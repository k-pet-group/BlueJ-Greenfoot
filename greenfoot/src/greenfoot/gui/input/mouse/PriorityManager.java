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
package greenfoot.gui.input.mouse;

import java.awt.event.MouseEvent;

/**
 * This class resolves the priorities of mouse events for the MousePollingManager.
 * <p>
 *
 * Priorities with highest priority first::
 * <ul>
 * <li> dragEnd </li>
 * <li> click </li>
 * <li> press </li>
 * <li> drag </li>
 * <li> move </li>
 * </ul>
 * 
 * If several of the same type of event happens, then the last one is used.
 * <p>
 * If two buttons are pressed at the same time, the behaviour is
 * undefined. Maybe we should define it so that button1 always have higher
 * priority than button2 and button2 always higher than button3. But not
 * necessarily documenting this to the user.
 * 
 * @author Poul Henriksen
 * 
 */
public class PriorityManager
{
    /**
     * Returns true if the new mouse event has higher or equal priority than the current.
     * @param newEvent The AWT mouse event code
     * @param currentData The existing mouse data to compare against
     * @return True if the new event is higher priority
     */
    public static boolean isHigherPriority(int newEvent, MouseEventData currentData)
    {
        int currentPriority = getPriority(currentData);
        int newPriority = getPriority(newEvent);
        return newPriority <= currentPriority;
    }
    /**
     * Priority 0 is highest.
     * @param event The AWT event code for the mouse event
     * @return The mapped priority, to enable a comparison.
     */
    private static int getPriority(int event)
    {
        if(event == MouseEvent.MOUSE_RELEASED) {
            return 0;
        }
        else if(event == MouseEvent.MOUSE_CLICKED) {
            return 1;
        }
        else if(event == MouseEvent.MOUSE_PRESSED) {
            return 2;
        }
        else if(event == MouseEvent.MOUSE_DRAGGED) {
            return 3;
        }
        else if(event == MouseEvent.MOUSE_MOVED) {
            return 4;
        }
        else {
            return Integer.MAX_VALUE;
        }
    }

    private static int getPriority(MouseEventData data)
    {
        if(data.isMouseDragEnded()) {
            return 0;
        }
        else if(data.isMouseClicked()) {
            return 1;
        }
        else if(data.isMousePressed()) {
            return 2;
        }
        else if(data.isMouseDragged()) {
            return 3;
        }
        else if(data.isMouseMoved()) {
            return 4;
        }
        else {
            return Integer.MAX_VALUE;
        }
    }
}
