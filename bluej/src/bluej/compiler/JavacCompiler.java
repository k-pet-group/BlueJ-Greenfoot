package bluej.compiler;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import sun.tools.javac.BlueJJavacMain;

import bluej.utility.Debug;

/**
 ** @version $Id: JavacCompiler.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** JavacCompiler class - an implementation for the BlueJ "Compiler"
 ** class. This implementation provides an interface to Sun's javac
 ** compiler.
 **/

public class JavacCompiler extends Compiler
{
    PrintStream output;
    String destdir;
    String classpath;
    boolean debug;
    boolean deprecation;

    public JavacCompiler(PrintStream output)
    {
	this.output = output;
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
		
	BlueJJavacMain javac = new BlueJJavacMain(output);
		
	boolean result = false;
		
	try {
	    result = javac.compile(params, watcher);
	} catch(CompilerMessageError e) {
	    watcher.errorMessage(e.getFilename(), e.getLineNo(), 
				 e.getMessage(), true);
	}
		
	return result;
    }
}
