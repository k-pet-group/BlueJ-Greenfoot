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
package greenfoot.gui;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.Point;

/**
 * Interface that components can use for accepting dropping of objects
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: DropTarget.java 6216 2009-03-30 13:41:07Z polle $
 */
public interface DropTarget
{
    /**
     * Tells this component to do whatever when a component is dragged along it.
     * 
     * The component is responsible for repainting itself.
     * 
     * @param o
     * @return true if the drag was processed, false otherwise
     */
    @OnThread(Tag.Simulation)
    public boolean drag(Object o, Point p);
}
