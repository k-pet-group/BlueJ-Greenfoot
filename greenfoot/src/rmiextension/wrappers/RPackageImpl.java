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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;

import bluej.debugmgr.Invoker;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.BClass;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;

/**
 * This class is a wrapper for a BlueJ package
 * 
 * @see bluej.extensions.BPackage
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RPackageImpl.java 6216 2009-03-30 13:41:07Z polle $
 */
public class RPackageImpl extends java.rmi.server.UnicastRemoteObject
    implements RPackage
{

    //The BlueJ-package (from extensions) that is wrapped
    BPackage bPackage;

    public RPackageImpl(BPackage bPackage)
        throws java.rmi.RemoteException
    {
        super();
        this.bPackage = bPackage;
    }

    /////////////////////
    // Wrapper Methods
    /////////////////////

    /**
     * @param forceAll
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        bPackage.compile(waitCompileEnd);
    }
    
    public void compileAll(boolean waitCompileEnd)
    throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        bPackage.compileAll(waitCompileEnd);
    }

    /**
     * @param name
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RClass getRClass(String name)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return WrapperPool.instance().getWrapper(bPackage.getBClass(name));

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RClass[] getRClasses()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        BClass[] bClasses = bPackage.getClasses();
        int length = bClasses.length;
        RClass[] rClasses = new RClass[length];
        for (int i = 0; i < length; i++) {
            rClasses[i] = WrapperPool.instance().getWrapper(bClasses[i]);
        }

        return rClasses;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public String getName()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        return bPackage.getName();
    }

    /**
     * @param instanceName
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RObject getObject(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        BObject wrapped = bPackage.getObject(instanceName);
        RObject wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public BObject[] getObjects()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        return bPackage.getObjects();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public RProject getProject()
        throws RemoteException, ProjectNotOpenException
    {
        return WrapperPool.instance().getWrapper(bPackage.getProject());
    }

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void reload()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        bPackage.reload();
    }

    public RClass getSelectedClass()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        //TODO Hack for testing:
        return getRClasses()[0];
        //return selectedClass;
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.remote.RPackage#getDir()
     */
    public File getDir()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return bPackage.getDir();
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.remote.RPackage#newClass(java.lang.String)
     */
    public RClass newClass(String className)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException
    {
        BClass wrapped = bPackage.newClass(className);
        RClass wrapper = WrapperPool.instance().getWrapper(wrapped);
        bPackage.reload();
        return wrapper;
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
    public String invokeMethod(String className, String methodName, String [] argTypes, String [] args)
    {
        Package pkg = getPackage();
        Class cl = pkg.loadClass(className);
        View mClassView = View.getView(cl);
        
        MethodView theMethod = null;
        
        // do we really need to search super classes?
        classLoop:
        while (mClassView != null) {
            MethodView [] methods = mClassView.getDeclaredMethods();
            findMethod:
            for (int i = 0; i < methods.length; i++) {
                // This method is not the one we're looking for if it's
                // private, has a different name, or a different number
                // of parameters
                if ((methods[i].getModifiers() & Modifier.PRIVATE) != 0)
                    continue;
                if ((methods[i].getModifiers() & Modifier.STATIC) == 0)
                    continue;
                if (! methods[i].getName().equals(methodName))
                    continue;
                if (methods[i].getParameterCount() != argTypes.length)
                    continue;
                
                // ... or if any of the parameters are different
                Class [] params = methods[i].getParameters();
                for (int j = 0; j < params.length; j++) {
                    if (! params[j].getName().equals(argTypes[j]))
                        continue findMethod;
                }
                
                // we've found the right method
                return invokeCallable(PkgMgrFrame.findFrame(pkg), methods[i], null, args);
            }
            
            // try the super class
            mClassView = mClassView.getSuper();
        }

        throw new IllegalArgumentException("method not found");
    }
    
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
    {
        // TODO support generics
        
        Package pkg = getPackage();
        Class cl = pkg.loadClass(className);
        View mClassView = View.getView(cl);

        // Search through the constructors for the one we want.
        ConstructorView [] constructors = mClassView.getConstructors();
        consLoop:
        for (int i = 0; i < constructors.length; i++) {
            // check the parameter count and types match
            if (constructors[i].getParameterCount() != argTypes.length)
                continue;
            Class [] params = constructors[i].getParameters();
            for (int j = 0; j < params.length; j++) {
                if (! params[j].getName().equals(argTypes[j]))
                    continue consLoop;
            }
            
            // we have a match
            return invokeCallable(PkgMgrFrame.findFrame(pkg), constructors[i], null, args);
        }
        
        // Couldn't find the requested constructor
        throw new IllegalArgumentException("constructor not found");
    }
    
    /**
     * Invoke a callable (a constructor, static method or instance method).<p>
     * 
     * Return is the compiler error message preceded by '!' in the case of
     * a compile time error, or the name of the constructed object, or null
     * if a run-time error occurred.
     * 
     * @param pkg     The package from which to perform the invocation
     * @param cv      The constructor or method to invoke
     * @param ow      The object to invoke against (null for static method
     *                or constructor)
     * @param argVals The arguments to apply to the call
     */
    public static String invokeCallable(PkgMgrFrame pmf, CallableView cv, ObjectWrapper ow, String [] argVals)
    {
        // also used by RPackage
        InvocationResultWatcher watcher = new InvocationResultWatcher();
        Invoker invoker;
        if (ow == null)
            invoker = new Invoker(pmf, cv, watcher);
        else
            invoker = new Invoker(pmf, (MethodView) cv, ow, watcher);

        synchronized (watcher) {
            invoker.invokeDirect(argVals);
            try {
                watcher.wait();
            }
            catch (InterruptedException ie) {}
        }
        
        if (watcher.errorMsg != null) {
            // some error occurred
            return "!" + watcher.errorMsg;
        }
        else {
            if (watcher.resultObj == null)
                return null;
            
            ObjectWrapper newOw = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), watcher.resultObj, watcher.resultObj.getGenType(), "result");
            pmf.getObjectBench().addObject(newOw);
            Package pkg = pmf.getPackage();
            pkg.getDebugger().addObject(pkg.getId(), newOw.getName(), newOw.getObject());
            //BObject newBObject = bObject.getPackage().getObject(newOw.getName());
            //WrapperPool.instance().getWrapper(newBObject);
            //new RObjectImpl(newBObject);
            return newOw.getName();
        }
    }
    
    /**
     * Helper routine. Extract the actual Bluej package (bluej.pkgmgr.Package)
     * from the extension interface (BPackage) using reflection.
     */
    private Package getPackage()
    {
        try {
            Class bPackageClass = bPackage.getClass();
            Field packageId = bPackageClass.getDeclaredField("packageId");
            packageId.setAccessible(true);
            Object identifier = packageId.get(bPackage);
            
            Class identifierClass = identifier.getClass();
            Method getBluejPackage = identifierClass.getDeclaredMethod("getBluejPackage", new Class[0]);
            getBluejPackage.setAccessible(true);
            Package pkg = (bluej.pkgmgr.Package) getBluejPackage.invoke(identifier, (Object[]) null);
            
            return pkg;
        }
        catch (NoSuchFieldException nsfe) { nsfe.printStackTrace(); }
        catch (IllegalAccessException iae) { iae.printStackTrace(); }
        catch (NoSuchMethodException nsme) { nsme.printStackTrace(); }
        catch (InvocationTargetException ite) { ite.printStackTrace(); }
        return null;
    }

    public void close()
        throws RemoteException
    {

        //Make sure that we first close "greenfoot" package becuase we don't want that to auto open the next time.
        try {
            PkgMgrFrame pkgMgrFrame = (PkgMgrFrame) bPackage.getFrame();
            pkgMgrFrame.doClose(false, true);
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
