package bluej.extensions.event;

/**
 * This interface allows you to listen for when an invocation has finished.
 *
 * @version $Id: InvocationEventListener.java 1888 2003-04-25 09:09:16Z damiano $
 */
public interface InvocationEventListener
{
    /**
     * This method will be called when an invocation has finished.
     * If a long operation must be performed you should start a Thread.
     */
    public void invocationFinished (InvocationEvent event);
}