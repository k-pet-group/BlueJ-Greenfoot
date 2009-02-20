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
package greenfoot.core;

import greenfoot.event.CompileListener;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import rmiextension.wrappers.event.RCompileEvent;
import rmiextension.wrappers.event.RProjectListenerImpl;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.CompileEvent;
import bluej.utility.Debug;

/**
 * Represents a project in greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GProject extends RProjectListenerImpl
    implements CompileListener
{    
    private Map<RPackage,GPackage> packagePool = new HashMap<RPackage,GPackage>();
    
    private RProject rProject;

    private ProjectProperties projectProperties;
    
    private List<CompileListener> compileListeners = new LinkedList<CompileListener>();
    
    
    /**
     * Create a G(reenfoot)Project object. This is a singleton for every
     * running Greenfoot project (for every VM).
     * 
     * <p>The creator is responsible for setting up the GProject as a compile listener.
     */
    public GProject(RProject rmiProject) throws RemoteException
    {
        this.rProject = rmiProject;
        rmiProject.addListener(this);
        try {
            projectProperties = new ProjectProperties(getDir());
        }
        catch (Exception exc) {
            Debug.reportError("Could not open greenfoot scenario properties");
            exc.printStackTrace();
        }
    }
    
    public void close()
        throws RemoteException
    {
        rProject.close();
    }

    /**
     * Request a save of all open files in the project.
     * @throws ProjectNotOpenException
     * @throws RemoteException
     */
    public void save()
        throws ProjectNotOpenException, RemoteException
    {
        rProject.save();
    }
    
    /**
     * returns the default package.
     * 
     */
    public GPackage getDefaultPackage() throws ProjectNotOpenException, RemoteException {
    	return getPackage("");
    }

    /**
     * returns the greenfoot package.
     * 
     */
    public GPackage getGreenfootPackage() throws ProjectNotOpenException, RemoteException {
        return getPackage("greenfoot");
    }
    
    /**
     * returns the named package.
     */
    public GPackage getPackage(String packageName) throws ProjectNotOpenException, RemoteException
    {
        RPackage rPkg = rProject.getPackage(packageName);
        if (rPkg == null) {
            return null;
        }
        else {
            return getPackage(rPkg);
        }
    }

    /**
     * Get a GPackage wrapper for an RPackage object
     */
    public GPackage getPackage(RPackage pkg)
    {
        GPackage ret = packagePool.get(pkg);
        if (ret == null) {
            ret = new GPackage(pkg, this);
            packagePool.put(pkg, ret);
        }
        return ret;
    }
    
    public RPackage[] getPackages()
        throws ProjectNotOpenException, RemoteException
    {
        return rProject.getPackages();
    }


    public GPackage newPackage(String fullyQualifiedName)
        throws ProjectNotOpenException, PackageAlreadyExistsException, RemoteException
    {
        return getPackage(rProject.newPackage(fullyQualifiedName));
    }

    /**
     * Get a remote reference to a class in this project.
     * 
     * @param fullyQualifiedName  The fully-qualified class name
     * @return  A remote reference to the class
     */
    public RClass getRClass(String fullyQualifiedName)
    {
        try {
            int lastDotIndex = fullyQualifiedName.lastIndexOf('.');
            RPackage pkg;
            String className;
            if (lastDotIndex == -1) {
                pkg = rProject.getPackage("");
                className = fullyQualifiedName;
            }
            else {
                pkg = rProject.getPackage(fullyQualifiedName.substring(0, lastDotIndex));
                className = fullyQualifiedName.substring(lastDotIndex + 1);
            }
            if (pkg == null) {
                return null;
            }
            return pkg.getRClass(className);
        }
        catch (RemoteException re) {
            throw new InternalGreenfootError(re);
        }
        catch (ProjectNotOpenException pnoe) {
            throw new InternalGreenfootError(pnoe);
        }
        catch (PackageNotFoundException pnfe) {
            throw new InternalGreenfootError(pnfe);
        }
    }

    public File getDir()
        throws ProjectNotOpenException,RemoteException
    {
        return rProject.getDir();
    }

    
    /**
     * Get the project name (the name of the directory containing it)
     */
    public String getName()
    {
        try {
            return rProject.getName();
        }
        catch (ProjectNotOpenException pnoe) {
            // this exception should never happen
            pnoe.printStackTrace();
        }
        catch (RemoteException re) {
            // this should also not happen
            re.printStackTrace();
        }
        return null;
    }
        
    public boolean inTestMode()
    {
        //Greenfoot does not support testing:
        return false;
    }
    
    /**
     * Retrieve the properties for a package. Loads the properties if necessary.
     */
    public ProjectProperties getProjectProperties()
    {
        return projectProperties;
    }
    
    /**
     * Show the readme file for this project in an editor window.
     */
    public void openReadme()
    {
        try {
            rProject.openReadmeEditor();
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
        }
    }
    
    /**
     * Get the remote project reference which this GProject wraps.
     */
    public RProject getRProject()
    {
        return rProject;
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RProjectListener#projectClosing()
     */
    public void projectClosing()
    {
        GreenfootMain.getInstance().projectClosing();
    }
    
    /**
     * Checks whether every class in this project is compiled.
     * @return True is all classes are compiled, false otherwise
     */
    public boolean isCompiled()
    {
        try {
            GClass[] classes = getDefaultPackage().getClasses();
            for (int i = 0; i < classes.length; i++) {
                GClass cls = classes[i];
                if(!cls.isCompiled())  {
                    return false;
                }
            }
        }
        catch (Exception e2) {
            e2.printStackTrace();
            return false;
        }
        return true;
    }
    
    public void addCompileListener(CompileListener listener)
    {
    	synchronized (compileListeners) {
    		compileListeners.add(listener);
    	}
    }
    
    public void removeCompileListener(CompileListener listener)
    {
    	synchronized (compileListeners) {
    		compileListeners.remove(listener);
    	}
    }
    
    // ----------- CompileListener interface -------------
    
    public void compileError(RCompileEvent event)
    {
    	delegateCompileEvent(event);
    }
    
    public void compileFailed(RCompileEvent event)
    {
    	reloadClasses();
    	
    	delegateCompileEvent(event);
    }
    
    public void compileStarted(RCompileEvent event)
    {
    	delegateCompileEvent(event);
    }
    
    public void compileSucceeded(RCompileEvent event)
    {
    	reloadClasses();
    	
    	delegateCompileEvent(event);
    }
    
    public void compileWarning(RCompileEvent event)
    {
    	delegateCompileEvent(event);
    }
    
    // ----------- End of CompileListener interface ------
    
    private void reloadClasses()
    {
        try {
            GPackage pkg = getDefaultPackage();  
            GClass[] classes = pkg.getClasses();
            for (GClass cls : classes) {
                cls.reload();
            }
        }
        catch (ProjectNotOpenException e) {
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
        }
        
    }
    
    private void delegateCompileEvent(RCompileEvent event)
    {
    	synchronized (compileListeners) {
    		List<CompileListener> listeners = new ArrayList<CompileListener>(compileListeners);
    		Iterator<CompileListener> i = listeners.iterator();
    		while (i.hasNext()) {
    			CompileListener listener = i.next();
    			try {
    				switch (event.getEvent()) {
    					case CompileEvent.COMPILE_START_EVENT:
    						listener.compileStarted(event);
    						break;
    					case CompileEvent.COMPILE_DONE_EVENT:
    						listener.compileSucceeded(event);
    						break;
    					case CompileEvent.COMPILE_FAILED_EVENT:
    						listener.compileFailed(event);
    						break;
    					case CompileEvent.COMPILE_ERROR_EVENT:
    						listener.compileError(event);
    						break;
    					case CompileEvent.COMPILE_WARNING_EVENT:
    						listener.compileWarning(event);
    						break;
    					default:
    				}
    			}
    			catch (RemoteException re) {
    				re.printStackTrace();
    			}
    		}
    	}
    }

    /**
     * Gets the image library for this project. If the directory does not
     * exist, it is created.
     * 
     */
    public File getImageDir()
    {
        File projDir;
        try {
            projDir = getDir().getAbsoluteFile();
            File projImagesDir = new File(projDir, "images");
            projImagesDir.mkdir();
            return projImagesDir;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        return null;
    }
}
