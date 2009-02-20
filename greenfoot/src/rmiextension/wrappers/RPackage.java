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

import bluej.extensions.BObject;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
/**
 * The interface for a package.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RPackage.java 6170 2009-02-20 13:29:34Z polle $
 */
public interface RPackage
    extends java.rmi.Remote
{

    /**
     * Compile all modified files.
     * @param waitCompileEnd
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws RemoteException
     * @throws CompilationNotStartedException
     */
	public abstract void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException;

    /**
     * Force compilation of all files in the package.
     * @param waitCompileEnd
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws RemoteException
     * @throws CompilationNotStartedException
     */
	public abstract void compileAll(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException;

    /**
     * @param name
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RClass getRClass(String name)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RClass[] getRClasses()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract String getName()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @param instanceName
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RObject getObject(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract BObject[] getObjects()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public abstract RProject getProject()
        throws ProjectNotOpenException, RemoteException;

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void reload()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    public abstract RClass getSelectedClass()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * Returns the directory where this package is stored.
     * 
     * @throws ProjectNotOpenException
     *             if the project this package is part of has been closed by the
     *             user.
     * @throws PackageNotFoundException
     *             if the package has been deleted by the user.
     */
    public abstract File getDir()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * Creates a new Class with the given name. The class name must not be a
     * fully qualified name, and the .java file must already exist.
     * 
     * @throws ProjectNotOpenException
     *             if the project this package is part of has been closed by the
     *             user.
     * @throws PackageNotFoundException
     *             if the package has been deleted by the user.
     * @throws MissingJavaFileException
     *             if the .java file for the new class does not exist.
     */
    public abstract RClass newClass(String className)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException;

    /**
     * Invoke a constructor. Put the resulting object on the bench.<p>
     * 
     * Return is the compiler error message preceded by '!' in the case of
     * a compile time error, or the name of the constructed object, or null
     * if a run-time error occurred.
     * 
     * @param className   The fully qualified name of the class to instantiate
     * @param argTypes    The (raw) argument types of the constructor
     * @param args        The argument strings to use
     * @return   The name of the constructed object (see notes).
     */
    public String invokeConstructor(String className, String [] argTypes, String [] args)
        throws RemoteException;
    
    /**
     * Invoke a static method.
     * 
     * Return is the compiler error message preceded by '!' in the case of
     * a compile time error, or the name of the constructed object, or null
     * if a run-time error occurred.
     * 
     * @param className  The class for which to invoke the method
     * @param methodName The name of the method
     * @param argTypes   The argument types of the method (class names)
     * @param args       The argument strings to use
     * @return   The name of the returned object (see notes above).
     */
    public String invokeMethod(String className, String methodName, String [] argTypes, String [] args)
        throws RemoteException;

    public void close() throws RemoteException;

}
