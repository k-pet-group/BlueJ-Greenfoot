package bluej.compiler;

import java.io.File;

import bluej.Config;

/**
 * A compiler "job". A list of filenames to compile + parameters.
 * Jobs are held in a queue by the CompilerThread, which compiles them
 * by running the job's "compile" method.
 *
 * @author  Michael Cahill
 * @version $Id: Job.java 2691 2004-06-30 05:23:41Z davmac $
 */
class Job
{
    Compiler compiler;		// The compiler for this job
    CompileObserver observer;
    File destDir;
    String classPath;
    File sources[];
    boolean internal; // true for compiling shell files; false for user files
	
    /**
     * Create a job with a set of sources.
     */
    public Job(File[] sourceFiles, Compiler compiler, CompileObserver observer,
    			String classPath, File destDir, boolean internal)
    {
        this.sources = sourceFiles;
        this.compiler = compiler;
        this.observer = observer;
        this.classPath = classPath;
        this.destDir = destDir;
        this.internal = internal;
    }
	
    /**
     * Compile this job
     */
    public void compile()
    {
        try {
            boolean successful = true;
			
            if(observer != null)
                observer.startCompile(sources);
            if(destDir != null)
                compiler.setDestDir(destDir);
            if(classPath != null)
                compiler.setClassPath(classPath);

            successful = compiler.compile(sources, observer, internal);
	        //Debug.message("compile success: " + successful);

            if(observer != null)
                observer.endCompile(sources, successful);
        } catch(Exception e) {
            System.err.println(Config.getString("compileException") + e);
            e.printStackTrace();
        }
    }
}
