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

import greenfoot.TestUtilDelegate;
import greenfoot.World;
import greenfoot.TestObject;
import greenfoot.WorldCreator;
import greenfoot.util.GreenfootUtil;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * Tests the World's and Actor's getObjectsInRange method.
 * 
 * @author Poul Henriksen
 */
public class InRangeTest extends TestCase
{
    private World world;
    
    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());        
    }

    @SuppressWarnings("unchecked")
    public void testNoWrap()
    {
        world = WorldCreator.createWorld(10, 10, 10);

        TestObject actor1 = new TestObject(20, 20);
        world.addObject(actor1, 2, 2);

        TestObject actor2 = new TestObject(10, 10);
        world.addObject(actor2, 2, 4);
        
        Collection inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor1));

        actor2.setLocation(2, 5);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor1));

        actor2.setLocation(2, 6);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertFalse(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(3, TestObject.class);
        assertFalse(inRange.contains(actor1));

        actor2.setLocation(4, 4);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(3, TestObject.class);
        assertTrue(inRange.contains(actor1));

        actor2.setLocation(5, 5);
        inRange = actor1.getObjectsInRangeP(3, TestObject.class);
        assertFalse(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(3, TestObject.class);
        assertFalse(inRange.contains(actor1));

        actor2.setLocation(5, 5);
        inRange = actor1.getObjectsInRangeP(10000, TestObject.class);
        assertTrue(inRange.contains(actor2));
        inRange = actor2.getObjectsInRangeP(10000, TestObject.class);
        assertTrue(inRange.contains(actor1));
    }
}
