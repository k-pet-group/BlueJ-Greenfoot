package bluej.compiler;

/**
 * Observer interface for classes that are interested in compilation
 *
 * @author  Michael Cahill
 * @version $Id: CompileObserver.java 532 2000-06-08 07:46:08Z ajp $
 */
public interface CompileObserver
{
    void startCompile(String[] sources);
    void checkTarget(String qualifiedName);
    void errorMessage(String filename, int lineNo, String message,
    			 boolean invalidate);
    void endCompile(String[] sources, boolean succesful);
}
