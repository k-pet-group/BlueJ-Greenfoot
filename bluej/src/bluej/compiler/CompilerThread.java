package bluej.compiler;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Queue;

/**
 * @version $Id: CompilerThread.java 1458 2002-10-23 12:06:40Z jckm $
 * @author Michael Cahill
 * @author Michael Kolling
 *
 * The compiler thread. BlueJ uses exactly one thread for compilation.
 * Jobs are queued, and this thread processes tham one by one.  If there
 * is no job, this thread just sleeps.
 */

public class CompilerThread extends Thread
{
    Queue jobs;
    boolean busy;
	
    public CompilerThread()
    {
        super(title);
        jobs = new Queue();
    }
	
    public void run()
    {
        Job job;

        while(true) {
            busy = true;
            while((job = (Job)jobs.dequeue()) != null) {
                job.compile();
            }
            busy = false;
		
            synchronized(this) {
                notifyAll();
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
        notifyAll();
    }
	
	public synchronized boolean isBusy()
	{
	    return (busy || !jobs.isEmpty());
	}
	
    static String title = Config.getString("compiler.thread.title");
}
