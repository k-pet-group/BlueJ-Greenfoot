package greenfoot.core;

import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RProject;
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
 * @version $Id: ClassStateManager.java 5154 2007-08-10 07:02:51Z davmac $
 */
public class ClassStateManager extends RClassListenerImpl
{
	private GProject project;
	
    /**
     * Construct a ClassStateManager.
     * @throws RemoteException
     */
    public ClassStateManager(GProject project) throws RemoteException
    {
        super();
        this.project = project;
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassListener#classStateChanged(rmiextension.wrappers.event.RClassEvent)
     */
    public void classStateChanged(RClassEvent event) throws RemoteException
    {
        RClass eventClass = event.getRClass();
        int eventId = event.getEventId();
        
        try {
            RProject eventProject = eventClass.getPackage().getProject();
            if(! project.getRProject().equals(eventProject)) {
                // BlueJ sends out events from all projects to all other projects
                // For greenfoot we ignore events not belonging to this project.
                return;
            }            
            
            GPackage pkg = project.getPackage(eventClass.getPackage());
            GClass gClass = pkg.getGClass(eventClass);
            
            if (eventId == ClassEvent.STATE_CHANGED) {
                boolean compiled = event.isClassCompiled();
                gClass.setCompiledState(compiled);
            }
            else if (eventId == ClassEvent.CHANGED_NAME) {
                gClass.nameChanged(event.getOldName());
            }
        }
        catch (PackageNotFoundException pnfe) {}
        catch (ProjectNotOpenException pnoe) {}
    }
}
