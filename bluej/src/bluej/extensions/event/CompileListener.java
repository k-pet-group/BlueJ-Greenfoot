package bluej.extensions.event;

/**
 * This interface allows you to listen for compile events.
 *
 * @version $Id: CompileListener.java 1892 2003-04-25 09:35:41Z damiano $
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
    public void compileSuceeded (CompileEvent event);


    /**
     * This method will be called when the compile fails.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileFailed (CompileEvent event);


}
