package bluej.compiler;

import bluej.Config;
import bluej.utility.Debug;

import java.util.Vector;

/**
 ** @version $Id: Job.java 1085 2002-01-11 22:30:13Z mik $
 ** @author Michael Cahill
 **
 ** A compiler "job" - list of filenames to compile + parameters.
 ** Jobs are held in a queue by the CompilerThread, which compiles them
 ** by running the job's "compile" method.
 **/

public class Job
{
    Compiler compiler;		// The compiler for this job
    CompileObserver observer;
    String destdir;
    String classpath;
    String sources[];
	
    /**
     * contructor - create a job with a set of sources
     */
    public Job(String[] sources, Compiler compiler,
               CompileObserver observer, String classpath, String destdir)
    {
        this.sources = sources;
        this.compiler = compiler;
        this.observer = observer;
        this.classpath = classpath;
        this.destdir = destdir;
    }
	
    /**
     * contructor - create a job with one source file
     */
    public Job(String sourcefile, Compiler compiler,
               CompileObserver observer, String classpath, String destdir)
    {
        this.sources = new String[1];
        this.sources[0] = sourcefile;
        this.compiler = compiler;
        this.observer = observer;
        this.classpath = classpath;
        this.destdir = destdir;
    }

    /**
     * compile - compile this job
     */
    public void compile()
    {
        try {
            boolean successful = true;
			
            if(observer != null)
                observer.startCompile(sources);
            if(destdir != null)
                compiler.setDestDir(destdir);
            if(classpath != null)
                compiler.setClassPath(classpath);

            successful = compiler.compile(sources, observer);
	    //Debug.message("compile success: " + successful);

            if(observer != null)
                observer.endCompile(sources, successful);
        } catch(Exception e) {
            System.err.println(compileException + e);
            e.printStackTrace();
        }
    }
	
    static String compileException = Config.getString("compileException");
}
