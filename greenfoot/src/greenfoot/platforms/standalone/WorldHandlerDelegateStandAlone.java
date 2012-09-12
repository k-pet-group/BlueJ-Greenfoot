/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.standalone;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.core.WorldHandler;
import greenfoot.export.GreenfootScenarioViewer;
import greenfoot.gui.input.InputManager;
import greenfoot.platforms.WorldHandlerDelegate;

import java.awt.event.MouseEvent;


/**
 * Implementation for running scenarios in a standalone application or applet.
 * 
 * @author Poul Henriksen
 */
public class WorldHandlerDelegateStandAlone implements WorldHandlerDelegate
{    
    private WorldHandler worldHandler;
    private GreenfootScenarioViewer viewer;
    private boolean lockScenario;
    
    public WorldHandlerDelegateStandAlone (GreenfootScenarioViewer viewer, boolean lockScenario) 
    {
        this.viewer = viewer;
        this.lockScenario = lockScenario;
    }
    
    public boolean maybeShowPopup(MouseEvent e)
    {
        // Not used in standalone
        return false;
    }

    public void mouseClicked(MouseEvent e)
    {
        // Not used in standalone
    }
    
    public void mouseMoved(MouseEvent e)
    {
        // Not used in standalone
    }

    @Override
    public void setWorld(final World oldWorld, final World newWorld)
    {
        // Not needed
    }

    public void setWorldHandler(WorldHandler handler)
    {
        this.worldHandler = handler;
    }

    public void instantiateNewWorld()
    {
        WorldHandler.getInstance().clearWorldSet();
        World newWorld = viewer.instantiateNewWorld();
        if (! WorldHandler.getInstance().checkWorldSet()) {
            WorldHandler.getInstance().setWorld(newWorld);
        }
    }

    public InputManager getInputManager()
    {
        InputManager inputManager = new InputManager();
        inputManager.setDragListeners(null, null, null);
        if (lockScenario) {
            inputManager.setIdleListeners(null, null, null);
            inputManager.setMoveListeners(null, null, null);
        }
        else {
            inputManager.setIdleListeners(worldHandler, worldHandler, worldHandler);
            inputManager.setMoveListeners(worldHandler, worldHandler, worldHandler);
        }
        return inputManager;
    }

    public void discardWorld(World world)
    {
        // Nothing special to do.    
    }

    public void addActor(Actor actor, int x, int y)
    {
        // Nothing to be done
    }

    @Override
    public void actorDragged(Actor actor, int xCell, int yCell)
    {
    }
    
    @Override
    public void objectAddedToWorld(Actor actor)
    {
    }
}
