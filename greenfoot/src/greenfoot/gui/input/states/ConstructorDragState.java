/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;

/**
 * This state is active when the constructor of an Actor has been invoked via
 * the context-menu of the Actor. Works only while the simulation is stopped,
 * but there is another state {@link ConstructorDragWhileRunningState} which
 * handles the same case when the simulation is running.
 * 
 * @author Poul Henriksen
 */
public class ConstructorDragState extends State
{
    protected static ConstructorDragState instance;

    public static synchronized ConstructorDragState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    
    private ConstructorDragState(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized ConstructorDragState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        instance = new ConstructorDragState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event, Object obj)
    {
        super.switchToNextState(event, obj);
        switch(event) {
            case SHIFT_PRESSED :
                switchAndActivateState(QuickAddDragState.getInstance(), obj);
                break;
            case MOUSE_RELEASED :
                switchAndActivateState(IdleState.getInstance(), obj);
                break;
            case ESC_PRESSED :
                switchAndActivateState(IdleState.getInstance(), obj);
                break;
        }
    }
}