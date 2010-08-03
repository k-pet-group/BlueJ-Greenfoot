/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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

import bluej.compiler.CompileObserver;
import bluej.compiler.JobQueue;
import bluej.pkgmgr.Package;

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
            // DAV handle and report exceptions which occur at the other end
            // (ServerError, ServerException).
            @Override
            public void startCompile(File[] sources)
            {
                try {
                    observer.startCompile(sources);
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
                catch (RemoteException re) {
                    // probably, connection broken
                }
            }
            @Override
            public void errorMessage(String filename, int lineNo, String message)
            {
                try {
                    observer.errorMessage(filename, lineNo, message);
                }
                catch (RemoteException re) {
                    // probably, connection broken
                }
            }
            @Override
            public void warningMessage(String filename, int lineNo,
                    String message)
            {
                try {
                    observer.warningMessage(filename, lineNo, message);
                }
                catch (RemoteException re) {
                    // probably, connection broken
                }
            }
        };
        queue.addJob(files, cobserver, pkg.getProject().getClassLoader(), pkg.getPath(), true);
    }
}
