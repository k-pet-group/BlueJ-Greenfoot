/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.core.WorldHandler;
import greenfoot.gui.input.InputManager;

import java.awt.event.MouseEvent;


/**
 * Interface to classes that contain specialized behaviour for the WorldHandler
 * depending on where and how the Greenfoot project is running.
 * 
 * @author Poul Henriksen
 */
public interface WorldHandlerDelegate
{
    /**
     * Show the popup menu if the MouseEvent is a popup trigger.
     */
    boolean maybeShowPopup(MouseEvent e);

    void mouseClicked(MouseEvent e);

    void mouseMoved(MouseEvent e);
    
    /**
     * A new world has been set as the active world.
     * @param oldWorld   The previously active world
     * @param newWorld   The new active world
     */
    void setWorld(World oldWorld, World newWorld);

    void setWorldHandler(WorldHandler handler);
    
    void addActor(Actor actor, int x, int y); 
    
    /**
     * Instantiate a new world and do any initialisation needed to activate that world.
     */
    void instantiateNewWorld();

    InputManager getInputManager();

    void discardWorld(World world);
    
    /**
     * An actor was dragged to a new location. Called with the world locked.
     */
    public void actorDragged(Actor actor, int xCell, int yCell);
    
    /**
     * An actor was added into the world (by any means, possibly programmatically). Called with the world locked.
     */
    public void objectAddedToWorld(Actor actor);
}
