package bluej.compiler;

import sun.tools.javac.JavacWatcher;

/**
 ** @version $Id: CompileObserver.java 49 1999-04-28 03:01:02Z ajp $
 ** @author Michael Cahill
 ** CompileObserver interface - classes that are interested in compilation
 ** can implement this interface
 **/

public interface CompileObserver extends JavacWatcher
{
	void startCompile(String[] sources);
	void errorMessage(String filename, int lineNo, String message,
				 boolean invalidate);
	void endCompile(String[] sources, boolean succesful);
}

