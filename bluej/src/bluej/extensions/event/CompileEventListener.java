package bluej.extensions.event;

/**
 * This interface allows you to listen for compile events.
 *
 * @version $Id: CompileEventListener.java 1873 2003-04-22 12:50:07Z damiano $
 */
public interface CompileEventListener
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
