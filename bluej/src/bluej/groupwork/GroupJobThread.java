package bluej.groupwork;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Queue;

/**
 ** @version $Id: GroupJobThread.java 504 2000-05-24 04:44:14Z markus $
 ** @author Markus Ostman, with influences from CompilerThread in the bluej 
 ** compiler Package
 ** 
 **
 ** The GroupJob thread. This thread is used for processing groupwork jobs.
 ** Jobs are queued, and this thread processes them one by one to ensure 
 ** that the jobs are executed in a specific order.  If there
 ** is no job, this thread just sleeps.
 **/

public class GroupJobThread extends Thread
{
    Queue jobs;
    static String title = Config.getString("groupwork.thread.title");
	
    public GroupJobThread()
    {
	super(title);
	jobs = new Queue();
    }
	
    public void run()
    {
	GroupJob job;
		
	while(true) {
	    while((job = (GroupJob)jobs.dequeue()) != null) {
		job.process();
	    }
			
	    synchronized(this) {
		try {
		    wait();
		} catch(InterruptedException e) {
		}
	    }
	}
    }

    public synchronized void addJob(GroupJob job)
    {
	jobs.enqueue(job);
	notify();
    }
	
    public synchronized void clearQueue()
    {
        while(jobs.dequeue() != null) {}
    }
    
}
