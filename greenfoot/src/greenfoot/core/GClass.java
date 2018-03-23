/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2014,2015,2016  Poul Henriksen and Michael Kolling
 
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

import greenfoot.util.GreenfootUtil;

import java.awt.EventQueue;
import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.SourceType;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;


/**
 * Represents a class in Greenfoot. This class wraps the RMI class and contains
 * some extra functionality. The main reason for creating this class is to have
 * a place to store information about inheritance relations between classes that
 * have not been compiled.
 * 
 * @author Poul Henriksen
 */
public class GClass
{
    private static String simObj = "greenfoot.Actor";
    private static String worldObj = "greenfoot.World";
    
    private RClass rmiClass;
    private GPackage pkg;
    private String superclassGuess;
    private String className;
    private boolean compiled;
    
    private Class<?> realClass;
    private boolean hasKnownError;
    
    /**
     * Constructor for GClass. You should generally not use this -
     * GPackage maintains a class pool which needs to be updated. Use
     * GPackage.getGClass().
     * 
     * If you use it, remember to call loadSavedSuperClass() afterwards.
     */
    public GClass(RClass cls, GPackage pkg, boolean inRemoteCallback)
    {
        this.rmiClass = cls;
        this.pkg = pkg;
        
        try {
            compiled = cls.isCompiled(inRemoteCallback);
            if (compiled) {
                loadRealClass();
            }
        }
        catch (PackageNotFoundException e) {
            Debug.reportError("Problem checking class compiled state", e);
        }
        catch (ProjectNotOpenException e) {
            Debug.reportError("Problem checking class compiled state", e);
        }
        catch (RemoteException e) {
            Debug.reportError("Problem checking class compiled state", e);
        }
    }
    
    /**
     * Load the super class from the saved property, or if it is not saved
     * attempt to guess it.
     * 
     * @param inRemoteCallback   should be true iff the execution of this method is blocking the dispatch thread
     *                           in the remote VM.
     */
    public void loadSavedSuperClass(boolean inRemoteCallback) 
    {
        String savedSuperclass = getClassProperty("superclass");
        if(savedSuperclass == null) {
            guessSuperclass(inRemoteCallback);
        } else {
            setSuperclassGuess(savedSuperclass);
        }
    }

    /**
     * Notify this class that its name has been changed.
     */
    public synchronized void nameChanged(final String oldName)
    {
        try {
            className = null; // retrieve it lazily
            
            ProjectProperties props = pkg.getProject().getProjectProperties();
            String superClass = props.removeProperty("class." + oldName + ".superclass");
            String classImage = props.removeProperty("class." + oldName + ".image");
            GreenfootUtil.removeCachedImage(oldName);
            
            setClassProperty("superclass", superClass);
            if(classImage != null ) {
                setClassProperty("image", classImage);
            }            
        }
        catch (Exception e) {
            Debug.reportError("Remote error in GClass.nameChanged()", e);
        }
    }
    
    /**
     * Get the value of a persistent property for this class
     * 
     * @param propertyName   The property name
     * @return   The property value (a String)
     */
    public String getClassProperty(String propertyName)
    {
        return pkg.getProject().getProjectProperties().getString("class." + getName() + "." + propertyName);
    }
    
    /**
     * Set the value of a persistent property for this class
     * 
     * @param propertyName  The property name to set
     * @param value         The value to set the property to
     */
    public void setClassProperty(String propertyName, String value)
    {
        try {
            String key = "class." + getName() + "." + propertyName;
            if (value != null) {
                pkg.getProject().getProjectProperties().setString(key, value);
            }
            else {
                pkg.getProject().getProjectProperties().removeProperty(key);
            }
        }
        catch (Exception exc) {
            Debug.reportError("Greenfoot: Could not set class property: " + getName() + "." + propertyName, exc);
        }
    }
    
    public SourceType getSourceType()
    {
        try {
            return rmiClass.getSourceType();
        }
        catch (ProjectNotOpenException e) {
            Debug.reportError("Could not query source type", e);
            return null;
        }
        catch (PackageNotFoundException e) {
            Debug.reportError("Could not query source type", e);
            return null;
        }
        catch (RemoteException e) {
            Debug.reportError("Could not query source type", e);
            return null;
        }
    }

    public void remove()
    {
        ProjectProperties props = pkg.getProject().getProjectProperties();
        props.removeProperty("class." + getName() + ".superclass");
        props.removeProperty("class." + getName() + ".image");
        GreenfootUtil.removeCachedImage(getName());
        try {
            rmiClass.remove();
        }
        catch (ProjectNotOpenException e) {
            Debug.reportError("Could not remove class", e);
        }
        catch (PackageNotFoundException e) {
            Debug.reportError("Could not remove class", e);
        }
        catch (ClassNotFoundException e) {
            Debug.reportError("Could not remove class", e);
        }
        catch (RemoteException e) {
            Debug.reportError("Could not remove class", e);
        }
    }
    
