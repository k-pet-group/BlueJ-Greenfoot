package bluej.compiler;

import java.io.*;
import java.util.*;

import bluej.prefmgr.PrefMgr;
import bluej.utility.DialogManager;

/**
 * JavacCompiler class - an implementation for the BlueJ "Compiler"
 * class. This implementation provides an interface to Sun's javac
 * compiler running through a seperate Process.
 *
 * @author Michael Cahill
 * @author Michael Kolling
 * @author Bruce Quig
 * @version $Id: JavacCompiler.java 1765 2003-04-09 05:56:45Z ajp $
 */
public class JavacCompiler extends Compiler
{
    private String executable;
    private File destDir;
    private String classPath;
    private boolean debug;
    private boolean deprecation;

    public JavacCompiler(String executable)
    {
        this.executable = executable;
        setDebug(true);
    }

    public void setDestDir(File destDir)
    {
        this.destDir = destDir;
    }

    public void setClassPath(String classPath)
    {
        this.classPath = classPath;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    public void setDeprecation(boolean deprecation)
    {
        this.deprecation = deprecation;
    }

    public boolean compile(File[] sources, CompileObserver watcher)
    {
        List args = new ArrayList();

        args.add(executable);

        if(destDir != null) {
            args.add("-d");
            args.add(destDir.getPath());
        }

        if(classPath != null) {
            args.add("-classpath");
            args.add(classPath);
        }

        if(debug)
            args.add("-g");

        if(deprecation)
            args.add("-deprecation");
        
        if(! System.getProperty("java.vm.version").startsWith("1.3"))
            if(PrefMgr.getFlag(PrefMgr.ENABLE_JDK14)) {
                args.add("-source");
                args.add("1.4");
            }

         /** Not used at present...
        // add user specified compiler options
        List userOptions = CompileUtility.getUserCompilerOptions();
        if(userOptions != null && userOptions.size() > 0) {
            Iterator it = userOptions.iterator();
            while(it.hasNext()) {
                args.add((String)it.next());
            }
        }
        */

        for(int i = 0; i < sources.length; i++)
            args.add(sources[i]);

        int length = args.size();
        String[] params = new String[length];
        args.toArray(params);

        boolean result = false;

        try {
            result = executeCompiler(params, watcher);
        }
        catch (Exception ioe) {
            DialogManager.showErrorWithText(null, "cannot-run-compiler",
        				    executable);
        }

        return result;
    }

    private boolean executeCompiler(String[] params, CompileObserver watcher)
        throws IOException, InterruptedException
    {
	int processresult = 0;		// default to fail in case we don't even start compiler process
	boolean readerror = false;

	Process compiler = Runtime.getRuntime().exec(params);

	BufferedReader d = new BufferedReader(
					      new InputStreamReader(compiler.getErrorStream()));
	String line;

	while((line = d.readLine()) != null) {

	    // Debug.message("Compiler message: " + line);

	    // javac produces error messages in the format
	    // /home/ajp/sample/Tester.java:10: description of error.
	    // line of source code
	    //              ^

	    int first_colon = line.indexOf(':', 0);

	    if(first_colon == -1) {
				// cannot read format of error message
		DialogManager.showErrorWithText(null, "compiler-error", line);
		break;
	    }

	    String filename = line.substring(0, first_colon);

	    // Windows might have a colon after drive name. If so, ignore it
	    if(! filename.endsWith(".java")) {
		first_colon = line.indexOf(':', first_colon + 1);

		if(first_colon == -1) {
		    // cannot read format of error message
		    DialogManager.showErrorWithText(null, "compiler-error",
						    line);
		    break;
		}
		filename = line.substring(0, first_colon);
	    }

	    int second_colon = line.indexOf(':', first_colon + 1);
	    if(second_colon == -1) {
				// cannot read format of error message
		DialogManager.showErrorWithText(null, "compiler-error", line);
		break;
	    }

	    int lineNo = 0;

	    try {
		lineNo = Integer.parseInt(line.substring(first_colon + 1, second_colon));
	    } catch(NumberFormatException e) {
				// ignore it
	    }

	    String error = line.substring(second_colon + 1);

	    // read and ignore the following two lines (these contain
	    // the faulty source line and an up arrow ^)

	    if((d.readLine() == null) || (d.readLine() == null)) {
				// we are missing part of the normal error report
		DialogManager.showError(null, "stream-incomplete");
	    }
	    else {
		//Debug.message("Indicating error " + filename + " " + lineNo);
		readerror = true;

		watcher.errorMessage(filename, lineNo, error, true);
		break;
	    }
	}

	processresult = compiler.waitFor();

	// we consider ourselves successful if we got no error messages and the process
	// gave a 0 result

	return (processresult == 0 && !readerror);
    }
}

