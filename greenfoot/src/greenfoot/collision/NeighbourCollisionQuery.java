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
package greenfoot.collision;

import greenfoot.Actor;
import greenfoot.ActorVisitor;

/**
 * Checks if a Greenfoot object is within a specific neighbourhood.
 * 
 * <p>For the "in the neighbourhood" check, only the object's center point is considered,
 * not its graphical extent (size).
 *
 * @author Poul Henriksen
 */
public class NeighbourCollisionQuery implements CollisionQuery
{
    private int x;
    private int y;
    private int distance;
    private boolean diag;
    private Class<?> cls;
    
    /**
     * Set the NeighbourCollisionQuery parameters.
     * @param x       The X co-ordinate of the center point, in cells
     * @param y       The Y co-ordinate of the center point, in cells
     * @param distance  The distance, in cells, as a number of steps from the center point
     * @param diag    Whether the distance can include diagonal steps
     * @param cls     The class of actor to look for. If non-null, any actor not of this
     *                class will not be found by this query. 
     */
    public void init(int x, int y, int distance, boolean diag, Class<?> cls) 
    {
        this.x = x;
        this.y = y;
        this.distance = distance;
        this.diag = diag;
        this.cls = cls;
    }

    public boolean checkCollision(Actor actor)
    {
        if(cls != null && !cls.isInstance(actor)) {
            return false;
        }
        
        int actorX = ActorVisitor.getX(actor);
        int actorY = ActorVisitor.getY(actor);
        
        if(actorX == x && actorY == y) {
            return false;
        }       
        int ax = ActorVisitor.getX(actor);
        int ay = ActorVisitor.getY(actor);
        if(diag) {
            int x1 = x - distance;            
            int y1 = y - distance;            
            int x2 = x + distance;            
            int y2 = y + distance;
            return (ax >= x1 && ay >=y1 && ax <= x2 && ay <=y2);
        } else {
            int dx = Math.abs(ax - x);
            int dy = Math.abs(ay - y);
            return ((dx+dy) <= distance);            
        }
    }
}
