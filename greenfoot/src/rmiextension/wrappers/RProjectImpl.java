/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2013  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.util.DebugUtil;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rmiextension.wrappers.event.RProjectListener;
import bluej.debugger.DebuggerThread;
import bluej.debugmgr.ExecControls;
import bluej.extensions.BClass;
import bluej.extensions.BField;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ReadmeTarget;
import bluej.utility.Debug;

/**
 * Implementation of the remote project interface.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class RProjectImpl extends java.rmi.server.UnicastRemoteObject
    implements RProject
{
    /** The BlueJ-package (from extensions) that is wrapped */
    private BProject bProject;
    
    /** The Greenfoot simulation thread */
    private DebuggerThread simulationThread;
    
    /**
     * A launcher object with a field called "transportField", used to
     * allow obtaining a remote reference to a debug VM object.
     */
    private BObject transportObject;
    
    private List<RProjectListener> listeners = new ArrayList<RProjectListener>();

    private boolean vmRestarted = false;

    /**
     * Construct an RProjectImpl - generally only should be called from
     * WrapperPool (use WrapperPool.instance().getWrapper(...)).
     * 
     * @param bProject  The project to wrap
     * @throws java.rmi.RemoteException
     */
    public RProjectImpl(BProject bProject)
        throws java.rmi.RemoteException
    {
        super();
        this.bProject = bProject;
        
        try {
            Project thisProject = Project.getProject(bProject.getDir());
            thisProject.getExecControls().setRestrictedClasses(DebugUtil.restrictedClassesAsNames());
        } catch (ProjectNotOpenException e) {
            Debug.message("Project not open while setting up debugger");
        }
    }

    /**
     * Set the object used for passing objects from the remote (debug VM) to the local VM.
     * The object should have a field of type Object called "transportField".
     */
    public synchronized void setTransportObject(BObject transportObject)
    {
        this.transportObject = transportObject;
        notifyAll();
    }
    
    /*
     * @see rmiextension.wrappers.RProject#close()
     */
    public void close()
    {
        notifyClosing();
        
        try {
            bProject.close();
        }
        catch (ProjectNotOpenException pnoe) {
            // this isn't a big deal; after all, we were trying to close
            // the project...
        }
    }
    
    /**
     * Inform listeners that this project will close. This should be called if the
     * project will be closed other than by calling RProjectImpl.close().
     */
    public void notifyClosing()
    {
        List<RProjectListener> listeners;
        synchronized (this.listeners) {
            listeners = new ArrayList<RProjectListener>(this.listeners);
        }
        
        Iterator<RProjectListener> i = listeners.iterator();
        while (i.hasNext()) {
            RProjectListener listener = i.next();
            try {
                listener.projectClosing();
            }
            catch (ServerError se) {
                Debug.reportError("Error when scenario closing: ", se);
            }
            catch (ServerException se) {
                Debug.reportError("Error when scenario closing: ", se);
            }
            catch (ConnectException ce) {
                // Almost certainly due to the other VM having already terminated:
                // So we'll ignore it.
                removeListener(listener);
            }
            catch (ConnectIOException cioe) {
                // Almost certainly due to the other VM having already terminated:
                // So we'll ignore it.
                removeListener(listener);
            }
            catch (RemoteException re) {
                Debug.reportError("Error when scenario closing: ", re);
                removeListener(listener);
            }
        }
    }

    /*
     * @see rmiextension.wrappers.RProject#getDir()
     */
    public File getDir()
        throws ProjectNotOpenException
    {
        return bProject.getDir();
    }

    /*
     * @see rmiextension.wrappers.RProject#getName()
     */
    public String getName()
        throws ProjectNotOpenException
    {
        return bProject.getName();
    }

    /*
     * @see rmiextension.wrappers.RProject#getPackage(java.lang.String)
     */
    public RPackage getPackage(String name)
        throws ProjectNotOpenException, RemoteException
    {
        BPackage bPackage = bProject.getPackage(name);
        RPackage wrapper = null;
        if (bPackage != null) {
            wrapper = WrapperPool.instance().getWrapper(bPackage);
        }

        return wrapper;
    }

    /*
     * @see rmiextension.wrappers.RProject#newPackage(java.lang.String)
     */
    public RPackage newPackage(String fullyQualifiedName)
        throws ProjectNotOpenException, PackageAlreadyExistsException, RemoteException
    {
        BPackage bPackage = bProject.newPackage(fullyQualifiedName);
        RPackage wrapper = null;
        wrapper = WrapperPool.instance().getWrapper(bPackage);

        return wrapper;
    }

    /*
     * @see rmiextension.wrappers.RProject#getPackages()
     */
    public RPackage[] getPackages()
        throws ProjectNotOpenException, RemoteException
    {
        BPackage[] packages = bProject.getPackages();
        int length = packages.length;
        RPackage[] wrapper = new RPackage[length];

        for (int i = 0; i < length; i++) {
            wrapper[i] = WrapperPool.instance().getWrapper(packages[i]);
        }

        return wrapper;
    }

    /*
     * @see rmiextension.wrappers.RProject#save()
     */
    public void save()
        throws ProjectNotOpenException
    {
        bProject.save();
    }
    
    /*
     * @see rmiextension.wrappers.RProject#openReadmeEditor()
     */
    public void openReadmeEditor()
        throws ProjectNotOpenException
    {
        Project thisProject = Project.getProject(bProject.getDir());
        bluej.pkgmgr.Package defaultPackage = thisProject.getPackage("");
        ReadmeTarget readmeTarget = defaultPackage.getReadmeTarget();
        readmeTarget.open();
    }
    
    /*
     * @see rmiextension.wrappers.RProject#addListener(rmiextension.wrappers.event.RProjectListener)
     */
    public void addListener(RProjectListener listener)
        throws RemoteException
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /*
     * @see rmiextension.wrappers.RProject#removeListener(rmiextension.wrappers.event.RProjectListener)
     */
    public void removeListener(RProjectListener listener)
    {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /*
     * @see rmiextension.wrappers.RProject#getRemoteObject()
     */
    @Override
    public synchronized RObject getRemoteObject() throws RemoteException
    {
        try {
            while (transportObject == null) {
                wait();
            }
            BClass bClass = transportObject.getBClass();
            BField field = bClass.getField("transportField");
            final BObject value = (BObject) field.getValue(transportObject);
            
            if (value != null) {
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run()
                    {
                        try {
                            String cName = value.getBClass().getName();
                            cName = cName.toLowerCase();
                            value.addToBench(cName);
                        }
                        catch (bluej.extensions.ClassNotFoundException cnfe) { }
                        catch (PackageNotFoundException pnfe) { }
                        catch (ProjectNotOpenException pnoe) { }
                    }
                });
                
                value.getInstanceName();
                return WrapperPool.instance().getWrapper(value);
            }
        }
        catch (InterruptedException ie) { }
        catch (bluej.extensions.ClassNotFoundException cnfe) { }
        catch (PackageNotFoundException pnfe) { }
        catch (ProjectNotOpenException pnoe) { }
        catch (InvocationTargetException ite) {
            Debug.reportError("Error adding object to bench", ite);
        }
        return null;
    }

    /*
     * @see rmiextension.wrappers.RProject#isExecControlVisible()
     */
    @Override
    public boolean isExecControlVisible() throws RemoteException
    {
        class ExecControlsChecker implements Runnable
        {
            public boolean visible;
            
            @Override
            public void run()
            {
                try {
                    Project thisProject = Project.getProject(bProject.getDir());
                    ExecControls execControls = thisProject.getExecControls();
                    execControls.setRestrictedClasses(DebugUtil.restrictedClassesAsNames());
                    visible = execControls.isVisible();
                }
                catch (ProjectNotOpenException pnoe) {
                    // This is ignorable.
                }
            }
        }
        
        ExecControlsChecker checker = new ExecControlsChecker();
        try {
            EventQueue.invokeAndWait(checker);
        }
        catch (InvocationTargetException ite) {
            Debug.reportError("Error checking exec controls visibility", ite);
        }
        catch (InterruptedException ie) { }
        
        return checker.visible;
    }

    /*
     * @see rmiextension.wrappers.RProject#toggleExecControls()
     */
    @Override
    public void toggleExecControls() throws RemoteException 
    {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                try {
                    Project thisProject = Project.getProject(bProject.getDir());
                    ExecControls execControls = thisProject.getExecControls();
                    execControls.makeSureThreadIsSelected(simulationThread);
                    execControls.showHide(!execControls.isVisible());
                    execControls.setRestrictedClasses(DebugUtil.restrictedClassesAsNames());
                }
                catch (ProjectNotOpenException pnoe) {
                    // This is ignorable.
                }
            } 
        });
    }
    
    /**
     * Set the Greenfoot simulation thread.
     */
    public void setSimulationThread(DebuggerThread simulationThread)
    {
        this.simulationThread = simulationThread;
        try {
            final Project thisProject = Project.getProject(bProject.getDir());
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run()
                {
                    if (thisProject.hasExecControls()) {
                        ExecControls execControls = thisProject.getExecControls();
                        execControls.makeSureThreadIsSelected(RProjectImpl.this.simulationThread);
                    }
                }
            });
        }
        catch (ProjectNotOpenException pnoe) { }
    }

    @Override
    public void restartVM() throws RemoteException, ProjectNotOpenException
    {
        try {
            // throws an illegal state exception
            // if this is called whilst the remote VM
            // is already restarting 
            vmRestarted = true;
            bProject.restartVM();
        }
        catch (IllegalStateException ise) {
        }
    }

    @Override
    public boolean isVMRestarted() throws RemoteException
    {
        return vmRestarted;
    }

    @Override
    public void setVmRestarted(boolean vmRestarted)
    {
        this.vmRestarted = vmRestarted;
    }
}
