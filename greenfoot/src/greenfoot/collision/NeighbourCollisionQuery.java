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
package greenfoot.collision;

import greenfoot.Actor;

/**
 *  Checks if a greenfoot object is within a specific neighbourhood.
 *
 * @author Poul Henriksen
 */
public class NeighbourCollisionQuery implements CollisionQuery{

    private int x;
    private int y;
    private int distance;
    private boolean diag;
    private Class cls;
    
    public void init(int x, int y, int distance, boolean diag, Class cls) 
    {
        if(distance < 0) {
            throw new IllegalArgumentException("Distance must not be less than 0. It was: " + distance);
        }
        this.x = x;
        this.y = y;
        this.distance = distance;
        this.diag = diag;
        this.cls = cls;
    }

    public boolean checkCollision(Actor actor) {
        if(cls != null && !cls.isInstance(actor)) {
            return false;
        }
        if(actor.getX() == x && actor.getY() == y) {
            return false;
        }       
        if(diag) {
            int x1 = x - distance;            
            int y1 = y - distance;            
            int x2 = x + distance;            
            int y2 = y + distance;
            return (actor.getX() >= x1 && actor.getY() >=y1 && actor.getX() <= x2 && actor.getY() <=y2);
        } else {
            int dx = Math.abs(actor.getX() - x);
            int dy = Math.abs(actor.getY() - y);
            return ((dx+dy) <= distance);            
        }
    }

}
