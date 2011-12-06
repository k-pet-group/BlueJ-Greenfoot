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
package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.BField;
import bluej.extensions.BMethod;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.editor.Editor;

/**
 * Remote BlueJ class interface.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public interface RClass
    extends java.rmi.Remote
{
    public abstract void compile(boolean waitCompileEnd, boolean forceQuiet)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException;

    public abstract void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;
    
    /**
     * Closes the editor (sets the editor to not visible)
     * @throws ProjectNotOpenException   if the project has been closed
     * @throws PackageNotFoundException  if the package has been removed
     * @throws RemoteException           if a remote exception occurs
     */
    public abstract void closeEditor()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;
    
    public abstract void insertAppendMethod(String comment, String access, String methodName, String methodBody, boolean showEditorOnCreate, boolean showEditorOnAppend)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    public abstract void insertMethodCallInConstructor(String methodName, boolean showEditor)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;
    
    public abstract RConstructor getConstructor(Class<?>[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    public abstract RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    public abstract BMethod getDeclaredMethod(String methodName, Class<?>[] params)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    public abstract BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    public abstract RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    public abstract BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    public abstract RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * Gets the superclass of this class (if it has one).
     * 
     * @param inRemoteCallback  whether this method is being called from a method which was itself invoked
     *                          by a callback from this virtual machine, which blocks the dispatch thread.
     */
    public abstract RClass getSuperclass(boolean inRemoteCallback)
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    public abstract boolean isCompiled(boolean inRemoteCallback)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    public abstract String getQualifiedName()
        throws RemoteException;

  
    public File getJavaFile()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    public abstract void remove()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    /**
     * Put the editor for this class in or out of read-only mode.
     * 
     * @see Editor#setReadOnly(boolean)
     */
    public abstract void setReadOnly(boolean b)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException ;

    /**
     * Show a message in the editor status area for this class.
     */
    public abstract void showMessage(String message)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException;

    /**
     * Check whether this class has a source file.
     */
    public abstract boolean hasSourceCode()
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException;

    /**
     * Auto-indents the code for this class.
     */
    public abstract void autoIndent()
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException;
}
