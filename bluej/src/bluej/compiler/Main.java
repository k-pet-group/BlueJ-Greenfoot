package bluej.compiler;

/**
 ** @version $Id: Main.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Reasonably generic interface between the BlueJ IDE and the Java
 ** compiler.
 **/

public class Main
{
    private static CompilerThread thread;
    // private static Compiler compiler = new PizzaCompiler();
    private static Compiler compiler = new JavacCompiler(new ErrorStream());
	
    /**
     * Makes global initializations that have to be set before a compilation.
     */
    public static void init()
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
    public static void addJob(String[] sources, CompileObserver observer,
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
    public static void addJob(String sourcefile, CompileObserver observer,
			      String classpath, String destdir)
    {
	thread.addJob(new Job(sourcefile, compiler, observer,
			      classpath, destdir));
    }
}
