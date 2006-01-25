package bluej.compiler;

import java.io.*;
import java.util.*;

import bluej.utility.DialogManager;

/**
 * JavacCompiler class - an implementation for the BlueJ "Compiler"
 * class. This implementation provides an interface to Sun's javac
 * compiler running through a seperate Process.
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 * @version $Id: JavacCompiler.java 3747 2006-01-25 10:29:24Z iau $
 */
class JavacCompiler extends Compiler
{
    private String executable;   

    public JavacCompiler(String executable)
    {
        this.executable = executable;
        setDebug(true);
        setDeprecation(true);
    }   

    public boolean compile(File[] sources, CompileObserver watcher, boolean internal)
    {
        List args = new ArrayList();

        args.add(executable);       

        args.addAll(getCompileOptions());        

        for(int i = 0; i < sources.length; i++)
            args.add(sources[i].getPath());

        int length = args.size();
        String[] params = new String[length];
        args.toArray(params);

        boolean result = false;

        try {
            result = executeCompiler(params, watcher, internal);
        }
        catch (Exception ioe) {
            DialogManager.showErrorWithText(null, "cannot-run-compiler",
        				    executable);
        }

        return result;
    }

    private boolean executeCompiler(String[] params, CompileObserver watcher, boolean internal)
        throws IOException, InterruptedException
    {
        int processresult = 0;		// default to fail in case we don't even start compiler process
        boolean readerror = false;
        
        Process compiler = Runtime.getRuntime().exec(params);
        
        BufferedReader d = new BufferedReader(
                new InputStreamReader(compiler.getErrorStream()));
        JavacErrorStream output = new JavacErrorStream(internal);
        
        String line;
        
        while((line = d.readLine()) != null)            
            output.print(line);
        
        // Handle any error message
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
        
        processresult = compiler.waitFor();
        
        // we consider ourselves successful if we got no error messages and the process
        // gave a 0 result
        
        return (processresult == 0 && !readerror);
    }
}

