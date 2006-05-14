package bluej.extensions.event;

/**
 * This interface allows you to listen for class events.
 * 
 * <p>Currently the only event is the "state changed" event which can
 * be used to detect when a class becomes uncompiled (i.e. the source is
 * changed) or compiled. 
 * 
 * @author Davin McCall
 * @version $Id: ClassListener.java 4257 2006-05-14 16:38:01Z davmac $
 */
public interface ClassListener
{
    /**
     * The class state changed. This means that the class source was
     * changed so that the class is now uncompiled, or the class was
     * compiled.
     * 
     * <p>Use event.isCompiled() to check the class compilation state.
     */
    public void classStateChanged(ClassEvent event);
}
