package bluej.extensions.event;

/**
 * This interface allows you to listen for when an invocation has finished.
 *
 * @version $Id: InvocationListener.java 1894 2003-04-25 09:53:08Z damiano $
 */
public interface InvocationListener
{
    /**
     * This method will be called when an invocation has finished.
     * If a long operation must be performed you should start a Thread.
     */
    public void invocationFinished (InvocationEvent event);
}