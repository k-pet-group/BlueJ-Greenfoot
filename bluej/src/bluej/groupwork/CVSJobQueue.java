package bluej.groupwork;

import java.lang.*;

import bluej.utility.Debug;
import bluej.Config;

/**
 ** @version $Id: CVSJobQueue.java 426 2000-04-14 01:11:12Z markus $
 ** @author Markus Ostman, but the idea is taken from JobQueue in bluej 
 ** compiler package.
 ** 
 ** I have a feeling that this class might be redundant. It is just here
 ** because it fits the copied structure. If it doesn't prove useful I 
 ** will remove it.
 **/

public class CVSJobQueue
{
    private static CVSJobQueue queue = null;

    public static CVSJobQueue getJobQueue() 
    {
	if(queue == null)
	    queue = new CVSJobQueue();
	return queue;
    }

    // ---- instance ----

    private GroupJobThread thread = null;
	
    /**
     * 
     */
    private CVSJobQueue()
    {
	thread = new GroupJobThread();
	// Lower priority to improve GUI response time during compilation
	//thread.setPriority(Thread.currentThread().getPriority() - 1);
	thread.start();
    }

    /**
     * Adds a job to the CVS request queue.
     */
    public void addJob(String name, Runnable runner, CVSJob.Monitor monitor)
    {
	thread.addJob(new CVSJob(name, runner, monitor));
    }

//     /** 
//      * Adds a job to the compile queue.
//      */
//     public void addJob(String sourcefile, CompileObserver observer,
// 			      String classpath, String destdir)
//     {
// 	thread.addJob(new Job(sourcefile, compiler, observer,
// 			      classpath, destdir));
//     }

}

