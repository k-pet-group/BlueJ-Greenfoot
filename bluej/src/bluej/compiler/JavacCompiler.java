package bluej.compiler;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.Config;

/**
 ** @version $Id: JavacCompiler.java 176 1999-07-09 04:13:10Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** JavacCompiler class - an implementation for the BlueJ "Compiler"
 ** class. This implementation provides an interface to Sun's javac
 ** compiler running through a seperate Process.
 **/

public class JavacCompiler extends Compiler
{
    String executable;
    String destdir;
    String classpath;
    boolean debug;
    boolean deprecation;

    public JavacCompiler(String executable)
    {
	this.executable = executable;
	setDebug(true);
    }
	
    public void setDestDir(String destdir)
    {
	this.destdir = destdir;
    }
	
    public void setClassPath(String classpath)
    {
	this.classpath = classpath;
    }

    public void setDebug(boolean debug)
    {
	this.debug = debug;
    }

    public void setDeprecation(boolean deprecation)
    {
	this.deprecation = deprecation;
    }

    public boolean compile(String[] sources, CompileObserver watcher)
    {
	Vector args = new Vector();
		
	args.addElement(executable);

	if(destdir != null) {
	    args.addElement("-d");
	    args.addElement(destdir);
	}
		
	if(classpath != null) {
	    args.addElement("-classpath");
	    args.addElement(classpath);
	}
		
	if(debug)
	    args.addElement("-g");
		
	if(deprecation)
	    args.addElement("-deprecation");
		
	for(int i = 0; i < sources.length; i++)
	    args.addElement(sources[i]);
			
	int length = args.size();
	String[] params = new String[length];
	args.copyInto(params);
		
	boolean result = false;

	try {
	    result = executeCompiler(params, watcher);
	}
	catch (Exception ioe) {
	    Utility.showError(null, "Compiler error running " + executable + " (is the program in your path)\n");
	}

	return result;	
    }

    private boolean executeCompiler(String[] params, CompileObserver watcher) throws IOException, InterruptedException
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
		Utility.showError(null, "Compiler error:\n" + line);
		break;
	    }

	    String filename = line.substring(0, first_colon);
	
	    // Windows might have a colon after drive name. If so, ignore it
	    if(! filename.endsWith(".java")) {
		first_colon = line.indexOf(':', first_colon + 1);
	
		if(first_colon == -1) {
		    // cannot read format of error message
		    Utility.showError(null, "Compiler error:\n" + line);
		    break;
		}
		filename = line.substring(0, first_colon);
	    }
	
	    int second_colon = line.indexOf(':', first_colon + 1);
	    if(second_colon == -1) {
				// cannot read format of error message
		Utility.showError(null, "Compiler error:\n" + line);
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
		Utility.showError(null, "Compiler error. Error stream incomplete.\n");
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

