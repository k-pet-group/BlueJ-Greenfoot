/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009, 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.actions;

import greenfoot.GreenfootImage;
import greenfoot.core.ObjectDragProxy;
import greenfoot.core.WorldHandler;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * An action that creates an ObjectDragProxy when performed and initiates a drag
 * with that ObjectDragProxy.
 * 
 * @author Poul Henriksen
 * 
 */
public class DragProxyAction extends AbstractAction
{

    private GreenfootImage dragImage;
    private Action dropAction;

    public DragProxyAction(GreenfootImage dragImage, Action dropAction)
    {
        super((String)dropAction.getValue(Action.NAME));
        this.dragImage = dragImage;
        this.dropAction = dropAction;
    }

    public void actionPerformed(ActionEvent e)
    {
        ObjectDragProxy object = new ObjectDragProxy(dragImage, dropAction);
        WorldHandler.getInstance().getInputManager().objectCreated(object);
    }

}
