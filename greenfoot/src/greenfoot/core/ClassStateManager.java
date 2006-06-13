package greenfoot.core;

import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.event.RClassEvent;
import rmiextension.wrappers.event.RClassListenerImpl;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.ClassEvent;

/**
 * This is a class for keeping track of class state changes, and informing
 * GClass objects when their compilation state changes. This avoids the need
 * for each GClass to be a ClassListener.
 * 
 * @author Davin McCall
 * @version $Id: ClassStateManager.java 4356 2006-06-13 05:20:11Z davmac $
 */
public class ClassStateManager extends RClassListenerImpl
{
    /**
     * Construct a ClassStateManager.
     * @throws RemoteException
     */
    public ClassStateManager() throws RemoteException
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassListener#classStateChanged(rmiextension.wrappers.event.RClassEvent)
     */
    public void classStateChanged(RClassEvent event) throws RemoteException
    {
        RClass eventClass = event.getRClass();
        int eventId = event.getEventId();
        
        try {
            GProject project = GreenfootMain.getInstance().getProject();
            GPackage pkg = project.getPackage(eventClass.getPackage());
            GClass gClass = pkg.getGClass(eventClass);
            
            if (eventId == ClassEvent.STATE_CHANGED) {
                boolean compiled = event.isClassCompiled();
                gClass.setCompiledState(compiled);
            }
            else if (eventId == ClassEvent.CHANGED_NAME) {
                gClass.nameChanged();
            }
        }
        catch (PackageNotFoundException pnfe) {}
        catch (ProjectNotOpenException pnoe) {}
    }
}
