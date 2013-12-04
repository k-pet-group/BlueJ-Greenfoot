/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.collision.ibsp.IBSPColChecker;

import java.awt.Graphics;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


/**
 * This class manages collision checkers. It doesn't do any collision checking
 * itself but optimises the collision checking by deciding how to delegate
 * collision checking to other collision checkers.
 * 
 * @author Poul Henriksen
 */
public class ColManager implements CollisionChecker
{

    /** Map from classes to objects that are not part of the collision checking (yet). */
    private Map<Class<? extends Actor>, LinkedList<Actor>> freeObjects = new HashMap<Class<? extends Actor>, LinkedList<Actor>>();
    
    /** Classes that are part of the collision checking. */
    private Set<Class<? extends Actor>> collisionClasses = new HashSet<Class<? extends Actor>>();
    
    /** The actual collision checker. */
    private CollisionChecker collisionChecker = new IBSPColChecker();

    /**
     * Ensures that objects of this class are in the collision checker
     * 
     */
    private void makeCollisionObjects(Class<? extends Actor> cls, boolean includeSubclasses)
    {
        if (cls == null) {
            //long start = System.nanoTime();
            Set<Entry<Class<? extends Actor>, LinkedList<Actor>>> entries = freeObjects.entrySet();
            for (Entry<Class<? extends Actor>, LinkedList<Actor>> entry : entries) {
                // TODO: bulk add could be faster if implemented in collision checker?
                for (Actor actor : entry.getValue()) {
                    collisionChecker.addObject(actor);
                }
                collisionClasses.add(entry.getKey());
            }
            //long end = System.nanoTime();

            //System.out.println("move all took seconds: " + (end - start) / 1000000000d);
            freeObjects.clear();
        }
        else if (collisionClasses.contains(cls)) {
        }
        else {
            List<? extends Actor> classSet = freeObjects.remove(cls);

            if( classSet != null) {
                collisionClasses.add(cls);
    
                // Add all the objects to the collision checker
                // TODO: bulk add could be faster if implemented in collision checker?
                for (Actor actor : classSet) {
                    collisionChecker.addObject(actor);
                }
            }
        }

        if (includeSubclasses) {
            // Clone it to avoid concurrent modification:
            Set<Entry<Class<? extends Actor>, LinkedList<Actor>>> entries = 
                    new HashSet<Entry<Class<? extends Actor>, LinkedList<Actor>>>(freeObjects.entrySet());
            // Run through all classes to see if any of them is a subclass.
            for (Entry<Class<? extends Actor>, LinkedList<Actor>> entry : entries) {
                if(cls.isAssignableFrom(entry.getKey())) {
                    makeCollisionObjects(entry.getKey(), false);
                }
            }
        }
    }

    /**
     * Ensure that objects of the actors class and all objects of 'cls' or a
     * subclass is part of the collision detection.
     * 
     */
    private <T extends Actor> void prepareForCollision(Actor actor, Class<T> cls)
    {
        makeCollisionObjects(actor.getClass(), false);
        makeCollisionObjects(cls, true);
    }

    public void addObject(Actor actor)
    {
        Class<? extends Actor> cls = actor.getClass();

        if (collisionClasses.contains(cls)) {
            collisionChecker.addObject(actor);
        }
        else {
            LinkedList<Actor> classSet = freeObjects.get(cls);
            if (classSet == null) {
                classSet = new LinkedList<Actor>();
                freeObjects.put(cls, classSet);
            }
            classSet.add(actor);
        }
    }

    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls)
    {
        prepareForCollision(actor, cls);
        return collisionChecker.getIntersectingObjects(actor, cls);
    }

    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance, boolean diag, Class<T> cls)
    {
        prepareForCollision(actor, cls);
        return collisionChecker.getNeighbours(actor, distance, diag, cls);
    }

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjects(Class<T> cls)
    {
        List<T> result = collisionChecker.getObjects(cls);

        Set<Entry<Class<? extends Actor>, LinkedList<Actor>>> entries = freeObjects.entrySet();
        for (Entry<Class<? extends Actor>, LinkedList<Actor>> entry : entries) {
            if (cls == null || cls.isAssignableFrom(entry.getKey())) {
                result.addAll((Collection<? extends T>) entry.getValue());
            }
        }
        return result;
    }

    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls)
    {
        makeCollisionObjects(cls, true);
        return collisionChecker.getObjectsAt(x, y, cls);
    }

    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls)
    {
        makeCollisionObjects(cls, true);
        return collisionChecker.getObjectsInDirection(x, y, angle, length, cls);
    }

    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r, Class<T> cls)
    {
        makeCollisionObjects(cls, true);
        return collisionChecker.getObjectsInRange(x, y, r, cls);
    }

    public List<Actor> getObjectsList()
    {
        return getObjects(null);
    }

    public <T extends Actor> T getOneIntersectingObject(Actor object, Class<T> cls)
    {
        prepareForCollision(object, cls);
        return collisionChecker.getOneIntersectingObject(object, cls);
    }

    public <T extends Actor> T getOneObjectAt(Actor object, int dx, int dy, Class<T> cls)
    {
        prepareForCollision(object, cls);
        return collisionChecker.getOneObjectAt(object, dx, dy, cls);
    }

    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        collisionChecker.initialize(width, height, cellSize, wrap);
    }

    public void paintDebug(Graphics g)
    {
        collisionChecker.paintDebug(g);
    }

    public void removeObject(Actor object)
    {
        LinkedList<Actor> classSet = freeObjects.get(object.getClass());
        if (classSet != null) {
            classSet.remove(object);
        }
        else {
            collisionChecker.removeObject(object);
        }
    }

    public void startSequence()
    {
        collisionChecker.startSequence();
    }

    public void updateObjectLocation(Actor object, int oldX, int oldY)
    {
        if (!freeObjects.containsKey(object.getClass())) {
            collisionChecker.updateObjectLocation(object, oldX, oldY);
        }
    }

    public void updateObjectSize(Actor object)
    {
        if (!freeObjects.containsKey(object.getClass())) {
            collisionChecker.updateObjectSize(object);
        }
    }
}