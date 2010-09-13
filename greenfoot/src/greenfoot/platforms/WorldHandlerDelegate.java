/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.record.InteractionListener;

import java.awt.event.MouseEvent;


/**
 * Interface to classes that contain specialized behaviour for the WorldHandler
 * depending on where and how the greenfoot project is running.
 * 
 * @author Poul Henriksen
 */
public interface WorldHandlerDelegate
{
    /**
     * Show the popup menu if the mouseevent is a popup trigger.
     */
    boolean maybeShowPopup(MouseEvent e);

    void mouseClicked(MouseEvent e);

    void mouseMoved(MouseEvent e);
    
    void setWorld(World oldWorld, World newWorld);

    void setWorldHandler(WorldHandler handler);
    
    void addActor(Actor actor, int x, int y); 
    
    /**
     * Instantiate a new world and do any initialisation needed to activate that world.
     */
    void instantiateNewWorld();

    InputManager getInputManager();

    void discardWorld(World world);
    
    void initialisingWorld(World world);
    
    void simulationActive();

    InteractionListener getInteractionListener();
}
