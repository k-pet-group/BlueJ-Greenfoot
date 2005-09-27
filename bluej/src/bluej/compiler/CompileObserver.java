package bluej.compiler;

import java.io.File;

/**
 * Observer interface for classes that are interested in compilation.
 *
 * All events are generated on the compiler thread.
 *
 * @author  Michael Cahill
 * @version $Id: CompileObserver.java 3590 2005-09-27 04:33:52Z davmac $
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
     * A Compilation job finished.
     */
    void endCompile(File[] sources, boolean succesful);
}
