package bluej.compiler;

import sun.tools.javac.JavacWatcher;

/**
 ** @version $Id: CompileObserver.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** CompileObserver interface - classes that are interested in compilation
 ** can implement this interface
 **/

public interface CompileObserver extends JavacWatcher
{
	public void startCompile(String[] sources);
	public void errorMessage(String filename, int lineNo, String message,
				 boolean invalidate);
	public void endCompile(String[] sources, boolean succesful);
}

