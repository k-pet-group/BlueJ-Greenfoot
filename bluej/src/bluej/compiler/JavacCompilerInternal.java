package bluej.compiler;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import sun.tools.javac.Main;

import bluej.utility.*;

/**
 ** @version $Id: JavacCompilerInternal.java 269 1999-11-10 05:36:05Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** JavacCompilerInternal class - an implementation for the BlueJ "Compiler"
 ** class. This implementation provides an interface to Sun's javac
 ** compiler by executing the sun.tools methods directly.
 **/

public class JavacCompilerInternal extends Compiler
{
	String destdir;
	String classpath;
	boolean debug;
	boolean deprecation;

	public JavacCompilerInternal()
	{
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
		
		ErrorStream output = new ErrorStream();

		Main javac = new Main(output, "javac");
		
		boolean result = javac.compile(params);

		if (output.hasError()) {
			watcher.errorMessage(output.getFilename(),
						output.getLineNo(),
						output.getMessage(),true);
		}
		
		return result;
	}
}

/**
 ** @version $Id: JavacCompilerInternal.java 269 1999-11-10 05:36:05Z mik $
 ** @author Michael Cahill
 ** ErrorStream - OutputStream that parses javac output.
 **/

class ErrorStream extends PrintStream
{
    private boolean haserror = false;
    private String filename, message;
    private int lineno;

    public ErrorStream()
    {
	// we do not actually intend to use an actual OutputStream from
	// within this class yet our superclass requires us to pass a
	// non-null OutputStream
	// we pass it the system error stream
	super(System.err);
    }
	
    public boolean hasError()
    {
	return haserror;
    }

    public String getFilename()
    {
	Debug.assert(haserror);
	return filename;
    }

    public int getLineNo()
    {
	Debug.assert(haserror);
	return lineno;
    }

    public String getMessage() 
    {
	Debug.assert(haserror);
	return message;
    }

    /**
     ** Note: this class "cheats" by assuming that all output will be written by
     ** a call to println. It happens that this is true for the current version 
     ** of javac but this could change in the future.
     **
     ** We assume a certain error message format here:
     **   filename:line-number:message
     **
     ** We find the components by searching for the colons. Careful: MS Windows
     ** systems might have a colon in the file name (if it is an absolute path
     ** with a drive name included). In that case we have to ignore the first
     ** colon.
     **/
    public void println(String msg)
    {
	if (haserror)
	    return;

	// Debug.message("Compiler message: " + msg);
		
	int first_colon = msg.indexOf(':', 0);
	if(first_colon == -1) {
	    // cannot read format of error message
	    DialogManager.showErrorWithText(null, "compiler-error", msg);
	    return;
	}

	filename = msg.substring(0, first_colon);

	// Windows might have a colon after drive name. If so, ignore it
	if(! filename.endsWith(".java")) {
	    first_colon = msg.indexOf(':', first_colon + 1);
	    if(first_colon == -1) {
				// cannot read format of error message
		DialogManager.showErrorWithText(null, "compiler-error", msg);
		return;
	    }
	    filename = msg.substring(0, first_colon);
	}
	int second_colon = msg.indexOf(':', first_colon + 1);
	if(second_colon == -1) {
	    // cannot read format of error message
	    DialogManager.showErrorWithText(null, "compiler-error", msg);
	    return;
	}

	lineno = 0;
	try {
	    lineno = Integer.parseInt(msg.substring(first_colon + 1, second_colon));
	} catch(NumberFormatException e) {
	    // ignore it
	}

	message = msg.substring(second_colon + 1);

	haserror = true;
    }
}
