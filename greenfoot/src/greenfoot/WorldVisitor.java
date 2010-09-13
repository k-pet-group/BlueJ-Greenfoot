/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010  Poul Henriksen and Michael Kolling 
 
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

import java.awt.Graphics;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to world methods that are package protected. We need some
 * package-protected methods in the world, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class WorldVisitor
{
    public static int getWidthInCells(World w)
    {
        return w.width;
    }
    
    public static int getHeightInCells(World w)
    {
        return w.height;
    }
    
    public static int getWidthInPixels(World w)
    {
        return w.getWidthInPixels();
    }

    public static int getHeightInPixels(World w)
    {
        return w.getHeightInPixels();
    }

    public static int getCellSize(World w)
    {
        return w.cellSize;
    }
    
    public static Collection<Actor> getObjectsAtPixel(World w, int x, int y)
    {
        return w.getObjectsAtPixel(x, y);
    }

    /**
     * Used to indicate the start of an animation sequence. For use in the collision checker.
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    public static void startSequence(World w)
    {
        w.startSequence();
    }

    public static void paintDebug(World world, Graphics g)
    {
        world.paintDebug(g);
    }
    
    /**
     * Inform the world that it has been repainted; must be called with the world
     * lock held.
     */
    public static void worldPainted(World world)
    {
        world.repainted();
    }
    
    /**
     * Convert a location in pixels into a cell location
     */
    public static int toCellFloor(World world, int x)
    {
        return world.toCellFloor(x);
    }
    
    /**
     * Returns the center of the cell. It should be rounded down with Math.floor() if the integer version is needed.
     * @param l Cell location.
     * @return Absolute location of the cell center in pixels.
     */
    public static  double getCellCenter(World w, int c)
    {
        return w.getCellCenter(c);
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on the world lock.
     */
    public static TreeActorSet getObjectsListInPaintOrder(World world)
    {
        return world.getObjectsListInPaintOrder(); 
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on itself or the World to avoid concurrent
     * modifications.
     */
    public static TreeActorSet getObjectsListInActOrder(World world)
    {
        return world.getObjectsListInActOrder(); 
    }

    /**
     * Get the background image for the world, but without initialising it if it is not yet created.
     * 
     * @return Background of the world or null if not create yet.
     */
    public static GreenfootImage getBackgroundImage(World world)
    {
        return world.getBackgroundNoInit();
    }

    public static ReentrantReadWriteLock getLock(World world) 
    {
        return world.lock;    
    }
    
    public static int getReadLockTimeout(World world) 
    {
        return World.READ_LOCK_TIMEOUT;    
    }
}
