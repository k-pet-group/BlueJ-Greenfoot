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

import java.util.List;

import junit.framework.TestCase;
import greenfoot.World;
import greenfoot.TestObject;
import greenfoot.WorldCreator;

/**
 * Common tests for the Collision API in greenfoot.
 * 
 * @author Poul Henriksen
 */
public class CommonTest extends TestCase
{
    private World world;
    private TestObject obj1;
    
    protected void setUp()        
    {
        world = WorldCreator.createWorld(10,10,10);
        obj1 = new TestObject(10,10);
        world.addObject(obj1, 0, 0);
    }

    /**
     * Test that all the collision methods can take null as the parameter for
     * the class type.
     */
    public void testNullClass()
    {
        boolean gotException = false;
        try {
            world.getObjectsAt(0, 0, null);

            obj1.getIntersectingObjectsP(null);

            obj1.getNeighboursP(1, true, null);
            obj1.getNeighboursP(1, false, null);

            obj1.getObjectsAtP(5, 5, null);

            obj1.getObjectsInRangeP(1, null);

            obj1.getOneIntersectingObjectP(null);

            obj1.getOneObjectAtP(0, 0, null);
        }
        catch (NullPointerException npe) {
            gotException = true;
            npe.printStackTrace();
        }
        assertFalse(gotException);
    }

    /**
     * test if the object itself is included in results.
     */
    @SuppressWarnings("unchecked")
    public void testSelfInclusion()
    {
        world.getObjectsAt(0, 0, TestObject.class);

        List l = obj1.getIntersectingObjectsP(TestObject.class);
        assertFalse(l.contains(obj1));

        l = obj1.getNeighboursP(1, true, TestObject.class);
        assertFalse(l.contains(obj1));

        l = obj1.getNeighboursP(1, false, TestObject.class);
        assertFalse(l.contains(obj1));

        l = obj1.getObjectsAtP(5, 5, TestObject.class);
        assertFalse(l.contains(obj1));

        // obj1.getObjectsInDirectionP(0, 1, TestObject.class);

        l = obj1.getObjectsInRangeP(1, TestObject.class);
        assertFalse(l.contains(obj1));

        Object o = obj1.getOneIntersectingObjectP(TestObject.class);
        assertNotSame(obj1, o);

        o  = obj1.getOneObjectAtP(0, 0, TestObject.class);
        assertNotSame(obj1, o);
    }
    
}
