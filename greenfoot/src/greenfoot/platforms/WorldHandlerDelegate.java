/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2014  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.gui.DropTarget;
import greenfoot.gui.input.InputManager;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Interface to classes that contain specialized behaviour for the WorldHandler
 * depending on where and how the Greenfoot project is running.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Simulation)
public interface WorldHandlerDelegate
{    
    /**
     * A new world has been set as the active world.
     * @param oldWorld   The previously active world
     * @param newWorld   The new active world
     */
    void setWorld(World oldWorld, World newWorld);

    void setWorldHandler(WorldHandler handler);

    /**
     * Instantiate a new world and do any initialisation needed to activate that world.
     * 
     * @param className The fully qualified name of the world class to instantiate
     *                  if a specific class is wanted.  If null, use the most recently
     *                  instantiated world class.
     * @param runIfError A piece of code to run if there is an error during instantiation.
     *                   This is passed as a Runnable rather than handled by return because
     *                   we may hop thread to do the instantiation, so we cannot directly
     *                   return the result without blocking.
     */
    void instantiateNewWorld(String className, Runnable runIfError);

    InputManager getInputManager();

    void discardWorld(World world);
    
    /**
     * An actor was added into the world (by any means, possibly programmatically). Called with the world locked.
     */
    public void objectAddedToWorld(Actor actor);

    /**
     * Show a text prompt asking for input, with given prompt string
     */
    public String ask(String prompt);

    public default void initialisingWorld() {};

    /**
     * Repaint the world.
     * @param forcePaint Force paint (ignore any optimisations to not paint frames too often, etc)
     */
    void paint(boolean forcePaint);

    /**
     * The simulation had some user code which threw an exception
     * that was not caught by the user code.
     */
    void notifyStoppedWithError();

    /**
     * Set the listener for when a target is dropped.
     */
    void setDropTargetListener(DropTarget dropTarget);
}
