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

import java.rmi.RemoteException;

import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RObject.java 6216 2009-03-30 13:41:07Z polle $
 */
public interface RObject
    extends java.rmi.Remote
{
    /**
     * @param instanceName
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void addToBench(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     * @throws PackageNotFoundException 
     */
    public abstract RClass getRClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException, PackageNotFoundException;

    /**
     * @return
     */
    public abstract String getInstanceName()
        throws RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void removeFromBench()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    
    // no longer needed
//    public MenuSerializer getMenu()
//        throws RemoteException;
    
    /**
     * Allow an arbitrary method to be invoked with arbitrary parameters.
     * 
     * If a compilation error occurs, returns the generated error message
     * preceded by an exclamation mark (!).<p>
     * 
     * If successful and a return value exists, put the return value on the
     * bench and return its name.<p>
     * 
     * Otherwise (no error, no return value) return null. 
     * 
     * @param method    The name of the method to invoke
     * @param argTypes  The classnames of the argument types of the method
     * @param argVals   The argument "values" as they should appear in the shell code
     * @throws RemoteException
     */
    public String invokeMethod(String method, String [] argTypes, String [] argVals)
        throws RemoteException;

}