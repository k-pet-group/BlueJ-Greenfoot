/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.event;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.EventListener;

/**
 * Listener to receive notifcations when worlds are created and removed. All methods are called
 * on the simulation thread.
 * 
 * @author Poul Henriksen
 */
public interface WorldListener
    extends EventListener
{
    /**
     * Called when a new world is created and shown.
     */
    @OnThread(Tag.Simulation)
    public void worldCreated(WorldEvent e);

    /**
     * Called when a world is removed.
     */
    @OnThread(Tag.Simulation)
    public void worldRemoved(WorldEvent e);
}
