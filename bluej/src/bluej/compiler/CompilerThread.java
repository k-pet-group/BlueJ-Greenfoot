package bluej.compiler;

import bluej.Config;
import bluej.utility.Queue;

/**
 * The compiler thread. BlueJ uses exactly one thread for compilation. Jobs are
 * queued, and this thread processes tham one by one. If there is no job, this
 * thread just sleeps.
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 * @version $Id: CompilerThread.java 2812 2004-07-22 06:41:02Z davmac $
 */
class CompilerThread extends Thread
{
    private Queue jobs;
    private boolean busy = true;

    /**
     * Create a new compiler thread that holds its own job queue.
     */
    public CompilerThread()
    {
        super(Config.getString("compiler.thread.title"));
        jobs = new Queue();
    }

    /**
     * Start running this thread. The compiler thread will run infinitely in a
     * loop. It will compile jobs as long as there are any jobs pending, and
     * then wait for new jobs to be scheduled. New jobs are scheduled using the
     * addJob method.
     */
    public void run()
    {
        Job job;
        while (true) {
            synchronized (this) {
                while ((job = (Job) jobs.dequeue()) == null) {
                    busy = false;
                    notifyAll();
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {}
                }
            }

            job.compile();
        }
    }

    /**
     * Add a new job to this thread's job queue. The job will be processed by
     * this thread some tim ein the near future. This method returns
     * immediately.
     */
    public synchronized void addJob(Job job)
    {
        jobs.enqueue(job);
        busy = true;
        notifyAll();
    }

    public boolean isBusy()
    {
        return busy;
    }
}