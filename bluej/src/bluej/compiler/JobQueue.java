package bluej.compiler;

import java.io.File;

import bluej.Config;
import bluej.utility.Debug;

/**
 * Reasonably generic interface between the BlueJ IDE and the Java compiler.
 * 
 * @author Michael Cahill
 * @version $Id: JobQueue.java 2812 2004-07-22 06:41:02Z davmac $
 */
public class JobQueue
{
    private static JobQueue queue = null;

    public static synchronized JobQueue getJobQueue()
    {
        if (queue == null)
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

        }
        else if (compilertype.equals("javac")) {

            compiler = new JavacCompiler(Config.getJDKExecutablePath("bluej.compiler.executable", "javac"));

        }
        else if (compilertype.equals("jikes")) {

            compiler = new JikesCompiler(Config.getPropString("bluej.compiler.executable", "jikes"));

        }
        else {
            Debug.message(Config.getString("compiler.invalidcompiler"));
        }

        thread = new CompilerThread();

        // Lower priority to improve GUI response time during compilation
        int priority = Thread.currentThread().getPriority() - 1;
        if (priority < Thread.MIN_PRIORITY)
            priority = Thread.MIN_PRIORITY;
        thread.setPriority(Thread.currentThread().getPriority() - 1);

        thread.start();
    }

    /**
     * Adds a job to the compile queue.
     */
    public void addJob(File[] sources, CompileObserver observer, String classPath, File destDir, boolean internal)
    {
        thread.addJob(new Job(sources, compiler, observer, classPath, destDir, internal));
    }

    /**
     * Wait until the compiler job queue is empty, then return.
     */
    public void waitForEmptyQueue()
    {
        synchronized (thread) {
            while (thread.isBusy()) {
                try {
                    thread.wait();
                }
                catch (InterruptedException ex) {}
            }
        }
    }
}