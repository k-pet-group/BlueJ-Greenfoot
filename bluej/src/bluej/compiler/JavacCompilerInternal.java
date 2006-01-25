package bluej.compiler;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import bluej.utility.*;

/**
 * An implementation for the BlueJ "Compiler"
 * class providing an interface to Sun's javac
 * compiler by executing the com.sun.tools methods directly.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @author  Bruce Quig
 * @version $Id: JavacCompilerInternal.java 3747 2006-01-25 10:29:24Z iau $
 */
class JavacCompilerInternal extends Compiler
{
    // private ErrorStream firstStream = null;
	
	public JavacCompilerInternal()
	{
		setDebug(true);
        setDeprecation(true);
	}
	

	public boolean compile(File[] sources, CompileObserver watcher, boolean internal)
	{
		List args = new ArrayList();		

        args.addAll(getCompileOptions());      
                
        for(int i = 0; i < sources.length; i++)
            args.add(sources[i].getPath());

        int length = args.size();
        String[] params = new String[length];
        args.toArray(params);

        Class compiler = null;
        Method compileMethod = null;

        /* problem number one is that between 1.3 and 1.4 the compile
           method changed from an instance method to a static method.
           use reflection to fix this.
           based on an idea from the JDEE code by jslopez@alum.mit.edu */

        try {
            compiler = Class.forName("com.sun.tools.javac.Main");

            if (compiler == null)
                return false;

            Class[] p = new Class[] {String[].class};

            compileMethod = compiler.getMethod("compile", p);

        } catch (ClassNotFoundException e) {
            Debug.message("com.sun.tools.javac.Main compiler is not available");
            return false;
        } catch (NoSuchMethodException e) {
            Debug.message("com.sun.tools.javac.Main compile method could not be found");
            return false;
        }

        if (compileMethod == null)
            return false;

        PrintStream systemErr = System.err;
        JavacErrorStream output = new JavacErrorStream(internal);

        /* second problem with the jdk1.4 beta. It seems to use a
           PrintWriter wrapped around the system error stream. It also
           seems to cache the creation of this, so on subsequent compiles,
           the original output stream is still being used. To cope with
           both 1.3 and 1.4, we check the results of both the first stream
           and the newly created stream */

        /* 2004-06-30: No longer seems to be a problem */
        
        //if (firstStream == null)
        //    firstStream = output;

        System.setErr(output);      // redirect errors to our stream

        int result = 1;
        try {
            Object objResult;
            Object[] arguments = new Object[] { params };
            objResult = compileMethod.invoke(compiler.newInstance(), arguments);

            result = ((Integer) objResult).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // only one of 'output' or 'firstStream' should be receiving output so
        // we will only generate one error message
		if (output.hasError()) {
			watcher.errorMessage(output.getFilename(),
						output.getLineNo(),
						output.getMessage());
		}

        //if (firstStream != null && firstStream != output && firstStream.hasError()) {
		//	watcher.errorMessage(firstStream.getFilename(),
		//				firstStream.getLineNo(),
		//				firstStream.getMessage(),true);
		//}

        // Handle compiler warning messages        
        if (output.hasWarnings()) {
            watcher.warningMessage(output.getFilename(),
						output.getLineNo(),
						output.getWarning());
        }
        System.setErr(systemErr);   // restore

        // in case we are reusing the first stream, reset the hasError boolean
        //if (firstStream != null)
        //    firstStream.reset();

		return result==0;
	}
}
