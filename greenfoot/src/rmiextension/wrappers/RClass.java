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
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RClass.java 6216 2009-03-30 13:41:07Z polle $
 */
public interface RClass
    extends java.rmi.Remote
{
    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException;

    public abstract void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @param signature
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract RConstructor getConstructor(Class[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @param methodName
     * @param params
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract BMethod getDeclaredMethod(String methodName, Class[] params)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @param fieldName
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public abstract RClass getSuperclass()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract boolean isCompiled()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * This should actually have been the toString() method, but we cannot add
     * an exception to an inherited method. And to get RMI to work, it must
     * throw RemoteException.
     * 
     * @return
     * @throws ClassNotFoundException 
     * @throws ProjectNotOpenException 
     */
    public String getToString()
        throws RemoteException, ProjectNotOpenException, ClassNotFoundException;

    /**
     * @return
     * @throws ClassNotFoundException 
     * @throws ProjectNotOpenException 
     */
    public abstract String getQualifiedName()
        throws RemoteException, ProjectNotOpenException, ClassNotFoundException;

  
    public File getJavaFile()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;
    //public MenuSerializer getMenu()
    //    throws RemoteException;

    public abstract void remove() throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    /**
     * 
     * @see Editor#setReadOnly(boolean)
     */
    public abstract void setReadOnly(boolean b) throws RemoteException, ProjectNotOpenException, PackageNotFoundException ;
}