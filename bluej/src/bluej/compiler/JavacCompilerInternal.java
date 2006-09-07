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
 * @version $Id: JavacCompilerInternal.java 4599 2006-09-07 02:40:51Z davmac $
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

        // There are two "compile" methods, one which takes a printwriter as an
        // argument. We'd prefer to use that one if we can find it.
        boolean compileMethodTakesPrintWriter = false;
        
        try {
            compiler = Class.forName("com.sun.tools.javac.Main");

            if (compiler == null)
                return false;

            Class[] ppw = new Class[] {String[].class, PrintWriter.class};
            Class[] p = new Class[] {String[].class};

            try {
                compileMethod = compiler.getMethod("compile", ppw);
                compileMethodTakesPrintWriter = true;
            }
            catch (NoSuchMethodException nsme) {
                compileMethod = compiler.getMethod("compile", p);
            }
            
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
        JavacErrorWriter output = new JavacErrorWriter(internal);
        WriterOutputStream outputS = null;

        if (! compileMethodTakesPrintWriter) {
            // We have to temporarily redirect System.out in order to be
            // able to trap compiler messages
            outputS = new WriterOutputStream(output);
            System.setErr(new PrintStream(outputS));
        }

        int result = 1;
        try {
            Object objResult;
            if (compileMethodTakesPrintWriter) {
                Object [] arguments = new Object [] { params, new PrintWriter(output) };
                objResult = compileMethod.invoke(compiler.newInstance(), arguments);
            }
            else {
                Object[] arguments = new Object[] { params };
                objResult = compileMethod.invoke(compiler.newInstance(), arguments);
            }

            result = ((Integer) objResult).intValue();
        } catch (Throwable e) {
            System.setErr(systemErr);   // restore
            e.printStackTrace(System.out);
            return false;
        }
        
        if (! compileMethodTakesPrintWriter) {
            try {
                outputS.close();
            }
            catch (IOException ioe) { }
            System.setErr(systemErr);   // restore
        }

		if (output.hasError()) {
			watcher.errorMessage(output.getFilename(),
						output.getLineNo(),
						output.getMessage());
		}

        // Handle compiler warning messages        
        if (output.hasWarnings()) {
            watcher.warningMessage(output.getFilename(),
						output.getLineNo(),
						output.getWarning());
        }
        
		return result==0;
	}
}
