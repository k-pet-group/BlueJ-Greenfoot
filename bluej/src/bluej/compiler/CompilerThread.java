package bluej.compiler;

import bluej.Config;
import bluej.utility.Queue;

/**
 * The compiler thread. BlueJ uses exactly one thread for compilation.
 * Jobs are queued, and this thread processes tham one by one.  If there
 * is no job, this thread just sleeps.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: CompilerThread.java 1765 2003-04-09 05:56:45Z ajp $
 */
public class CompilerThread extends Thread
{
    Queue jobs;
    boolean busy;
	
    public CompilerThread()
    {
        super(Config.getString("compiler.thread.title"));
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
                } catch(InterruptedException e) { }
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
}
