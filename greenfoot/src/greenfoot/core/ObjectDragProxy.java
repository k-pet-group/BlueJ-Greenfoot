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
package greenfoot.core;

import greenfoot.GreenfootImage;
import greenfoot.Actor;

import javax.swing.Action;

/**
 * This object is used when dragging greenfoot objects around. Because we do not
 * want objects ot be constructed until they are actually added into the world,
 * we need a temporary object to be dragged around , which represents the real
 * class that will be instantiated.
 * 
 * @author Poul Henriksen
 * 
 */
public class ObjectDragProxy extends Actor
{
    private Action realAction;

    public ObjectDragProxy(GreenfootImage dragImage, Action realAction)
    {
        setImage(dragImage);
        this.realAction = realAction;
    }

    /**
     * Create the real object
     * 
     */
    public void createRealObject()
    {
        realAction.actionPerformed(null);
    }

}
