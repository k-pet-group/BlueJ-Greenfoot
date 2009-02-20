/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import rmiextension.ProjectManager;
import rmiextension.wrappers.event.*;
import bluej.Config;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.ClassListener;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RBlueJImpl.java 6170 2009-02-20 13:29:34Z polle $
 */
public class RBlueJImpl extends java.rmi.server.UnicastRemoteObject
    implements RBlueJ
{
    BlueJ blueJ;

    Map listeners = new Hashtable();

    
    public RBlueJImpl(BlueJ blueJ)
        throws RemoteException
    {
        super();
        this.blueJ = blueJ;
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#addCompileListener(rmiextension.wrappers.event.RCompileListener, java.lang.String)
     */
    public void addCompileListener(RCompileListener listener, File projectPath)
    {
        BProject[] projects = blueJ.getOpenProjects();
        BProject project = null;
        for (int i = 0; i < projects.length; i++) {
            BProject prj = projects[i];
            try {
                if(prj.getDir().equals(projectPath)) {
                    project = prj;
                }
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
        }
        RCompileListenerWrapper wrapper = new RCompileListenerWrapper(listener, project);
        listeners.put(listener, wrapper);
        blueJ.addCompileListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#addInvocationListener(rmiextension.wrappers.event.RInvocationListener)
     */
    public void addInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = new RInvocationListenerWrapper(listener);
        listeners.put(listener, wrapper);
        blueJ.addInvocationListener(wrapper);
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#addClassListener(rmiextension.wrappers.event.RClassListener)
     */
    public void addClassListener(RClassListener listener) throws RemoteException
    {
        ClassListener wrapper = new RClassListenerWrapper(listener);
        listeners.put(listener, wrapper);
        blueJ.addClassListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getBlueJPropertyString(java.lang.String, java.lang.String)
     */
    public String getBlueJPropertyString(String property, String def)
    {
        return blueJ.getBlueJPropertyString(property, def);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getExtensionPropertyString(java.lang.String, java.lang.String)
     */
    public String getExtensionPropertyString(String property, String def)
    {
        return blueJ.getExtensionPropertyString(property, def);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getOpenProjects()
     */
    public RProject[] getOpenProjects()
        throws RemoteException
    {

        BProject[] bProjects = blueJ.getOpenProjects();
        int length = bProjects.length;
        RProject[] rProjects = new RProject[length];
        for (int i = 0; i < length; i++) {
            rProjects[i] = WrapperPool.instance().getWrapper(bProjects[i]);
        }

        return rProjects;
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#getSystemLibDir()
     */
    public File getSystemLibDir()
    {
        File f = blueJ.getSystemLibDir();
        //The getAbsoluteFile() fixes a weird bug on win using jdk1.4.2_06
        return f.getAbsoluteFile();
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#newProject(java.io.File)
     */
    public RProject newProject(File directory)
        throws RemoteException
    {
        ProjectManager.instance().addNewProject(directory);
        BProject wrapped = blueJ.newProject(directory);
        RProject wrapper = WrapperPool.instance().getWrapper(wrapped);
        ProjectManager.instance().removeNewProject(directory);
        return wrapper;
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#openProject(java.lang.String)
     */
    public RProject openProject(String directory)
        throws RemoteException
    {

        return WrapperPool.instance().getWrapper(blueJ.openProject(new File(directory)));
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#removeCompileListener(rmiextension.wrappers.event.RCompileListener)
     */
    public void removeCompileListener(RCompileListener listener)
    {
        RCompileListenerWrapper wrapper = (RCompileListenerWrapper) listeners.get(listener);
        listeners.remove(listener);
        blueJ.removeCompileListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#removeInvocationListener(rmiextension.wrappers.event.RInvocationListener)
     */
    public void removeInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = (RInvocationListenerWrapper) listeners.get(listener);
        listeners.remove(listener);
        blueJ.removeInvocationListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#removeClassListener(rmiextension.wrappers.event.RClassListener)
     */
    public void removeClassListener(RClassListener listener) throws RemoteException
    {
        ClassListener wrapper = (ClassListener) listeners.remove(listener);
        blueJ.removeClassListener(wrapper);
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#setExtensionPropertyString(java.lang.String, java.lang.String)
     */
    public void setExtensionPropertyString(String property, String value)
    {
        blueJ.setExtensionPropertyString(property, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.remote.RBlueJ#exit()
     */
    public void exit()
        throws RemoteException
    {
        BProject[] bProjects = blueJ.getOpenProjects();
        int length = bProjects.length;
        for (int i = 0; i < length; i++) {
            RProjectImpl rpImpl = WrapperPool.instance().getWrapper(bProjects[i]);
            rpImpl.notifyClosing();
        }
        
        PkgMgrFrame [] frames = PkgMgrFrame.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            frames[i].doClose(false, true);
        }
    }

    public Properties getInitialCommandLineProperties()
        throws RemoteException
    {
        return Config.getInitialCommandLineProperties();
    }
}
