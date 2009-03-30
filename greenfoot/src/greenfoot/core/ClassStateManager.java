/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
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
 * @version $Id: ClassStateManager.java 6216 2009-03-30 13:41:07Z polle $
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
