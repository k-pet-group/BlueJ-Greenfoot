package bluej.compiler;

/**
 * Observer interface for classes that are interested in compilation
 *
 * @author  Michael Cahill
 * @version $Id: CompileObserver.java 505 2000-05-24 05:44:24Z ajp $
 */
public interface CompileObserver
{
    void startCompile(String[] sources);
    void errorMessage(String filename, int lineNo, String message,
    			 boolean invalidate);
    void endCompile(String[] sources, boolean succesful);
}