    /**
     * Get the java.lang.Class object representing this class. Returns null if
     * the class cannot be loaded (including if the class is not compiled).
     */
    public Class<?> getJavaClass()
    {
        return realClass;
    }

    public GPackage getPackage()
    {
        return pkg;
    }

    /**
     * Gets the qualified name of this class (thread-safe).
     */
    public synchronized String getQualifiedName()
    {
        if (className == null) {
            try {
                className = rmiClass.getQualifiedName();
            }
            catch (Exception e) {
                Debug.reportError("While trying to get class name", e);
            }
        }
        return className;
    }

    /**
     * Gets the name of this class. NOT the qualified name.
     */
    public String getName()
    {
        return GreenfootUtil.extractClassName(getQualifiedName());
    }
    /**
     * Returns the superclass or null if no superclass can be found.
     * 
     * @return superclass, or null if the superclass is not part of this
     *         project.
     */
    public GClass getSuperclass()
    {
        GClass superClass = getSuperclassWithoutCheck();
        // Check if there are cyclic hierarchies, and return null if there is.
        if(containsCyclicHierarchy()) {
            return null;
        }
        return superClass;
    }

    /**
     * Returns true if there is a cycle in the inheritance hierarchy.
     * @return
     */
    private boolean containsCyclicHierarchy()
    {
        GClass superCls = getSuperclassWithoutCheck();
        while (superCls != null) {
            if (superCls == this) {
                return true;
            }
            superCls = superCls.getSuperclassWithoutCheck();
        }
        return false;
    }
    
    /**
     * Get the GClass for the superclass guess without checking cycles.
     * 
     */
    private GClass getSuperclassWithoutCheck()
    {
        String superclassName = getSuperclassGuess();
        if (superclassName == null) {
            return null;
        }
        superclassName = GreenfootUtil.extractClassName(superclassName);
        // This line seems to cause deadlock at the moment, so commenting out during Greenfoot rewrite:
        GClass superClass = null; // pkg.getClass(superclassName);
        return superClass;
    }

    /**
     * This method tries to guess which class is the superclass. This can be
     * used for non compilable and non parseable classes. If the superclass
     * guess is a class that is not in the default package, it will return the
     * empty string, since it can otherwise result in problems because we only
     * deal with the unqualified class name.
     * <p>
     * If the class is compiled, it will return the real superclass. <br>
     * If the class is parseable this information will be used to extract the
     * superclass. <br>
     * If the class is not parseable it will use the last superclass that was
     * known. <br>
     * In general, we will try to remember the last known superclass, and report
     * that back. It also saves the superclass between different runs of the
     * greenfoot application.
     * 
     * @return Best guess of the fully qualified name of the superclass.
     */
    public String getSuperclassGuess()
    {
        return superclassGuess;
    }

    /**
     * Sets the superclass guess that will be returned if it is not possible to
     * find it in another way.
     * 
     * <p>If this guess results in a cyclic hierarchy, it will not be set.
     * 
     * @return True if it was a valid name. False if invalid and something else
     *         should be tried (for instance if the guess is the same as the
     *         name of this)
     */
    public boolean setSuperclassGuess(final String superclassName)
    {
        if (superclassName.equals(getQualifiedName())) {
            return false;
        }
        
        boolean isDefaultPkg = !superclassName.contains(".");
        boolean isGreenfootClass = superclassName.startsWith("greenfoot.");
        boolean isNewName = superclassGuess == null || !superclassGuess.equals(superclassName);

    
        if (isNewName && (isDefaultPkg || isGreenfootClass)) {
            // If the superclass guess is a class that is not in the default
            // package, it can result in problems because we only deal with the
            // unqualified class name. Except if it is one of the Greenfoot classes

            superclassGuess = superclassName;  
            if(containsCyclicHierarchy()) {
                superclassGuess = "";            
            } 
        } else if (!isDefaultPkg && !isGreenfootClass){
            // We found a super class that is not interesting to greenfoot
            superclassGuess = "";
        }
        setClassProperty("superclass", superclassGuess);
        return true;
    }
    
