/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2018  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.util.GreenfootUtil;

import java.util.Collection;

import junit.framework.TestCase;

/**
 * Test for collisions between Actors
 * 
 * @author Poul Henriksen
 */
public class IntersectionTest extends TestCase
{
    private World world;

    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());
        Simulation.initialize();
    }
    
    @SuppressWarnings("unchecked")
    public void testIntersectingSingleCell()
    {
        world = WorldCreator.createWorld(10, 10, 10);
        TestObject o1 = new TestObject(10,10);
        world.addObject(o1, 2, 2);
        
        TestObject o2 = new TestObject(10,10);
        world.addObject(o2, 2, 2);
        
        assertTrue(o1.intersectsP(o2));
        assertTrue(o2.intersectsP(o1));
        
        Collection c = o1.getIntersectingObjectsP(TestObject.class);
        assertTrue(c.contains(o2));
        assertEquals(1, c.size());
        
        o2.setLocation(3,2);
        assertFalse(o1.intersectsP(o2));
        assertFalse(o2.intersectsP(o1));
    }
    
    @SuppressWarnings("unchecked")
    public void testIntersectingPixelLevelOdd()
    {
        world = WorldCreator.createWorld(70, 70, 1);

        WorldHandler.initialise();
        WorldHandler.getInstance().setWorld(world, false);
        
        TestObject o1 = new TestObject(7,7);
        world.addObject(o1, 0 ,0);
        
        TestObject o2 = new TestObject(7,7);
        world.addObject(o2, 6, 6);
        
        assertTrue(o1.intersectsP(o2));
        assertTrue(o2.intersectsP(o1));
        
        Collection c = o1.getIntersectingObjectsP(TestObject.class);
        assertTrue(c.contains(o2));
        assertEquals(1, c.size());
        
        o2.setLocation(7,7);
        assertFalse(o1.intersectsP(o2));
        assertFalse(o2.intersectsP(o1));
    }
    
    @SuppressWarnings("unchecked")
    public void testIntersectingPixelLevelEven()
    {
        world = WorldCreator.createWorld(80, 80, 1);
        TestObject o1 = new TestObject(8,8);
        world.addObject(o1, 0 ,0);
        
        TestObject o2 = new TestObject(8,8);
        world.addObject(o2, 7, 7);
        
        assertTrue(o1.intersectsP(o2));
        assertTrue(o2.intersectsP(o1));
        
        Collection c = o1.getIntersectingObjectsP(TestObject.class);
        assertTrue(c.contains(o2));
        assertEquals(1, c.size());
        
        o2.setLocation(8,8);
        assertFalse(o1.intersectsP(o2));
        assertFalse(o2.intersectsP(o1));
        
        o2.setLocation(9,9);
        assertFalse(o1.intersectsP(o2));
        assertFalse(o2.intersectsP(o1));
    }
    
    public void testRotationIntersection45()
    {
        world = WorldCreator.createWorld(200, 200, 1);
        TestObject o1 = new TestObject(50,50);
        world.addObject(o1, 0 ,0);
        TestObject o2 = new TestObject(50,50);
        world.addObject(o2, 55 ,0);

        // They do not touch...
        assertNull( o2.getOneIntersectingObjectP(TestObject.class));        

        // But if we rotate the second one 45 degress, its corner now overlaps
        // the first object:
        o2.setRotation(45);
        assertEquals(o1, o2.getOneIntersectingObjectP(TestObject.class));
        
        // Then we move the second object down a bit:
        o2.setLocation(55, 55);
        // Now the axis aligned bounding boxes will collide, so only a
        // intersection test that uses rotated bounding boxes will succeed here
        assertNull(o2.getOneIntersectingObjectP(TestObject.class));
        o1.setRotation(45);
        assertNull(o2.getOneIntersectingObjectP(TestObject.class));        
    }
    
    public void testRotationIntersection90()
    {
        world = WorldCreator.createWorld(200, 200, 1);
        TestObject o1 = new TestObject(100,10);
        world.addObject(o1, 0 ,0);
        TestObject o2 = new TestObject(10,10);
        world.addObject(o2, 0, 40);

        assertNull( o2.getOneIntersectingObjectP(TestObject.class));        
        o1.setRotation(90);
        assertEquals(o1, o2.getOneIntersectingObjectP(TestObject.class));      
    }
    
    /**
     * Tests intersection when both actors are rotated by the same multiple of 90 degrees
     */
    public void testRotationIntersectionBoth()
    {
        world = WorldCreator.createWorld(100, 100, 1);
        TestObject actor1 = new TestObject(3,3);
        TestObject actor2 = new TestObject(3,3);
        
        int xoffs = 2;
        int yoffs = 0;
        for (int rot = 0; rot < 360; rot += 90) {
            actor1.setRotation(rot);
            actor2.setRotation(rot);
            world.addObject(actor1, 50, 50);
            world.addObject(actor2, 50 + xoffs, 50 + yoffs);
            
            // Actors should just overlap:
            assertTrue(actor2.isTouchingP(TestObject.class));
            
            // rotate the offset position by 90 degrees:
            int pyoffs = yoffs;
            yoffs = xoffs;
            xoffs = -pyoffs;
            
            world.removeObject(actor1);
            world.removeObject(actor2);
        }
    }
    
    public void testIntersectionSmallObject()
    {
        world = WorldCreator.createWorld(200, 200, 1);
        TestObject o1 = new TestObject(100,100);
        world.addObject(o1, 55, 55);
        TestObject o2 = new TestObject(1,1);
        o2.setRotation(45);
        world.addObject(o2, 56, 56);
        
        assertNotNull(o2.getOneIntersectingObjectP(TestObject.class));
        
        // Place o2 inside the axis-aligned bounding rect of o1, but not intersecting:
        o1.setRotation(45);
        o2.setLocation(100, 100);
        assertNull(o2.getOneIntersectingObjectP(TestObject.class));
    }
}
