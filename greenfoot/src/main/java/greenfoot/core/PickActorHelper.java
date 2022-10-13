/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2018  Poul Henriksen and Michael Kolling
 
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

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.WorldVisitor;

import java.awt.Point;

/**
 * A helper class which can be instantiated to fetch a reference to the
 * actor at a particular mouse location, where the user has left/right-clicked
 * in the interface.  If the location is outside the world, or there are no
 * actors at that point, the world is picked instead.
 */
public class PickActorHelper
{
    // Special fields examined by GreenfootDebugHandler.  Do not rename
    // without also renaming there:
    public Actor[] actorPicks;
    // Relevant only if actorPicks.length == 0 after a pick:
    public World worldPick;
    public int pickId;

    /**
     * Finds an actor at the given location.  If the location is invalid
     * or there are no actors there, the world is picked instead.
     *
     * @param sx       The x pixel coordinate in the world (will be an integer)
     * @param sy       The y pixel coordinate in the world (will be an integer)
     * @param spickId  The pick request ID
     * @param requestType  The request type.  If "drag", a drag on that actor will begin.
     */
    public PickActorHelper(String sx, String sy, String spickId, String requestType)
    {
        int x = Integer.parseInt(sx);
        int y = Integer.parseInt(sy);
        int pickId = Integer.parseInt(spickId);

        Simulation.getInstance().runLater(() -> {
            // The fields must be up to date and valid at the point we call picked():
            WorldHandler worldHandler = WorldHandler.getInstance();
            this.worldPick = worldHandler.getWorld();
            if (worldPick != null && x >= 0 && x < WorldVisitor.getWidthInPixels(worldPick)
                    && y >= 0 && y < WorldVisitor.getHeightInPixels(worldPick))
            {
                this.actorPicks = WorldVisitor.getObjectsAtPixel(this.worldPick, x, y).toArray(new Actor[0]);
            }
            else
            {
                this.actorPicks = new Actor[0];
            }
            this.pickId = pickId;
            if ("drag".equals(requestType))
            {
                // If there are any actors at that point, drag the topmost one:
                if (actorPicks.length > 0)
                {
                    // The top-most actor is actually the last in the list:
                    Actor topMost = actorPicks[actorPicks.length - 1];
                    worldHandler.startDrag(topMost, new Point(x, y), this.pickId);
                }
            }
            picked();
        });
    }

    /**
     * A special method which will have a breakpoint set by GreenfootDebugHandler.  Do
     * not remove/inline/rename without also editing that class.
     */
    public void picked()
    {
        // Used as a special breakpoint signifier so that JDI can be used to inspect the picks field
    }
}
