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
package greenfoot.event;

import greenfoot.Actor;
import greenfoot.core.WorldHandler;

import java.awt.event.MouseEvent;

/**
 * Listens for new instances of GrenfootObjects
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ActorInstantiationListener.java,v 1.6 2004/11/18
 *          09:43:52 polle Exp $
 */
public class ActorInstantiationListener
{
    private WorldHandler worldHandler;
    
    public ActorInstantiationListener(WorldHandler worldHandler)
    {
        super();
        this.worldHandler = worldHandler;
    }

    /**
     * Notification for when an object has been created.
     * @param realObject  The newly instantiated object
     * @param e           The mouse event used to locate where to position the actor
     *                    (if the object is an actor)
     */
    public void localObjectCreated(Object realObject, MouseEvent e)
    {
        if(realObject instanceof Actor) {
            worldHandler.addObjectAtEvent((Actor) realObject, e);
            worldHandler.repaint();
        }
    }

}