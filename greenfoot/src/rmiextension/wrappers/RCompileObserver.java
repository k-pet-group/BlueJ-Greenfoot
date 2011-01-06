/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2011  Poul Henriksen and Michael Kolling 
 
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

import bluej.compiler.Diagnostic;

/**
 * A remote version of the CompileObserver interface.
 * 
 * @author Davin McCall
 */
public interface RCompileObserver extends java.rmi.Remote
{
    /**
     * A compilation job has started.
     */
    void startCompile(File[] sources) throws RemoteException;
    
    /**
     * An error or warning message occurred during compilation
     */
    void compilerMessage(Diagnostic diagnostic) throws RemoteException;
    
    /**
     * A Compilation job finished.
     */
    void endCompile(File[] sources, boolean succesful) throws RemoteException;
}
