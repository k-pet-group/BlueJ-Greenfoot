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

import java.awt.Graphics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class CollisionProfiler implements CollisionChecker
{
    // Set this to true for output to console
    private static boolean to_console = true;
    
    // Set this to true for more complete output
    private static boolean verbose = true;
    
    // Set this to the number of sequences to collect times for,
    // before outputting the results and resetting the times
    private static final int MAX_SEQ_COUNT = 100;

    private CollisionChecker checker;
    
    private long addObjectTime;
    private long removeObjectTime;
    private long updateObjectLocationTime;
    private long updateObjectSizeTime;
    private long getObjectsAtTime;
    private long getIntersectingObjectsTime;
    private long getObjectsInRangeTime;
    private long getNeighboursTime;
    private long getObjectsInDirectionTime;
    private long getObjectsTime;
    private long getOneObjectAtTime;
    private long getOneIntersectingObjectTime;

    private int sequenceCount;
    private int sequences;
    
    private PrintStream fileStream;

    private int objectCount;
    
    public CollisionProfiler(CollisionChecker checker)
    {
        this.checker = checker;
    }
    
    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        checker.initialize(width, height, cellSize, wrap);
        if (to_console) {
            fileStream = System.out;
        }
        else {
            File f = new File(System.getProperty("user.home"));
            f = new File(f, "profile.txt");
            try {
                f.createNewFile();
            }
            catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            try {
                fileStream = new PrintStream(f);
            }
            catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public synchronized void addObject(Actor actor)
    {
        long t1 = System.nanoTime();
        checker.addObject(actor);
        long t2 = System.nanoTime();
        addObjectTime += t2 - t1;
    }

    public synchronized void removeObject(Actor object)
    {
        long t1 = System.nanoTime();
        checker.removeObject(object);
        long t2 = System.nanoTime();
        removeObjectTime += t2 - t1;
    }

    public  synchronized void updateObjectLocation(Actor object, int oldX, int oldY)
    {

        long t1 = System.nanoTime();
        checker.updateObjectLocation(object, oldX, oldY);
        long t2 = System.nanoTime();
        updateObjectLocationTime += t2 - t1;
        
    }

    public  synchronized void updateObjectSize(Actor object)
    {
        long t1 = System.nanoTime();
        checker.updateObjectSize(object);
        long t2 = System.nanoTime();
        updateObjectSizeTime += t2 - t1;
        
    }

    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls)
    {
        long t1 = System.nanoTime();
        List<T> l  = checker.getObjectsAt(x, y, cls);
        long t2 = System.nanoTime();
        getObjectsAtTime += t2 - t1;
        return l;
    }

    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls)
    {

        long t1 = System.nanoTime();
        List<T> l = checker.getIntersectingObjects(actor, cls);
        long t2 = System.nanoTime();
        getIntersectingObjectsTime += t2 - t1;
        return l;
    }

    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r, Class<T> cls)
    {
        long t1 = System.nanoTime();
        List<T> l = checker.getObjectsInRange(x, y, r, cls);
        long t2 = System.nanoTime();
        getObjectsInRangeTime += t2 - t1;
        return l;
    }

    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance, boolean diag, Class<T> cls)
    {
        long t1 = System.nanoTime();
        List<T> l = checker.getNeighbours(actor, distance, diag, cls);
        long t2 = System.nanoTime();
        getNeighboursTime += t2 - t1;
        return l;
    }

    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls)
    {
        long t1 = System.nanoTime();
        List<T> l = checker.getObjectsInDirection(x, y, angle, length, cls);
        long t2 = System.nanoTime();
        getObjectsInDirectionTime += t2 - t1;
        return l;
    }

    public synchronized <T extends Actor> List<T> getObjects(Class<T> cls)
    {
        long t1 = System.nanoTime();
        List<T> l = checker.getObjects(cls);
        long t2 = System.nanoTime();
        getObjectsTime += t2 - t1;
        return l;
    }
    
    public List<Actor> getObjectsList()
    {
        return checker.getObjectsList();
    }
    
    public void startSequence()
    {
        checker.startSequence();
        sequenceCount++;
        objectCount += checker.getObjects(null).size();
        if(sequenceCount > MAX_SEQ_COUNT) {
            
            printTimes();
            
            addObjectTime = 0;
            removeObjectTime = 0;
            updateObjectLocationTime = 0;
            updateObjectSizeTime = 0;
            getObjectsAtTime = 0;
            getIntersectingObjectsTime = 0;
            getObjectsInRangeTime = 0;
            getNeighboursTime = 0;
            getObjectsInDirectionTime = 0;
            getObjectsTime = 0;
            getOneObjectAtTime = 0;
            getOneIntersectingObjectTime = 0;
            
            objectCount = 0;
            
            sequenceCount = 0;
        }
        
        //Should write the file?
        fileStream.flush();
    }

    private void printTimes()
    {
        sequences++;
        if (verbose) {
            fileStream.println("Sequence # " + sequences);
        }
        
        long totalTime = 0;
        totalTime += addObjectTime;
        totalTime += removeObjectTime;
        totalTime += updateObjectLocationTime;
        totalTime += updateObjectSizeTime;
        totalTime += getObjectsAtTime;
        totalTime += getIntersectingObjectsTime ;
        totalTime += getObjectsInRangeTime;
        totalTime += getNeighboursTime;
        totalTime += getObjectsInDirectionTime;
        totalTime += getObjectsTime;
        totalTime += getOneObjectAtTime;
        totalTime += getOneIntersectingObjectTime;
        // totalTime += getObjectsListTime;
        
        if (verbose) {
            fileStream.println("addObjectTime                : " + addObjectTime);
            fileStream.println("removeObjectTime             : " + removeObjectTime);
            fileStream.println("updateObjectLocationTime     : " + updateObjectLocationTime);
            fileStream.println("updateObjectSizeTime         : " + updateObjectSizeTime);
            fileStream.println("getObjectsAtTime             : " + getObjectsAtTime);
            fileStream.println("getIntersectingObjectsTime   : " + getIntersectingObjectsTime);
            fileStream.println("getObjectsInRanageTime       : " + getObjectsInRangeTime);
            fileStream.println("getNeighboursTime            : " + getNeighboursTime);
            fileStream.println("getObjectsInDirectionTime    : " + getObjectsInDirectionTime);
            fileStream.println("getObjectsTime               : " + getObjectsTime);
            fileStream.println("getOneObjectAtTime           : " + getOneObjectAtTime);
            fileStream.println("getOneIntersectingObjectTime : " + getOneIntersectingObjectTime);
        }
        
        // System.out.println("Delay pr. object (nanosec/obj , objects): " + delay  + "," + objects);
        fileStream.println(  totalTime +","+ objectCount / sequenceCount);
        fileStream.println("========================");
    }

    public <T extends Actor> T getOneObjectAt(Actor actor, int dx, int dy, Class<T> cls)
    {
        long t1 = System.nanoTime();
        T o = checker.getOneObjectAt(actor, dx, dy, cls);
        long t2 = System.nanoTime();
        getOneObjectAtTime += t2 - t1;
        return o;
    }

    public <T extends Actor> T getOneIntersectingObject(Actor object, Class<T> cls)
    {
        long t1 = System.nanoTime();
        T o = checker.getOneIntersectingObject(object, cls);
        long t2 = System.nanoTime();
        getOneIntersectingObjectTime += t2 - t1;
        return o;
    }

    public void paintDebug(Graphics g)
    {
     //    checker.paintDebug(g);
    }
}
