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
import greenfoot.ActorVisitor;

/**
 * Checks collisions between GreenfooObjects.
 * 
 * @author Poul Henriksen
 *
 */
public class GOCollisionQuery implements CollisionQuery
{
    private Class<?> cls;
    private Actor compareObject;

    /**
     * Initialise.
     * 
     * @param cls   If not null, the compared object must be of this class. If null, the compared
     *              object's class is not considered.
     * @param actor Object to compare against other objects.
     */
    public void init(Class<?> cls, Actor actor)
    {
        this.cls = cls;
        this.compareObject = actor;
    }        
    
    /**
     * Checks if the other object collides with this object and if it is of the given class.
     */
    public boolean checkCollision(Actor other)
    {   
        if(cls != null && !cls.isInstance(other)) {
            return false;
        }
        
        if(compareObject == null) {
            return true;
        }
        else if(ActorVisitor.intersects(compareObject, other)) {
            return true;
        } 
        return false;
    }     
}