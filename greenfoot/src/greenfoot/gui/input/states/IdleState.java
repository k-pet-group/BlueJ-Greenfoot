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
package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;

/**
 * This is the default state, which is active when the simulation is stopped and
 * nothing else is happening (no dragging etc.)
 * 
 * @author Poul Henriksen
 */
public class IdleState extends State
{
    protected static IdleState instance;

    public static synchronized IdleState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    
    private IdleState(InputManager inputManager, TriggeredKeyListener keyListener, TriggeredMouseListener mouseListener,
            TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized IdleState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        instance = new IdleState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event, Object obj)
    {
        super.switchToNextState(event, obj);
        switch(event) {
            case CONSTRUCTOR_INVOKED :
                switchAndActivateState(ConstructorDragState.getInstance(), obj);
                break;
            case OBJECT_MOVED :
                switchAndActivateState(MoveState.getInstance(), obj);
                break;
            case SIMULATION_STARTED :
                switchAndActivateState(RunningState.getInstance(), obj);
                break;
            case MOUSE_PRESSED :
                switchAndActivateState(MoveState.getInstance(), obj);
                break;
            case SHIFT_PRESSED :
                switchAndActivateState(QuickAddDragState.getInstance(), obj);
                break;
        }
    }

};
