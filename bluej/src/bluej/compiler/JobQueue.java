package bluej.compiler;

import java.io.File;

import bluej.Config;
import bluej.utility.Debug;

/**
 * Reasonably generic interface between the BlueJ IDE and the Java
 * compiler.
 *
 * @author  Michael Cahill
 * @version $Id: JobQueue.java 2197 2003-10-02 04:12:34Z ajp $
 */
public class JobQueue
{
    private static JobQueue queue = null;

    public static synchronized JobQueue getJobQueue()
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

            compiler = new JavacCompilerInternal();

        } else if (compilertype.equals("javac")) {

            compiler = new JavacCompiler(
						Config.getJDKExecutablePath("bluej.compiler.executable","javac"));

        } else if (compilertype.equals("jikes")) {

            compiler = new JikesCompiler(
                        Config.getPropString("bluej.compiler.executable","jikes"));

        } else {
            Debug.message(Config.getString("compiler.invalidcompiler"));
        }

        thread = new CompilerThread();
        // Lower priority to improve GUI response time during compilation
        thread.setPriority(Thread.currentThread().getPriority() - 1);
        thread.start();
    }

    /**
     * Adds a job to the compile queue.
     */
    public void addJob(File[] sources, CompileObserver observer,
                       String classPath, File destDir)
    {
        thread.addJob(new Job(sources, compiler, observer,
                              classPath, destDir));
    }

    /**
     * Wait until the compiler job queue is empty, then return.
     */
    public void waitForEmptyQueue()
    {
        while (thread.isBusy()) {
            synchronized (thread) {
                try {
                    thread.wait();
                } catch (InterruptedException ex) {}
            }
        }
    }
}
