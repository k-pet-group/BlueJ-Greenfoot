package bluej.compiler;

import java.io.File;

/**
 * Observer interface for classes that are interested in compilation
 *
 * @author  Michael Cahill
 * @version $Id: CompileObserver.java 1765 2003-04-09 05:56:45Z ajp $
 */
public interface CompileObserver
{
    void startCompile(File[] sources);
    void checkTarget(String qualifiedName);
    void errorMessage(String filename, int lineNo, String message,
    			 boolean invalidate);
    void endCompile(File[] sources, boolean succesful);
}
