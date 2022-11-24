/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2014,2018  Poul Henriksen and Michael Kolling 
 
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

import java.util.List;

/**
 * Test object that can easily be configured to having different sizes.
 * 
 * @author Poul Henriksen
 */
public class TestObject extends Actor
{
    /**
     * A test object with an image size of 7x7. Using 7x7 gives a size just less
     * than 10x10 if rotating 45 degrees. This makes it suitable to test a scenario
     * where the gridsize is 10x10 and we do not want objects to extent to more than
     * one cell.
     */
    public TestObject()
    {
        this(7, 7);
    }
    
    public TestObject(int width, int height) {
        GreenfootImage image = new GreenfootImage(width, height);
        setImage(image);
    }

    @SuppressWarnings("unchecked")
    public List getNeighboursP(int distance, boolean diagonal, Class cls)
    {
        return getNeighbours(distance, diagonal, cls);
    }

    @SuppressWarnings("unchecked")
    public List getObjectsInRangeP(int distance, Class cls)
    {
        return getObjectsInRange(distance, cls);
    }

    public boolean intersectsP(Actor other)
    {
        return intersects(other);
    }

    @SuppressWarnings("unchecked")
    public List getIntersectingObjectsP(Class cls)
    {
        return getIntersectingObjects(cls);
    }

    @SuppressWarnings("unchecked")
    public List getObjectsAtP(int dx, int dy, Class cls)
    {
        return getObjectsAtOffset(dx, dy, cls);
    }

    public Actor getOneIntersectingObjectP(Class<? extends Actor> cls)
    {
       return getOneIntersectingObject(cls);
    }

    public Actor getOneObjectAtP(int dx, int dy, Class<? extends Actor> cls)
    {
        return getOneObjectAtOffset(dx, dy, cls);
    }
    
    /**
     * Public version of "isTouching" method.
     */
    public boolean isTouchingP(Class<? extends Actor> cls)
    {
        return isTouching(cls);
    }
}
