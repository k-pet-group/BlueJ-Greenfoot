package rmiextension.wrappers.event;

/**
 * Abstract implementation of an RProjectListener.
 * 
 * @author Davin McCall
 * @version $Id: RProjectListenerImpl.java 4349 2006-06-12 03:07:04Z davmac $
 */
public abstract class RProjectListenerImpl
    implements RProjectListener
{
    public RProjectListenerImpl()
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RProjectListener#projectClosing()
     */
    public abstract void projectClosing();

}
