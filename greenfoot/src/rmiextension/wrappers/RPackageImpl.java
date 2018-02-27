/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2012,2014,2015,2016  Poul Henriksen and Michael Kolling
 
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

import java.awt.EventQueue;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;

import javax.swing.SwingUtilities;

import bluej.debugmgr.Invoker;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.BClass;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.SourceType;
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
 */
public class RPackageImpl extends java.rmi.server.UnicastRemoteObject
    implements RPackage
{

    //The BlueJ-package (from extensions) that is wrapped
    private final WeakReference<BPackage> bPackage;
    
    // Used to hold exceptions thrown when we try to compile
    private static CompilationNotStartedException cnse;
    private static PackageNotFoundException pnfe;
    private static ProjectNotOpenException pnoe;
    // Returns from various methods
    private static RClass rclassResult;
    private static RClass[] rclassArrayResult;

    public RPackageImpl(BPackage bPackage)
        throws java.rmi.RemoteException
    {
        super();
        this.bPackage = new WeakReference<>(bPackage);
    }

    /*
     * @see rmiextension.wrappers.RPackage#getRClass(java.lang.String)
     */
    @Override
    public RClass getRClass(final String name)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        synchronized (RPackageImpl.class) {
            pnoe = null;
            pnfe = null;
            
            try {
                EventQueue.invokeAndWait(() -> {
                   try {
                       rclassResult = WrapperPool.instance().getWrapper(getBPackage().getBClass(name));
                   } catch (RemoteException e) {
                       e.printStackTrace();
                   } catch (ProjectNotOpenException e) {
                       pnoe = e;
                   } catch (PackageNotFoundException e) {
                       pnfe = e;
                   }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            
            if (pnoe != null) throw pnoe;
            if (pnfe != null) throw pnfe;
            
            return rclassResult;
        }
    }

    /*
     * @see rmiextension.wrappers.RPackage#getRClasses()
     */
    @Override
    public RClass[] getRClasses()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        synchronized (RPackageImpl.this) {
            pnoe = null;
            pnfe = null;

            try {
                EventQueue.invokeAndWait(() -> {
                    try {
                        BClass[] bClasses = getBPackage().getClasses();
                        int length = bClasses.length;
                        RClass[] rClasses = new RClass[length];
                        for (int i = 0; i < length; i++) {
                            rClasses[i] = WrapperPool.instance().getWrapper(bClasses[i]);
                        }

                        rclassArrayResult = rClasses;
                    }
                    catch (ProjectNotOpenException e) {
                        pnoe = e;
                    }
                    catch (PackageNotFoundException e) {
                        pnfe = e;
                    }
                    catch (RemoteException re) {}
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if (pnoe != null) throw pnoe;
            if (pnfe != null) throw pnfe;

            return rclassArrayResult;
        }
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RPackage#getProject()
     */
    @Override
    public RProject getProject()
        throws RemoteException, ProjectNotOpenException
    {
        return WrapperPool.instance().getWrapper(getBPackage().getProject());
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RPackage#close()
     */
    @Override
    public void close()
        throws RemoteException
    {

        //Make sure that we first close "greenfoot" package becuase we don't want that to auto open the next time.
        try {
            PkgMgrFrame pkgMgrFrame = ExtensionBridge.getPkgMgrFrame(getBPackage());
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

    private BPackage getBPackage()
    {
        return bPackage.get();
    }
}
