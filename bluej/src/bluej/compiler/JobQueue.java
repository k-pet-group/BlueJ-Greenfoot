package bluej.compiler;

import bluej.utility.Debug;

/**
 ** @version $Id: JobQueue.java 67 1999-05-10 02:42:49Z mik $
 ** @author Michael Cahill
 ** Reasonably generic interface between the BlueJ IDE and the Java
 ** compiler.
 **/

public class JobQueue
{
    private static JobQueue queue = null;

    public static JobQueue getJobQueue() 
    {
	if(queue == null)
	    queue = new JobQueue();
	return queue;
    }

    // ---- instance ----

    private CompilerThread thread = null;
    private Compiler compiler = new JavacCompiler(new ErrorStream());
	
    /**
     * 
     */
    private JobQueue()
    {
	thread = new CompilerThread();
	// Lower priority to improve GUI response time during compilation
	thread.setPriority(Thread.currentThread().getPriority() - 1);
	thread.start();
    }

    /**
     * Adds a job to the compile queue.
     * @pre init() is called
     * @see #init()
     */
    public void addJob(String[] sources, CompileObserver observer,
			      String classpath, String destdir)
    {
	thread.addJob(new Job(sources, compiler, observer,
			      classpath, destdir));
    }

    /** 
     * Adds a job to the compile queue.
     * Make sure init() is called once before.
     * @see #init()
     */
    public void addJob(String sourcefile, CompileObserver observer,
			      String classpath, String destdir)
    {
	thread.addJob(new Job(sourcefile, compiler, observer,
			      classpath, destdir));
    }
}
