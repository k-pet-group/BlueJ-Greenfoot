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
package greenfoot.gui.input.mouse;

import greenfoot.Actor;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Interface for locating actors and coordinates in the world.
 * 
 * @author Poul Henriksen
 */
public interface WorldLocator
{
    /**
     * Gets the top most actor at the given location.
     * @param worldPixelPositionX The X pixel position in the world (not cell position)
     * @param worldPixelPositionY The Y pixel position in the world (not cell position)
     * @return The top most actor, or null if no actor.
     */
    @OnThread(Tag.Simulation)
    public Actor getTopMostActorAt(int worldPixelPositionX, int worldPixelPositionY);

    /**
     * Translates the coordinates from the given source component into some other coordinate system.
     * @param worldPixelPositionX The X pixel position in the world (not cell position)
     * @return The new x-coordinate
     */
    public int getTranslatedX(int worldPixelPositionX);
    

    /**
     * Translates the coordinates from the given source component into some other coordinate system.
     * @param worldPixelPositionY The Y pixel position in the world (not cell position)
     * @return The new y-coordinate
     */
    public int getTranslatedY(int worldPixelPositionY);

}
