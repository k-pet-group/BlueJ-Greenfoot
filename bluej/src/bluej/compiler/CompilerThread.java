package bluej.compiler;

import bluej.Config;

/**
 ** @version $Id: CompilerThread.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** The compiler thread. BlueJ uses exactly one thread for compilation.
 ** Jobs are queued, and this thread processes tham one by one.  If there
 ** is no job, this thread just sleeps.
 **/

public class CompilerThread extends Thread
{
    Queue jobs;
	
    public CompilerThread()
    {
	super(title);
	jobs = new Queue();
    }
	
    public void run()
    {
	Job job;
		
	while(true) {
	    while((job = (Job)jobs.dequeue()) != null) {
		job.compile();
	    }
			
	    synchronized(this) {
		try {
		    wait();
		} catch(InterruptedException e) {
		}
	    }
	}
    }
	
    public synchronized void addJob(Job job)
    {
	jobs.enqueue(job);
	notify();
    }
	
    static String title = Config.getString("compiler.thread.title");
}
