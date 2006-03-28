package greenfoot.collision;

import greenfoot.Actor;

import java.awt.Graphics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class CollisionProfiler
    implements CollisionChecker
{

    private static final int MAX_SEQ_COUNT = 1;
    private long objectAtTime;
    private CollisionChecker checker;
    private long removeObjectTime;
    private long updateObjectLocationTime;
    private long updateObjectSizeTime;
    private long getObjectsAtTime;
    private long getIntersectingObjectsTime;
    private long getObjectsInRangeTime;
    private long getNeighboursTime;
    private long getObjectsInDirectionTime;
    private long getObjectsTime;
    private int sequenceCount;
    private long getOneObjectAtTime;
    private long getOneIntersectingObjectTime;
private File f;
private PrintStream fileStream;
    public CollisionProfiler(CollisionChecker checker) {
        this.checker = checker;
    }
    
    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        checker.initialize(width, height, cellSize, wrap);
        File f = new File("/home/polle/profile.txt");
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

    public synchronized void addObject(Actor actor)
    {
        long t1 = System.nanoTime();
        checker.addObject(actor);
        long t2 = System.nanoTime();
        objectAtTime += t2 - t1;
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

    public List getObjectsAt(int x, int y, Class cls)
    {
        long t1 = System.nanoTime();
        List l  = checker.getObjectsAt(x, y, cls);
        long t2 = System.nanoTime();
        getObjectsAtTime += t2 - t1;
        return l;
    }

    public List getIntersectingObjects(Actor actor, Class cls)
    {

        long t1 = System.nanoTime();
        List l = checker.getIntersectingObjects(actor, cls);
        long t2 = System.nanoTime();
        getIntersectingObjectsTime += t2 - t1;
        return l;
    }

    public List getObjectsInRange(int x, int y, int r, Class cls)
    {
        long t1 = System.nanoTime();
        List l = checker.getObjectsInRange(x, y, r, cls);
        long t2 = System.nanoTime();
        getObjectsInRangeTime += t2 - t1;
        return l;
    }

    public List getNeighbours(int x, int y, int distance, boolean diag, Class cls)
    {
        long t1 = System.nanoTime();
        List l = checker.getNeighbours(x, y, distance, diag, cls);
        long t2 = System.nanoTime();
        getNeighboursTime += t2 - t1;
        return l;
    }

    public List getObjectsInDirection(int x, int y, int angle, int length, Class cls)
    {
        long t1 = System.nanoTime();
        List l = checker.getObjectsInDirection(x, y, angle, length, cls);
        long t2 = System.nanoTime();
        getObjectsInDirectionTime += t2 - t1;
        return l;
    }

    public synchronized List getObjects(Class cls)
    {
        long t1 = System.nanoTime();
        List l = checker.getObjects(cls);
        long t2 = System.nanoTime();
        getObjectsTime += t2 - t1;
        return l;
    }

    public void startSequence()
    {
        checker.startSequence();
        sequenceCount++;
        if(sequenceCount > MAX_SEQ_COUNT) {
            
            printTimes();
            
            objectAtTime = 0;
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
            
            sequenceCount = 0;
        }
        
        //Should write the file?
        fileStream.flush();
        
    }

    private void printTimes()
    {
        long totalTime = 0;
        totalTime += objectAtTime;
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
        
        int objects = checker.getObjects(null).size();
        long delay =  0;
        if(objects > 0) {
            delay =  totalTime / objects;
        }
        
      //  System.out.println("Delay pr. object (nanosec/obj , objects): " + delay  + "," + objects);
            fileStream.println(  totalTime +","+ objects);
    }

    public Actor getOneObjectAt(int dx, int dy, Class cls)
    {
        long t1 = System.nanoTime();
        Actor o = checker.getOneObjectAt(dx, dy, cls);
        long t2 = System.nanoTime();
        getOneObjectAtTime += t2 - t1;
        return o;
    }

    public Actor getOneIntersectingObject(Actor object, Class cls)
    {
        long t1 = System.nanoTime();
        Actor o = checker.getOneIntersectingObject(object, cls);
        long t2 = System.nanoTime();
        getOneIntersectingObjectTime += t2 - t1;
        return o;
    }

    public void paintDebug(Graphics g)
    {
        // checker.paintDebug(g);
    }

}
