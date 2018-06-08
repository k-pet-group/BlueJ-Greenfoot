/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2016  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.GreenfootImage;
import greenfoot.TestObject;
import greenfoot.TestUtilDelegate;
import greenfoot.World;
import greenfoot.WorldCreator;
import greenfoot.core.Simulation;
import greenfoot.util.GreenfootUtil;
import junit.framework.TestCase;

public class GetAtTest extends TestCase
{
    private World world;
    
    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());
        Simulation.initialize();
    }
    
    public void testPixelOdd()
    {
        world = WorldCreator.createWorld(100, 100, 1);

        TestObject actor1 = new TestObject(21, 21);
        world.addObject(actor1, 50 , 50);

        List<TestObject> result = world.getObjectsAt(50, 50, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(60, 60, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(40, 40, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(39, 39, TestObject.class);
        assertFalse(result.contains(actor1));

        result = world.getObjectsAt(61, 61, TestObject.class);
        assertFalse(result.contains(actor1));

    }

    public void testPixelEven()
    {
        world = WorldCreator.createWorld(100, 100, 1);

        TestObject actor1 = new TestObject(20, 20);
        world.addObject(actor1, 50, 50);

        List<TestObject> result = world.getObjectsAt(50, 50, TestObject.class);
        assertTrue(result.contains(actor1));
        
        result = world.getObjectsAt(59, 59, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(40, 40, TestObject.class);
        assertTrue(result.contains(actor1));

        result = world.getObjectsAt(39, 39, TestObject.class);
        assertFalse(result.contains(actor1));

        result = world.getObjectsAt(60, 60, TestObject.class);
        assertFalse(result.contains(actor1));

    }
    
    /**
     * Test that collision checking works when an actor is rotated.
     */
    public void testRotation()
    {
        world = WorldCreator.createWorld(100, 100, 1);

        // add object spanning (30, 40)-(69, 59)
        TestObject actor1 = new TestObject(40, 20);
        world.addObject(actor1, 50, 50);
        List<TestObject> result = world.getObjectsAt(30, 40, TestObject.class);
        assertTrue(result.contains(actor1));
       
        result = world.getObjectsAt(69, 59, TestObject.class);
        assertTrue(result.contains(actor1));

        //Now, rotate it 90 degrees so it spans (40, 30)-(59, 69)
        actor1.setRotation(90);
        result = world.getObjectsAt(42, 32, TestObject.class);
        assertTrue(result.contains(actor1));
        result = world.getObjectsAt(57, 67, TestObject.class);
        assertTrue(result.contains(actor1));
        
        // TODO also try negative degress and odd degrees like 55 or something.
        // And some values outside the rotation
    }
    
    /** 
     * Test that the collision checker can handle rotated actors. 
     */
    public void testRotation2() 
    {
        world = WorldCreator.createWorld(10, 10, 50);
        // Test a second object forced to be on the bounds of the areas in the IBSPColChecker

        TestObject actor1 = new TestObject(50, 50);
        world.addObject(actor1, 0, 0);
        TestObject actor2 = new TestObject(50, 50);
        world.addObject(actor2, 5, 0);

        actor2.setRotation(45);
        
        List<TestObject> result = world.getObjectsAt(5, 0, TestObject.class);
        assertTrue(result.contains(actor2));

        //After the rotation, it should not overlap surrounding cells.
        
        result = world.getObjectsAt(6, 0, TestObject.class);
        assertFalse(result.contains(actor2));
        result = world.getObjectsAt(5, 1, TestObject.class);
        assertFalse(result.contains(actor2));
        result = world.getObjectsAt(4, 0, TestObject.class);
        assertFalse(result.contains(actor2));
    }
    
    /** 
     * Test that the collision checker can handle rotated actors. 
     */
    public void testRotation3() 
    {
        // This test currently fails, but I'm not convinced it *should* pass. Do we really want rotated
        // objects in a cell to overlap other cells? Does "getObjectsAt" really return all objects
        // overlapping any part of the cell? - DM
        
        world = WorldCreator.createWorld(10, 10, 50);
        // Test a second object forced to be on the bounds of the areas in the IBSPColChecker

        TestObject actor1 = new TestObject(50, 50);
        world.addObject(actor1, 0, 0);
        TestObject actor2 = new TestObject(50, 50);
        actor2.setRotation(45);
        world.addObject(actor2, 5, 0);
        
        List<TestObject> result = world.getObjectsAt(5, 0, TestObject.class);
        assertTrue(result.contains(actor2));

        //After the rotation, it should now overlap surrounding cells.
        
        result = world.getObjectsAt(6, 0, TestObject.class);
        assertFalse(result.contains(actor2));
        result = world.getObjectsAt(5, 1, TestObject.class);
        assertFalse(result.contains(actor2));
        result = world.getObjectsAt(4, 0, TestObject.class);
        assertFalse(result.contains(actor2));
    }
    
    
    public void testBigCells() 
    {
        world = WorldCreator.createWorld(10, 10, 50);

        TestObject actor1 = new TestObject(50, 50);
        world.addObject(actor1, 1, 1);

        List<TestObject> result = world.getObjectsAt(1, 1, TestObject.class);
        assertTrue(result.contains(actor1));
        result = world.getObjectsAt(0, 0, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(0, 1, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(1, 0, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(2, 2, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(1, 2, TestObject.class);
        assertFalse(result.contains(actor1));
        result = world.getObjectsAt(2, 1, TestObject.class);
        assertFalse(result.contains(actor1));  
    }
    
    public void testMovement()
    {
        world = WorldCreator.createWorld(10, 10, 50);
        TestObject actor1 = new TestObject(8,8);
        TestObject actor2 = new TestObject(8,8);
        TestObject actor3 = new TestObject(8,8);
        TestObject actor4 = new TestObject(8,8);
        world.addObject(actor1, 0, 0);
        world.addObject(actor2, 9, 0);
        world.addObject(actor3, 0, 9);
        world.addObject(actor4, 9, 9);
        assertSame(actor1.getOneObjectAtP(9, 0, TestObject.class), actor2);
        assertSame(actor1.getOneObjectAtP(0, 9, TestObject.class), actor3);
        assertSame(actor1.getOneObjectAtP(9, 9, TestObject.class), actor4);
        assertSame(actor2.getOneObjectAtP(-9, 0, TestObject.class), actor1);
        
        actor1.setLocation(1, 1);
        actor4.setLocation(8, 8);
        assertSame(actor1.getOneObjectAtP(8, -1, TestObject.class), actor2);
        assertSame(actor1.getOneObjectAtP(-1, 8, TestObject.class), actor3);
        assertSame(actor1.getOneObjectAtP(7, 7, TestObject.class), actor4);
        assertSame(actor2.getOneObjectAtP(-8, 1, TestObject.class), actor1);
        world.removeObjects(world.getObjects(null));
        
        world.addObject(actor1, 0, 0);
        world.addObject(actor2, 9, 0);
        world.addObject(actor3, 0, 9);
        world.addObject(actor4, 9, 9);
        assertSame(actor1.getOneObjectAtP(9, 0, TestObject.class), actor2);
        assertSame(actor1.getOneObjectAtP(0, 9, TestObject.class), actor3);
        assertSame(actor1.getOneObjectAtP(9, 9, TestObject.class), actor4);
        assertSame(actor2.getOneObjectAtP(-9, 0, TestObject.class), actor1);
    }
    
    public void testResize()
    {
        // (Fails in Greenfoot 3.0.2.)
        class BadLeaf extends TestObject {
            public BadLeaf()
            {
                super(512, 433);
            }
            
            @Override
            protected void addedToWorld(World world)
            {
                GreenfootImage myImage = getImage();
                myImage.scale(60, 60);
                setImage(new GreenfootImage(60, 60));
                getOneObjectAtOffset(0, 0, TestObject.class);
            }
        };
        
        world = WorldCreator.createWorld(10, 10, 60);
        TestObject wombat = new TestObject(58, 45);
        world.addObject(wombat, 4, 7);
        
        world.addObject(new BadLeaf(), 3, 5);
        world.addObject(new BadLeaf(), 2, 9);
        
        assertNull(wombat.getOneObjectAtP(1, 0, TestObject.class));
    }
}