    /**
     * This method tries to guess which class is the superclass. This can be used for non
     * compilable and non parseable classes.
     * 
     * <p>If the class is compiled, it will return the real superclass.<br>
     * If the class is parseable this information will be used to extract the superclass.<br>
     * If the is not parseable it will use the last superclass that was known.<br>
     * In general, we will try to remember the last known superclass, and report that back.
     * 
     * @param inRemoteCallback   should be true iff the execution of this method is blocking the dispatch thread
     *                           in the remote VM.
     * 
     * @return Best guess of the name of the superclass (NOT the qualified name).
     */
    private synchronized void guessSuperclass(boolean inRemoteCallback)
    {
        // TODO This should be called each time the source file is saved. However,
        // this is not possible at the moment, so we just do it when it is
        // compiled.
        String name = this.getQualifiedName();
        if(name.equals("greenfoot.World") || name.equals("greenfoot.Actor")) {
            //We do not want to waste time on guessing the name of the superclass for these two classes.
            if(setSuperclassGuess("")) {
                return;
            }
        }
        
        if (isCompiled()) {
            Class<?> superclass = realClass.getSuperclass();
            if (superclass == null || !setSuperclassGuess(superclass.getName())) {
                setSuperclassGuess("");
            }
            return;
        }
        
        try {
            RClass sclass = rmiClass.getSuperclass(inRemoteCallback);
            if (sclass != null) {
                setSuperclassGuess(sclass.getQualifiedName());
                return;
            }
        }
        catch (ProjectNotOpenException e) {
            Debug.reportError("Couldn't get superclass", e);
        }
        catch (PackageNotFoundException e) {
            Debug.reportError("Couldn't get superclass", e);
        }
        catch (ClassNotFoundException e) {
            Debug.reportError("Couldn't get superclass", e);
        }
        catch (RemoteException e) {
            Debug.reportError("Couldn't get superclass", e);
        }
        
        //Ok, nothing more to do. We just let the superclassGuess be whatever it is.
    }
    
    /**
     * Check whether this class is compiled (thread-safe).
     */
    public boolean isCompiled()
    {
        return compiled;
    }
    
    /**
     * Set the compiled state of this class.
     * @param hasKnownError
     */
    public synchronized void setCompiledState(boolean isCompiled, boolean hasKnownError)
    {
        compiled = isCompiled;
        this.hasKnownError = hasKnownError;
        
        if (isCompiled) {
            loadRealClass();
        }
        else {
            realClass = null;
        }
    }

    /**
     * Returns true if this class is a subclass of the given class.
     * 
     * <p>A class is not considered a subclass of itself. So, if the two classes
     * are same it returns false.
     * 
     * <p>It only looks at the name of class and not the fully qualified name.
     */
    public boolean isSubclassOf(String className)
    {    
        if (this.getQualifiedName().equals(className)) {
            return false;
        }
        
        //Recurse through superclasses
        GClass currentClass = this;
        do {
            String superclassName = currentClass.getSuperclassGuess();
            if (className.equals(superclassName)) {
                return true;
            }
            currentClass = currentClass.getSuperclass();
        } while (currentClass != null);
        return false;
    }   

    /**
     * Reload. Do not call from a remote callback.
     */
    public synchronized void reload()
    {
        loadRealClass();
        guessSuperclass(false);
    }

    /**
     * Try and load the "real" (java.lang.Class) class represented by this
     * GClass, using the current class loader.
     * 
     * <p>Must be called from a synchronized context!
     * 
     * @return The class, or null if unsuccessful
     */
    private void loadRealClass()
    {
        Class<?> cls = null;
        if (! isCompiled()) {
            realClass = null;
            return;
        }
        try {
            String className = getQualifiedName();
            //it is important that we use the right classloader
            ClassLoader classLdr = ExecServer.getCurrentClassLoader();
            cls = Class.forName(className, false, classLdr);
        }
        catch (java.lang.ClassNotFoundException cnfe) {
            // couldn't load: that's ok, we return null
            // cnfe.printStackTrace();
        }
        catch (LinkageError e) {
            // TODO log this properly? It can happen for various reasons, not
            // necessarily a real error.
            e.printStackTrace();
        }
        realClass = cls;
    }

    /**
     * Returns true if this GClass represents the greenfoot.Actor class.
     *
     */
    public boolean isActorClass()
    {
        return getQualifiedName().equals(simObj);
    }
    
    /**
     * Returns true if this GClass represents the greenfoot.World class.
     *
     */
    public boolean isWorldClass()
    {
        return getQualifiedName().equals(worldObj);
    }

    /**
     * Returns true if this GClass represents a class that is a subclass of the greenfoot.Actor class.
     *
     */
    public boolean isActorSubclass()
    {
        return isSubclassOf(simObj);
    }

    /**
     * Returns true if this GClass represents a class that is a subclass of the greenfoot.World class.
     *
     */
    public boolean isWorldSubclass()
    {        
        return isSubclassOf(worldObj);
    }

    public boolean hasKnownError()
    {
        return hasKnownError;
    }
}
