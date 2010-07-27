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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.BClass;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.MethodView;
import bluej.views.View;

/**
 * @author Poul Henriksen
 * @version $Id: RObjectImpl.java 7934 2010-07-27 05:55:36Z davmac $
 */
public class RObjectImpl extends UnicastRemoteObject
    implements RObject
{
    /**
     * @throws RemoteException
     */
    protected RObjectImpl()
        throws RemoteException
    {
        super();
    }

    public RObjectImpl(BObject bObject)
        throws RemoteException
    {
        this.bObject = bObject;
        if (bObject == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    BObject bObject;

    /**
     * @param instanceName
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void addToBench(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        bObject.addToBench(instanceName);
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     * @throws PackageNotFoundException 
     */
    public RClass getRClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException, PackageNotFoundException
    {
        BClass wrapped = bObject.getBClass();
        RClass wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @return
     */
    public String getInstanceName()
        throws RemoteException
    {
        return bObject.getInstanceName();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        BPackage wrapped = bObject.getPackage();
        RPackage wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void removeFromBench()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        bObject.removeFromBench();
    }

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
    public String invokeMethod(String method, String [] argTypes, String [] argVals)
        throws RemoteException
    {
        try {
            // First find the method. The existing extension mechanism makes
            // this pretty much impossible; BClass only allows searching for
            // Declared methods - we want a method that may have been declared
            // in a super class.
            
            // TODO: add to extension mechanism(?).
            // For the moment, cheat and use reflection.
            
            Class<?> BObjectClass = BObject.class;
            Field oWrapperField = BObjectClass.getDeclaredField("objectWrapper");
            oWrapperField.setAccessible(true);
            ObjectWrapper ow = (ObjectWrapper) oWrapperField.get(bObject);

            // Debug.message("Calling method: " + method + " on object: " + ow.getName());
            String className = ow.getObject().getClassName();
            PkgMgrFrame pmf = ow.getFrame();
            Class<?> oClass = ow.getPackage().loadClass(className);
            
            // can't just use getMethods() as that doesn't give us package-private
            // methods, sigh...
            View mClassView = View.getView(oClass);
            
            while (mClassView != null) {
                MethodView [] methods = mClassView.getDeclaredMethods();
                findMethod:
                for (int i = 0; i < methods.length; i++) {
                    // This method is not the one we're looking for if it's
                    // private, has a different name, or a different number
                    // of parameters
                    if ((methods[i].getModifiers() & Modifier.PRIVATE) != 0)
                        continue;
                    if (! methods[i].getName().equals(method))
                        continue;
                    if (methods[i].getParameterCount() != argTypes.length)
                        continue;
                    
                    // ... or if any of the parameters are different
                    Class<?> [] params = methods[i].getParameters();
                    for (int j = 0; j < params.length; j++) {
                        if (! params[j].getName().equals(argTypes[j]))
                            continue findMethod;
                    }
                    
                    // we've found the right method
                    // theMethod = methods[i];
                    return RPackageImpl.invokeCallable(pmf, methods[i], ow, argVals);
                }
                
                // try the super class
                mClassView = mClassView.getSuper();
            }
        }
        catch (NoSuchFieldException nsfe) {
            nsfe.printStackTrace();
        }
        catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }
        
        throw new IllegalArgumentException("method not found.");
    }

}