package bluej.compiler;

import sun.tools.javac.JavacWatcher;

/**
 ** @version $Id: CompileObserver.java 114 1999-06-08 04:02:49Z mik $
 ** @author Michael Cahill
 ** CompileObserver interface - classes that are interested in compilation
 ** can implement this interface
 **/

public interface CompileObserver
{
	void startCompile(String[] sources);
	void errorMessage(String filename, int lineNo, String message,
				 boolean invalidate);
	void endCompile(String[] sources, boolean succesful);
}

