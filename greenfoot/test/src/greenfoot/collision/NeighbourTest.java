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
import greenfoot.core.WorldHandler;
import greenfoot.util.GreenfootUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

/**
 * 
 * Tests the method that gets the neighbours.
 * 
 * @author Poul Henriksen
 */
public class NeighbourTest extends TestCase
{
    private World world;

    @Override
    protected void setUp()
        throws Exception
    {
        GreenfootUtil.initialise(new TestUtilDelegate());        
    }

    @SuppressWarnings("unchecked")
    public void testNoWrapDiagonal()
    {
        int d = 2;
        world = WorldCreator.createWorld(10, 10, 10);

        WorldHandler.initialise();
        WorldHandler.getInstance().setWorld(world);
        
        int xStart = 9;
        int yStart = 9;
        TestObject me = new TestObject();
        world.addObject(me,xStart, yStart);
        Collection neighbours = createNeigboursWithDiagonal(xStart, yStart, d);
        Collection c = me.getNeighboursP(d, true, TestObject.class);
        assertEquals(8, c.size());
        assertTrue(c.containsAll(neighbours));

        world = WorldCreator.createWorld(10, 10, 10);
        xStart = 0;
        yStart = 0;
        world.addObject(me, xStart, yStart);
        neighbours = createNeigboursWithDiagonal(xStart, yStart, d);
        c = me.getNeighboursP(d, true, TestObject.class);
        assertEquals(8, c.size());
        assertTrue(c.containsAll(neighbours));
    }

    // =====================
    //  UTILITY METHODS
    // =====================

    @SuppressWarnings("unchecked")
    private Collection createNeigboursWithDiagonal(int xStart, int yStart, int d)
    {
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                TestObject actor = new TestObject();
                world.addObject(actor, x, y);
            }
        }

        Collection neighbours = new ArrayList();
        for (int x = xStart - d; x <= xStart + d; x++) {
            for (int y = yStart - d; y <= yStart + d; y++) {
                if (/*!world.isWrapped() &&*/ (x < 0 || y < 0 || x >= world.getWidth() || y >= world.getHeight())) {
                    continue;
                }
                Collection remove = world.getObjectsAt(x, y, TestObject.class);
                for (Iterator iter = remove.iterator(); iter.hasNext();) {
                    TestObject element = (TestObject) iter.next();
                    if (!(x == xStart && y == yStart)) {
                        neighbours.add(element);
                    }
                }
            }
        }
        return neighbours;
    }
}
