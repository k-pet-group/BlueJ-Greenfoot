package bluej.compiler;

/**
 ** CompileObserver interface - classes that are interested in compilation
 ** can implement this interface
 **
 ** @author Michael Cahill
 **
 ** @version $Id: CompileObserver.java 124 1999-06-14 07:26:17Z mik $
 **/

public interface CompileObserver
{
	void startCompile(String[] sources);
	void errorMessage(String filename, int lineNo, String message,
				 boolean invalidate);
	void endCompile(String[] sources, boolean succesful);
}

