/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package rmiextension.wrappers.event;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.rmi.ServerException;

import rmiextension.wrappers.RBlueJImpl;

import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;
import bluej.utility.Debug;

/**
 * Wrapper for remote compile listeners.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class RCompileListenerWrapper
    implements CompileListener
{
    private RCompileListener remoteListener;
    private BProject project;
    private RBlueJImpl blueJ;

    public RCompileListenerWrapper(RCompileListener remoteListener, BProject project, RBlueJImpl blueJ)
    {
        this.remoteListener = remoteListener;
        this.project = project;
        this.blueJ = blueJ;
    }

    /**
     * Returns true if the files are in the project associated with this listener.
     */
    private boolean isInThisProject(File[] files)
    {
        if(files.length <= 0) {
            //Can't think of a reason why this should happen, but according to extensions API it can.
            return true;
        }
        File file = files[0];
        try {
            BPackage[] packages = project.getPackages();
            for (int i = 0; i < packages.length; i++) {
                BPackage pkg = packages[i];
                BClass[] classes = pkg.getClasses();
                for (int j = 0; j < classes.length; j++) {
                    BClass cls = classes[j];
                    if(cls.getJavaFile().equals(file)) {
                        return true;
                    }
                }
            }
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * @see bluej.extensions.event.CompileListener#compileStarted(bluej.extensions.event.CompileEvent)
     */
    public void compileStarted(final CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            new Thread() {
                @Override
                public void run()
                {
                    try {
                        RCompileEvent rEvent = new RCompileEventImpl(event);
                        remoteListener.compileStarted(rEvent);
                    }
                    catch (ServerError se) {
                        Debug.reportError("Remote compile listener ServerError", se.getCause());
                    }
                    catch (ServerException se) {
                        Debug.reportError("Remote compile listener ServerException", se.getCause());
                    }
                    catch (RemoteException re) {
                        // Connection interrupted or other problem; remote VM no longer accessible
                        blueJ.removeCompileListener(remoteListener);
                    }
                }
            }.start();
        }
    }

    
    /*
     * @see bluej.extensions.event.CompileListener#compileError(bluej.extensions.event.CompileEvent)
     */
    public void compileError(final CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            new Thread() {
                @Override
                public void run()
                {
                    try {
                        RCompileEvent rEvent = new RCompileEventImpl(event);
                        remoteListener.compileError(rEvent);
                    }
                    catch (ServerError se) {
                        Debug.reportError("Remote compile listener ServerError", se.getCause());
                    }
                    catch (ServerException se) {
                        Debug.reportError("Remote compile listener ServerException", se.getCause());
                    }
                    catch (RemoteException re) {
                        // Connection interrupted or other problem; remote VM no longer accessible
                        blueJ.removeCompileListener(remoteListener);
                    }
                }
            }.start();
        }
    }

    /*
     * @see bluej.extensions.event.CompileListener#compileWarning(bluej.extensions.event.CompileEvent)
     */
    public void compileWarning(final CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            new Thread() {
                @Override
                public void run()
                {
                    try {
                        RCompileEvent rEvent = new RCompileEventImpl(event);
                        remoteListener.compileWarning(rEvent);
                    }
                    catch (ServerError se) {
                        Debug.reportError("Remote compile listener ServerError", se.getCause());
                    }
                    catch (ServerException se) {
                        Debug.reportError("Remote compile listener ServerException", se.getCause());
                    }
                    catch (RemoteException re) {
                        // Connection interrupted or other problem; remote VM no longer accessible
                        blueJ.removeCompileListener(remoteListener);
                    }
                }
            }.start();
        }
    }

    /*
     * @see bluej.extensions.event.CompileListener#compileSucceeded(bluej.extensions.event.CompileEvent)
     */
    public void compileSucceeded(final CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            new Thread() {
                public void run() {
                    try {
                        RCompileEvent rEvent = new RCompileEventImpl(event);
                        remoteListener.compileSucceeded(rEvent);
                    }
                    catch (ServerError se) {
                        Debug.reportError("Remote compile listener ServerError", se.getCause());
                    }
                    catch (ServerException se) {
                        Debug.reportError("Remote compile listener ServerException", se.getCause());
                    }
                    catch (RemoteException re) {
                        // Connection interrupted or other problem; remote VM no longer accessible
                        blueJ.removeCompileListener(remoteListener);
                    }
                }
            }.start();
        }
    }

    /*
     * @see bluej.extensions.event.CompileListener#compileFailed(bluej.extensions.event.CompileEvent)
     */
    public void compileFailed(final CompileEvent event)
    {
        if (isInThisProject(event.getFiles())) {
            new Thread() {
                public void run() {
                    try {
                        RCompileEvent rEvent = new RCompileEventImpl(event);
                        remoteListener.compileFailed(rEvent);
                    }
                    catch (ServerError se) {
                        Debug.reportError("Remote compile listener ServerError", se.getCause());
                    }
                    catch (ServerException se) {
                        Debug.reportError("Remote compile listener ServerException", se.getCause());
                    }
                    catch (RemoteException re) {
                        // Connection interrupted or other problem; remote VM no longer accessible
                        blueJ.removeCompileListener(remoteListener);
                    }
                }
            }.start();
        }
    }
}
