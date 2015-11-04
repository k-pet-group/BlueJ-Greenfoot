package bluej.compiler;

import java.io.File;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Observer interface for classes that are interested in compilation.
 *
 * All events will be received on the event dispatch thread.
 *
 * @author  Michael Cahill
 */
@OnThread(Tag.Swing)
public interface EDTCompileObserver
{
    /**
     * A compilation job has started.
     */
    void startCompile(File[] sources);
    
    /**
     * An error or warning message occurred during compilation
     * 
     * Returns whether or not the error was shown to the user (for data collection purposes)
     */
    boolean compilerMessage(Diagnostic diagnostic);
    
    /**
     * A Compilation job finished.
     */
    void endCompile(File[] sources, boolean succesful);
}