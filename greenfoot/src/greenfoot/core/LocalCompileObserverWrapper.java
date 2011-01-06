/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
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

import java.io.File;
import java.rmi.RemoteException;

import rmiextension.wrappers.RCompileObserver;
import bluej.compiler.CompileObserver;
import bluej.compiler.Diagnostic;

/**
 * Wraps a local compile as a remote compile observer.
 * 
 * @author Davin McCall
 */
public class LocalCompileObserverWrapper extends java.rmi.server.UnicastRemoteObject
        implements RCompileObserver
{
    private CompileObserver observer;
    
    public LocalCompileObserverWrapper(CompileObserver observer) throws RemoteException
    {
        this.observer = observer;
    }
    
    @Override
    public void startCompile(File[] sources)
    {
        observer.startCompile(sources);
    }
    
    @Override
    public void endCompile(File[] sources, boolean succesful)
    {
        observer.endCompile(sources, succesful);
    }
    
    @Override
    public void compilerMessage(Diagnostic diagnostic) throws RemoteException
    {
        observer.compilerMessage(diagnostic);
    }
}
