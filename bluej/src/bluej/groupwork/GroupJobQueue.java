package bluej.groupwork;

import java.lang.*;

import bluej.utility.Debug;
import bluej.Config;

/**
 ** @version $Id: GroupJobQueue.java 604 2000-06-29 06:41:26Z markus $
 ** @author Markus Ostman, but the idea is taken from JobQueue in bluej 
 ** compiler package.
 ** 
 ** I have a feeling that this class might be redundant. It is just here
 ** because it fits the copied structure. If it doesn't prove useful I 
 ** might remove it.
 **/

public class GroupJobQueue
{
    private static GroupJobQueue queue = null;

    public static GroupJobQueue getJobQueue() 
    {
	if(queue == null)
	    queue = new GroupJobQueue();
	return queue;
    }

    // ---- instance ----

    private GroupJobThread thread = null;
	
    /**
     * 
     */
    private GroupJobQueue()
    {
	thread = new GroupJobThread();
	thread.start();
    }

    /**
     * Adds a job to the CVS request queue.
     */
    public void addJob(String name, Runnable runner, GroupJob.Monitor monitor)
    {
	thread.addJob(new GroupJob(name, runner, monitor));
    }
    
    /**
     * Clears the job queue from waiting jobs and notifies threads that
     * might be waiting for jobs to finish.
     * This can be necessary if we have several jobs depending on the 
     * success of previous jobs. If one job then is not succesfull, 
     * that job should be responsible of clearing the queue.
     * Great care should be taken upon calling this method. It is 
     * recommendable that it should only be called by the job responsible
     * for the error. It is not clear what will happen othervise. 
     */
    public void clearQueue()
    {
        //before we clear we wait for everything to stabilize
        synchronized(this) {
            try {
                wait(500);
            }catch(InterruptedException e) {}
        }
        //If someone is waiting for a job in the queue, notify them. 
        Sync.s.callNotify(false);
	thread.clearQueue();
    }
}

