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
 * @version $Id: CompilerThread.java 2197 2003-10-02 04:12:34Z ajp $
 */
class CompilerThread extends Thread
{
    private Queue jobs;
    private boolean busy;
	
    /**
     * Create a new compiler thread that holds its own job queue.
     */
    public CompilerThread()
    {
        super(Config.getString("compiler.thread.title"));
        jobs = new Queue();
    }
	
    /**
     * Start running this thread. The compiler thread will run infinitely in a loop.
     * It will compile jobs as long as there are any jobs pending, and then wait
     * for new jobs to be scheduled. New jobs are scheduled using the addJob method.
     */
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

    /**
     * Add a new job to this thread's job queue. The job will be processed by
     * this thread some tim ein the near future. This method returns immediately.
     */
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
