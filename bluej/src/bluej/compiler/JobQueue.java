package bluej.compiler;

import bluej.utility.Debug;
import bluej.Config;

/**
 ** @version $Id: JobQueue.java 98 1999-05-31 06:25:17Z ajp $
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
    private Compiler compiler = null;
	
    /**
     * 
     */
    private JobQueue()
    {
	// determine which compiler we should be using

	String compilertype = Config.getPropString("bluej.compiler.type");

	if (compilertype.equals("internal")) {

		System.out.println("internal not implemented yet");

	} else if (compilertype.equals("javac")) {

		compiler = new JavacCompiler(
				Config.getPropString("bluej.compiler.executable","javac"));

	} else if (compilertype.equals("jikes")) {

		compiler = new JikesCompiler(
				Config.getPropString("bluej.compiler.executable","jikes"));

	}

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
