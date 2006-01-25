package bluej.compiler;

import java.io.File;

/**
 * Observer interface for classes that are interested in compilation.
 *
 * All events are generated on the compiler thread.
 *
 * @author  Michael Cahill
 * @version $Id: CompileObserver.java 3747 2006-01-25 10:29:24Z iau $
 */
public interface CompileObserver
{
    /**
     * A compilation job has started.
     */
    void startCompile(File[] sources);
    
    /**
     * An error message occurred during compilation
     */
    void errorMessage(String filename, int lineNo, String message);
    
    /**
     * A warning message occurred during compilation
     */
    void warningMessage(String filename, int lineNo, String message);

    /**
     * A Compilation job finished.
     */
    void endCompile(File[] sources, boolean succesful);
}
