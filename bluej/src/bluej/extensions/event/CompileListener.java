package bluej.extensions.event;

/**
 * This interface allows you to listen for compile events.
 * The order of occurence of these method calls for a given compilation is:
 * <pre>
 *     compileStarted()
 *     compileError()                        # If a compilation error occurs
 *     compileWarning()                      # If a compilation warning occurs
 *     compileFailed() or compileSucceeded()
 * </pre>
 * Note that currently BlueJ only reports the first compilation error or warning.
 *
 * @version $Id: CompileListener.java 1904 2003-04-27 17:12:42Z iau $
 */
public interface CompileListener
{
    /**
     * This method will be called when a compilation starts.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileStarted (CompileEvent event);
    
    /**
     * This method will be called when there is a report of a compile error.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileError (CompileEvent event);

    /**
     * This method will be called when there is a report of a compile warning.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileWarning (CompileEvent event);

    /**
     * This method will be called when the compile ends successfully.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileSucceeded (CompileEvent event);


    /**
     * This method will be called when the compile fails.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileFailed (CompileEvent event);


}
