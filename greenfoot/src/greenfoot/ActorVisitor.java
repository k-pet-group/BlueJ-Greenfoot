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
package greenfoot;

import greenfoot.collision.ibsp.Rect;
import greenfoot.platforms.ActorDelegate;
import greenfoot.util.Circle;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to Actor methods that are package protected. We need some
 * package-protected methods, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen 
 * @version $Id$
 */
public class ActorVisitor
{
    public static void setLocationInPixels(Actor actor, int dragBeginX, int dragBeginY) {
        actor.setLocationInPixels(dragBeginX, dragBeginY);
    }
    
   
    public static boolean contains(Actor actor, int dx, int dy)
    {
        return actor.contains(dx, dy);
    }

    public static boolean intersects(Actor actor, Actor other)
    {
        return actor.intersects(other);
    }
    
    public static int toPixel(Actor actor, int x) 
    {
        return actor.toPixel(x);
    }
    
    public static Rect getBoundingRect(Actor actor) 
    {
        return actor.getBoundingRect();
    }
    
    public static void setData(Actor actor, Object n)
    {
        actor.setData(n);
    }
    
    public static Object getData(Actor actor)
    {
        return actor.getData();
    }
    
    /**
     * Get the display image for an actor. This is the last image that was
     * set using setImage(). The returned image should not be modified.
     * 
     * @param actor  The actor whose display image to retrieve
     */
    public static GreenfootImage getDisplayImage(Actor actor)
    {
        return actor.getImage();
    }

    
    public static void setDelegate(ActorDelegate instance)
    {
        Actor.setDelegate(instance);
    }
    
    public static int getSequenceNumber(Actor actor)
    {
        return actor.getSequenceNumber();
    }
    
    /**
     * Get the sequence number of the given actor from the last paint
     * operation on the world. (Returns whatever was set using the
     * setLastPaintSeqNum method).
     */
    public static int getLastPaintSeqNum(Actor actor)
    {
        return actor.getLastPaintSeqNum();
    }
    
    /**
     * Set the sequence number of the given actor from the last paint
     * operation on the world.
     */
    public static void setLastPaintSeqNum(Actor actor, int num)
    {
        actor.setLastPaintSeqNum(num);
    }
}
