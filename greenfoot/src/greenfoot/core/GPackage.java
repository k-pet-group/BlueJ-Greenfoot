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

import greenfoot.World;
import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RJobQueue;
import rmiextension.wrappers.RPackage;
import bluej.compiler.CompileObserver;
import bluej.debugmgr.InvokerCompiler;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * Represents a package in Greenfoot.
 * 
 * <p>A GPackage is essentially a reference to a remote package (RPackage), together
 * with a pool of GClass objects representing the classes in the package. 
 * 
 * @author Poul Henriksen
 */
public class GPackage
{
    private RPackage pkg;
    private GProject project; 
    
    private Map<RClass,GClass> classPool = new HashMap<RClass,GClass>();
    
    /**
     * Contructor for an unspecified package, but for which a project is known.
     * Used to allow a class to not be part of a package, but still being able
     * to get the project the class is part of.
     */
    GPackage(GProject project) 
    {
        if(project == null) {
            throw new NullPointerException("Project must not be null.");
        }
        this.project = project;
    }
    
    /**
     * Construct a new GPackage; this should generally only be called by
     * GProject.
     * 
     * @param pkg  The reference to the remote package
     * @param project  The project
     */
    public GPackage(RPackage pkg, GProject project)
    {
        if(pkg == null) {
            throw new NullPointerException("Pkg must not be null.");
        }
        if(project == null) {
            throw new NullPointerException("Project must not be null.");
        }
        this.pkg = pkg;
        this.project = project;
    }
    
    /**
     * Get the GClass wrapper for a remote class in this package.
     */
    public GClass getGClass(RClass remoteClass, boolean inRemoteCallback)
    {
        if (remoteClass == null) {
            return null;
        }
        
        GClass gClass;
        synchronized (classPool) {
            gClass = classPool.get(remoteClass);
            if (gClass == null) {
                gClass = new GClass(remoteClass, this, inRemoteCallback);
                classPool.put(remoteClass, gClass);
                gClass.loadSavedSuperClass(inRemoteCallback);
            }
        }
        return gClass;
    }

    public void compileAll()
    {
        try {
            pkg.compileAll();
        }
        catch (ProjectNotOpenException pnoe) {
            Debug.reportError("Could not start compilation", pnoe);
        }
        catch (CompilationNotStartedException cnse) {
            Debug.reportError("Could not start compilation", cnse);
        }
        catch (RemoteException re) {
            Debug.reportError("Could not start compilation", re);
        }
        catch (PackageNotFoundException pnfe) {
            Debug.reportError("Could not start compilation", pnfe);
        }
    }

    public File getDir()
    {
        try {
            return pkg.getDir();
        }
        catch (ProjectNotOpenException pnoe) {
            Debug.reportError("Could not get package directory", pnoe);
            throw new InternalGreenfootError(pnoe);
        }
        catch (PackageNotFoundException pnfe) {
            Debug.reportError("Could not get package directory", pnfe);
            throw new InternalGreenfootError(pnfe);
        }
        catch (RemoteException re) {
            Debug.reportError("Could not get package directory", re);
            throw new InternalGreenfootError(re);
        }
    }

    public GProject getProject()
    {
        return project;
    }

    public GClass[] getClasses(boolean inRemoteCallback)
    {
        try {
            RClass[] rClasses = pkg.getRClasses();
            GClass[] gClasses = new GClass[rClasses.length];
            for (int i = 0; i < rClasses.length; i++) {
                RClass rClass = rClasses[i];
                gClasses[i] = getGClass(rClass, inRemoteCallback);
            }
            return gClasses;
        }
        catch (ProjectNotOpenException e) {
            Debug.reportError("Could not get package classes", e);
            throw new InternalGreenfootError(e);
        }
        catch (PackageNotFoundException e) {
            Debug.reportError("Could not get package classes", e);
            throw new InternalGreenfootError(e);
        }
        catch (RemoteException e) {
            Debug.reportError("Could not get package classes", e);
            throw new InternalGreenfootError(e);
        }
    }

    public GClass newClass(String className, boolean inRemoteCallback)
    {
        GClass newClass = null;
        try {
            RClass newRClass = pkg.newClass(className);
            newClass = new GClass(newRClass, this, inRemoteCallback);
            synchronized (classPool) {
                classPool.put(newRClass, newClass);
            }
            newClass.loadSavedSuperClass(false);
        }
        catch (RemoteException re) {
            Debug.reportError("Creating new class", re);
        }
        catch (ProjectNotOpenException pnoe) {
            Debug.reportError("Creating new class", pnoe);
        }
        catch (PackageNotFoundException pnfe) {
            Debug.reportError("Creating new class", pnfe);
        }
        catch (MissingJavaFileException mjfe) {
            Debug.reportError("Creating new class", mjfe);
        }
        return newClass;
    }
    
    /**
     * Get the named class (null if it cannot be found).
     * Do not call from a remote callback.
     */
    public GClass getClass(String className)
    {
        try {
            RClass rClass = pkg.getRClass(className);
            return getGClass(rClass, false);
        }
        catch (RemoteException re) {
            Debug.reportError("Getting class", re);
        }
        catch (ProjectNotOpenException pnoe) {
            Debug.reportError("Creating new class", pnoe);
        }
        catch (PackageNotFoundException pnfe) {
            Debug.reportError("Creating new class", pnfe);
        }
        
        return null;
    }

    /** 
     * Returns all the world sub-classes in this package that can be instantiated.
     * Do not call from a remote callback.
     */
    @SuppressWarnings("unchecked")
    public List<Class<? extends World>> getWorldClasses()
    {
        List<Class<? extends World>> worldClasses= new LinkedList<Class<? extends World>>();
        GClass[] classes = getClasses(false);
        for (int i = 0; i < classes.length; i++) {
            GClass cls = classes[i];
            if(cls.isWorldSubclass()) {
                Class<? extends World> realClass = (Class<? extends World>) cls.getJavaClass();   
                if (GreenfootUtil.canBeInstantiated(realClass)) {                  
                    worldClasses.add(realClass);
                }                    
            }
        }
        return worldClasses;
    }

    /**
     * Get access to the remote compiler queue.
     */
    public InvokerCompiler getCompiler()
    {
        try {
            final RJobQueue rqueue = pkg.getCompiler();
            return new InvokerCompiler() {
                @Override
                public void compile(File[] files, CompileObserver observer)
                {
                    try {
                        rqueue.compile(files, new LocalCompileObserverWrapper(observer));
                    }
                    catch (RemoteException re) {
                        Debug.reportError("Error trying to compile on remote queue", re);
                    }
                }
            };
        }
        catch (RemoteException re) {
            Debug.reportError("Error getting remote compiler queue", re);
            return null;
        }
    }
    
    public void reload()
    {
        try {
            pkg.reload();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
