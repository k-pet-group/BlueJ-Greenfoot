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
package greenfoot.core;

import greenfoot.gui.classbrowser.ClassView;
import greenfoot.util.GreenfootUtil;

import java.awt.EventQueue;
import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RConstructor;
import rmiextension.wrappers.RField;
import bluej.extensions.*;
import bluej.extensions.ClassNotFoundException;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;


/**
 * Represents a class in Greenfoot. This class wraps the RMI class and contains
 * some extra functionality. The main reason for creating this class is to have
 * a place to store information about inheritance relations between classes that
 * have not been compiled.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class GClass
{
    private static String simObj = "greenfoot.Actor";
    private static String worldObj = "greenfoot.World";
    
    private RClass rmiClass;
    private GPackage pkg;
    private String superclassGuess;
    private boolean compiled;
    
    private ClassView classView;
    private Class realClass;

	/**
     * Constructor used by GCoreClass only
     */
    protected GClass() {
    	
    }
    
    /**
     * Constructor for GClass. You should generally not use this -
     * GPackage maintains a class pool which needs to be updated. Use
     * GPackage.getGClass().
     * 
     * If you use it, remember to call loadSavedSuperClass() afterwards.
     */
    public GClass(RClass cls, GPackage pkg)
    {
        this.rmiClass = cls;
        this.pkg = pkg;
        
        try {
            compiled = cls.isCompiled();
            if (compiled) {
                loadRealClass();
            }
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (PackageNotFoundException pnfe) {
            pnfe.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
        }
    }
    
    /**
     * Load the super class from the saved property, or if it is not saved
     * attempt to guess it.
     */
    public void loadSavedSuperClass() 
    {
        String savedSuperclass = getClassProperty("superclass");
        if(savedSuperclass == null) {
            guessSuperclass();
        } else {
            setSuperclassGuess(savedSuperclass);
        }
    }
    
    /**
     * Set the view to be associated with this GClass. The view is
     * notified when the compilation state changes.
     */
    public void setClassView(ClassView view)
    {
        classView = view;
    }

    /**
     * Notify this class that its name has been changed.
     */
    public void nameChanged(final String oldName)
    {
        try {
            ProjectProperties props = pkg.getProject().getProjectProperties();
            String superClass = props.removeProperty("class." + oldName + ".superclass");
            String classImage = props.removeProperty("class." + oldName + ".image");
            props.removeCachedImage(oldName);
            
            setClassProperty("superclass", superClass);
            if(classImage != null ) {
                setClassProperty("image", classImage);
            }
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            // can't happen
        }
        
        if(classView != null) {
            EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    classView.nameChanged(oldName);
                }
            });
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
        try {
            return pkg.getProject().getProjectProperties().getString("class." + getName() + "." + propertyName);
        }
        catch (ProjectNotOpenException e) {
            return null;
        }
        catch (RemoteException e) {
            return null;
        }    
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
            exc.printStackTrace();
            Debug.reportError("Greenfoot: Could not set class property: " + getName() + "." + propertyName);
        }
    }
    
    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException
    {
        rmiClass.compile(waitCompileEnd);
    }

    /**
     * Open the editor for this class.
     */
    public void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        rmiClass.edit();
        
    }


    public void remove() throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        ProjectProperties props = pkg.getProject().getProjectProperties();
        props.removeProperty("class." + getName() + ".superclass");
        props.removeProperty("class." + getName() + ".image");
        props.removeCachedImage(getName());
        rmiClass.remove();
    }
    
    public RConstructor getConstructor(Class[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getConstructor(signature);
    }

    public RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getConstructors();
    }

    public BMethod getDeclaredMethod(String methodName, Class[] params)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getDeclaredMethod(methodName, params);
    }

    public BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getDeclaredMethods();
    }

    public RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getField(fieldName);
    }

    public BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getFields();
    }

    /**
     * Get the java.lang.Class object representing this class. Returns null if
     * the class cannot be loaded (including if the class is not compiled).
     */
    public Class getJavaClass()
    {
        return realClass;
    }

    public GPackage getPackage()
    {
        return pkg;
    }

    /**
     * Gets the qualified name of this class.
     * @return
     */
    public String getQualifiedName()
    {
        try {
            return rmiClass.getQualifiedName();
        }
        catch (RemoteException e) {
            // TODO error reporting
        }
        catch (ProjectNotOpenException e) {}
        catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Gets the name of this class. NOT the qualified name.
     */
    public String getName() {
        // This class does not depend on the "realClass" since it does a call straight through RMI and to the RClass
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
    private boolean containsCyclicHierarchy() {
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
        GClass superClass = pkg.getClass(superclassName);
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
     * This name will be stripped of any qualifications.
     * 
     * If this guess results in a cyclic hierarchy, it will not be set.
     * 
     * @return True if it was a valid name. False if invalid and something else
     *         should be tried (for instance if the guess is the same as the
     *         name of this)
     */
    public boolean setSuperclassGuess(final String superclassName)
    {
        String superName = GreenfootUtil.extractClassName(superclassName);
       
        if (superName.equals(getName()) ) {
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
     * This method tries to guess which class is the superclass. This can be used for non compilable and non parseable classes.
     * <p>
     * If the class is compiled, it will return the real superclass.
     * <br>
     * If the class is parseable this information will be used to extract the superclass.
     * <br>
     * If the is not parseable it will use the last superclass that was known.
     * <br>
     * In general, we will try to remember the last known superclass, and report that back.
     * <p>
     * 
     * The meethod will only find superclasses that is part of this project or is one of the greenfoot API class (World or Actor).
     * 
     * OBS: This method can be very slow and shouldn't be called unless needed. Especially if the class isn't compiled it can be very slow.
     * 
     * @return Best guess of the name of the superclass (NOT the qualified name).
     */
    private void guessSuperclass()
    {
        // TODO This should be called each time the source file is saved. However,
        // this is not possible at the moment, so we just do it when it is
        // compiled.
        String name = this.getName();
        if(name.equals("World") || name.equals("Actor")) {
            //We do not want to waste time on guessing the name of the superclass for these two classes.
            if(setSuperclassGuess("")) {
                return;
            }
        }
        
        //First, try to get the real super class.
        String realSuperclass = null;
        try {
            if(isCompiled()) {                
                realSuperclass = rmiClass.getSuperclass().getQualifiedName();
            }
        }
        catch (RemoteException e) {
        }
        catch (ProjectNotOpenException e) {
        }
        catch (PackageNotFoundException e) {
        }
        catch (ClassNotFoundException e) {
        }
        catch (NullPointerException e) {
        }

        if(realSuperclass != null && setSuperclassGuess(realSuperclass)) {
            return;
        }
        
        // If the class is compiled, but we did not get a superclass back, then
		// the superclass is not from this project, but we can get it from the
		// real class
        if (realSuperclass == null && isCompiled()) {
        	Class<?> superclass = realClass.getSuperclass();
        	if (superclass != null && setSuperclassGuess(superclass.getName())) {
        	    return;
        	}
        	else {
        		setSuperclassGuess("");
        	}
        }
        
        //Second, try to parse the file
        String parsedSuperclass = null;
        try {
            ClassInfo info = ClassParser.parse(rmiClass.getJavaFile());//, classes);
            parsedSuperclass = info.getSuperclass();
           
            // TODO hack! If the superclass is Actor or World,
            // put it in the right package... parsing does not resolve references...
            if (parsedSuperclass.equals("Actor")) {
                parsedSuperclass = "greenfoot.Actor";
            }
            if (parsedSuperclass.equals("World")) {
                parsedSuperclass = "greenfoot.World";
            }
        }
        catch (ProjectNotOpenException e) {}
        catch (PackageNotFoundException e) {}
        catch (RemoteException e) {}
        catch (Exception e) {}

        if(parsedSuperclass != null && setSuperclassGuess(parsedSuperclass)) {
            return;
        }
        
        //Ok, nothing more to do. We just let the superclassGuess be whatever it is.
        // It can produce incorrect hierarchies if a class named Object inherits Class A, and then that inheritance is removed. It will then stay as a subclass of A because it has the same name as its superclass (java.lang.Object). 
    }
    
    /**
     * Strips the name of a class for its qualified part.
     */
    private String removeQualification(String classname)
    {
        int lastDotIndex = classname.lastIndexOf(".");
        if(lastDotIndex != -1) {
            return classname.substring(lastDotIndex+1);
        } else {
            return classname;
        }
    }

    public String getToString()
    {
        try {
            return rmiClass.getToString();
        }
        catch (RemoteException e) {
            // TODO error reporting
        }
        catch (ProjectNotOpenException e) {}
        catch (ClassNotFoundException e) {}
        return "Error getting real toString. super:" + super.toString();
    }

    /**
     * Check whether this class is compiled.
     */
    public boolean isCompiled()
    {
        return compiled;
    }
    
    /**
     * Set the compiled state of this class.
     */
    public void setCompiledState(boolean isCompiled)
    {
        compiled = isCompiled;
        if (classView != null) {
            // It's safe to call repaint off the event thread
            classView.repaint();
        }
        
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
     * A class is not considered a subclass of itself. So, if the two classes
     * are same it returns false.
     * 
     * It only looks at the name of class and not the fully qualified name.
     * 
     * @param className
     * @return
     */
    public boolean isSubclassOf(String className)
    {    
        className = removeQualification(className);
        GClass superclass = this;
        if(this.getName().equals(className)) {
            return false;
        }
        //Recurse through superclasses
        while (superclass != null) {
            String superclassName = superclass.getSuperclassGuess();
            //TODO Fix this hack. Should be done when non-greenfoot classes gets support.
            //HACK to ensure that a class with no superclass has "" as superclass. This is becuase of the ClassForest building which then allows the class to show up even though it doesn't have any superclass.
            if(superclassName == null) {
                superclassName = "";
            }
            if (superclassName != null && (className.equals(removeQualification(superclassName)))) {
                return true;
            }
            superclass = superclass.getSuperclass();
        }
        return false;
    }   

    public void reload()
    {
    	loadRealClass();
    	guessSuperclass();
        if(classView != null) {
            EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    classView.updateView();
                }
            });
        }
    }

    /**
     * Try and load the "real" (java.lang.Class) class represented by this
     * GClass, using the current class loader.
     * 
     * @return The class, or null if unsuccessful
     */
    private void loadRealClass()
    {
        Class cls = null;
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

}
