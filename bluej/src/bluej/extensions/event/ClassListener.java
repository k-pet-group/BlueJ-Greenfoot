package bluej.extensions.event;

/**
 * This interface allows you to listen for class events.
 * 
 * <p>Currently the only event is the "state changed" event which can
 * be used to detect when a class becomes uncompiled (i.e. the source is
 * changed) or compiled. 
 * 
 * @author Davin McCall
 * @version $Id: ClassListener.java 4354 2006-06-13 04:27:35Z davmac $
 */
public interface ClassListener
{
    /**
     * The class state changed. This means that the class source was
     * changed so that the class is now uncompiled, or the class was
     * compiled, or the class was renamed.
     */
    public void classStateChanged(ClassEvent event);
}
