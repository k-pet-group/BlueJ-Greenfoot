package bluej.extensions.event;

/**
 * This interface allows you to listen for when an invocation has finished.
 *
 * @version $Id: InvocationResultEventListener.java 1878 2003-04-22 14:36:01Z damiano $
 */
public interface InvocationResultEventListener
{
    /**
     * This method will be called when an invocation has finished.
     * If a long operation must be performed you should start a Thread.
     */
    public void invocationFinished (InvocationResultEvent event);
}