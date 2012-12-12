/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2011,2012  Poul Henriksen and Michael Kolling 
 
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
import java.rmi.ServerError;
import java.rmi.ServerException;

import bluej.compiler.CompileObserver;
import bluej.compiler.Diagnostic;
import bluej.compiler.JobQueue;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;

/**
 * Implementation of a remote compiler queue.
 * 
 * @author Davin McCall
 */
public class RJobQueueImpl extends java.rmi.server.UnicastRemoteObject
    implements RJobQueue
{
    private JobQueue queue;
    private Package pkg;
    
    public RJobQueueImpl(Package pkg) throws RemoteException
    {
        super();
        queue = JobQueue.getJobQueue();
        this.pkg = pkg;
    }
    
    @Override
    public void compile(File[] files, final RCompileObserver observer)
            throws RemoteException
    {
        CompileObserver cobserver = new CompileObserver() {
            @Override
            public void startCompile(File[] sources)
            {
                try {
                    observer.startCompile(sources);
                }
                catch (ServerError se) {
                    Debug.reportError("Server error in RMI call: " + se.getCause());
                }
                catch (ServerException se) {
                    Debug.reportError("Server error in RMI call: " + se.getCause());
                }
                catch (RemoteException re) {
                    // probably, connection broken
                }
            }
            @Override
            public void endCompile(File[] sources, boolean successful)
            {
                try {
                    observer.endCompile(sources, successful);
                }
                catch (ServerError se) {
                    Debug.reportError("Server error in RMI call: " + se.getCause());
                }
                catch (ServerException se) {
                    Debug.reportError("Server error in RMI call: " + se.getCause());
                }
                catch (RemoteException re) {
                    // probably, connection broken
                }
            }
            @Override
            public boolean compilerMessage(Diagnostic diagnostic)
            {
                try {
                    observer.compilerMessage(diagnostic);
                }
                catch (ServerError se) {
                    Debug.reportError("Server error in RMI call: " + se.getCause());
                }
                catch (ServerException se) {
                    Debug.reportError("Server error in RMI call: " + se.getCause());
                }
                catch (RemoteException re) {
                    // probably, connection broken
                }
                return true;
            }
        };
        queue.addJob(files, cobserver, pkg.getProject().getClassLoader(), pkg.getPath(), true,
                pkg.getProject().getProjectCharset());
    }
}
